import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { OrderDetails } from './order-details';
import { AuthService } from '../../../core/services/auth.service';
import { OrdersService } from '../../../core/services/orders.service';
import { ToastService } from '../../../core/services/toast.service';
import { Order } from '../../../shared/models/order.model';

describe('OrderDetails – cancel errors', () => {
  let ordersService: { getOne: ReturnType<typeof vi.fn>; cancel: ReturnType<typeof vi.fn> };
  let toast: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };
  let component: OrderDetails;

  beforeEach(() => {
    ordersService = { getOne: vi.fn(), cancel: vi.fn() };
    toast = { success: vi.fn(), error: vi.fn() };

    TestBed.configureTestingModule({
      imports: [OrderDetails],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 'o1' }) } } },
        { provide: OrdersService, useValue: ordersService },
        { provide: ToastService, useValue: toast },
      ],
    });

    component = TestBed.createComponent(OrderDetails).componentInstance;
    component.order.set({ id: 'o1', status: 'PENDING' } as Order);
    component.showCancelConfirm.set(true);
  });

  it('shows the backend message when the cancel is refused (409)', () => {
    ordersService.cancel.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { success: false, message: 'Order already shipped' },
    })));

    component.onCancelConfirmed();

    expect(toast.error).toHaveBeenCalledWith('Order already shipped');
    expect(component.showCancelConfirm()).toBe(false);
  });

  it('does not toast again when the interceptor already did (500)', () => {
    ordersService.cancel.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    component.onCancelConfirmed();

    expect(toast.error).not.toHaveBeenCalled();
    expect(component.showCancelConfirm()).toBe(false);
  });
});

describe('OrderDetails – double-click guards', () => {
  let ordersService: {
    getOne: ReturnType<typeof vi.fn>;
    cancel: ReturnType<typeof vi.fn>;
    remove: ReturnType<typeof vi.fn>;
  };
  let toast: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };
  let component: OrderDetails;

  beforeEach(() => {
    ordersService = { getOne: vi.fn(), cancel: vi.fn(), remove: vi.fn() };
    toast = { success: vi.fn(), error: vi.fn() };

    TestBed.configureTestingModule({
      imports: [OrderDetails],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 'o1' }) } } },
        { provide: OrdersService, useValue: ordersService },
        { provide: ToastService, useValue: toast },
      ],
    });

    component = TestBed.createComponent(OrderDetails).componentInstance;
    component.order.set({ id: 'o1', status: 'PENDING' } as Order);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  it('cancel: a second click while cancelling does not cancel twice', () => {
    const subject = new Subject<Order>();
    ordersService.cancel.mockReturnValue(subject.asObservable());
    component.showCancelConfirm.set(true);

    component.onCancelConfirmed();
    expect(component.cancelling()).toBe(true);

    component.onCancelConfirmed();
    expect(ordersService.cancel).toHaveBeenCalledTimes(1);

    subject.next({ id: 'o1', status: 'CANCELLED' } as Order);
    subject.complete();
    expect(component.cancelling()).toBe(false);
  });

  it('remove: a second click while removing does not remove twice', () => {
    const subject = new Subject<void>();
    ordersService.remove.mockReturnValue(subject.asObservable());
    component.showRemoveConfirm.set(true);

    component.onRemoveConfirmed();
    expect(component.removing()).toBe(true);

    component.onRemoveConfirmed();
    expect(ordersService.remove).toHaveBeenCalledTimes(1);

    subject.next();
    subject.complete();
    expect(component.removing()).toBe(false);
  });
});

describe('OrderDetails – status history', () => {
  let ordersService: { getOne: ReturnType<typeof vi.fn> };

  const baseOrder: Order = {
    id: 'o1',
    userId: 'u1',
    sellerId: 's1',
    status: 'SHIPPED',
    items: [],
    shippingAddress: { fullName: 'A', phone: null, street: 's', city: 'c', postalCode: '1', country: 'M' },
    paymentMethod: 'PAY_ON_DELIVERY',
    totalAmount: 0,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-03T00:00:00Z',
    statusHistory: [
      { status: 'PENDING', at: '2026-01-01T00:00:00Z', by: 'u1' },
      { status: 'CONFIRMED', at: '2026-01-02T00:00:00Z', by: 's1' },
      { status: 'SHIPPED', at: '2026-01-03T00:00:00Z', by: 's1' },
    ],
  };

  beforeEach(() => {
    ordersService = { getOne: vi.fn() };

    TestBed.configureTestingModule({
      imports: [OrderDetails],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 'o1' }) } } },
        { provide: OrdersService, useValue: ordersService },
        { provide: ToastService, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    });
  });

  it('shows every status history entry as a timeline', () => {
    ordersService.getOne.mockReturnValue(of(baseOrder));

    const fixture = TestBed.createComponent(OrderDetails);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('PENDING');
    expect(text).toContain('CONFIRMED');
    expect(text).toContain('SHIPPED');
  });

  it('hides the status history section when there is no history', () => {
    ordersService.getOne.mockReturnValue(of({ ...baseOrder, statusHistory: [] }));

    const fixture = TestBed.createComponent(OrderDetails);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('Status history');
  });
});

describe('OrderDetails – redo and remove', () => {
  let ordersService: {
    getOne: ReturnType<typeof vi.fn>;
    redo: ReturnType<typeof vi.fn>;
    remove: ReturnType<typeof vi.fn>;
  };
  let toast: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };
  let router: Router;

  const deliveredOrder: Order = {
    id: 'o1',
    userId: 'u1',
    sellerId: 's1',
    status: 'DELIVERED',
    items: [],
    shippingAddress: { fullName: 'A', phone: null, street: 's', city: 'c', postalCode: '1', country: 'M' },
    paymentMethod: 'PAY_ON_DELIVERY',
    totalAmount: 0,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    statusHistory: [],
  };

  function setUp(currentUserId: string) {
    ordersService = { getOne: vi.fn(), redo: vi.fn(), remove: vi.fn() };
    toast = { success: vi.fn(), error: vi.fn() };

    TestBed.configureTestingModule({
      imports: [OrderDetails],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 'o1' }) } } },
        { provide: OrdersService, useValue: ordersService },
        { provide: ToastService, useValue: toast },
        { provide: AuthService, useValue: { user: () => ({ id: currentUserId, role: 'CLIENT' }) } },
      ],
    });

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  }

  it('shows "Order again" and "Remove" only for the owner, on a cancelled/delivered order', () => {
    setUp('u1');
    ordersService.getOne.mockReturnValue(of(deliveredOrder));

    const fixture = TestBed.createComponent(OrderDetails);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Order again');
    expect(text).toContain('Remove');
  });

  it('hides those buttons for the seller viewing the same order', () => {
    setUp('s1');
    ordersService.getOne.mockReturnValue(of(deliveredOrder));

    const fixture = TestBed.createComponent(OrderDetails);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('Order again');
    expect(text).not.toContain('Remove');
  });

  it('redo: creates a new order and navigates to it', () => {
    setUp('u1');
    ordersService.getOne.mockReturnValue(of(deliveredOrder));
    ordersService.redo.mockReturnValue(of([{ ...deliveredOrder, id: 'o2', status: 'PENDING' }]));

    const fixture = TestBed.createComponent(OrderDetails);
    fixture.detectChanges();
    fixture.componentInstance.onRedoClick();

    expect(toast.success).toHaveBeenCalledWith('Order placed again.');
    expect(router.navigate).toHaveBeenCalledWith(['/orders', 'o2']);
  });

  it('redo: shows the backend message when it is refused (409)', () => {
    setUp('u1');
    ordersService.getOne.mockReturnValue(of(deliveredOrder));
    ordersService.redo.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { success: false, message: 'Out of stock' } })),
    );

    const fixture = TestBed.createComponent(OrderDetails);
    fixture.detectChanges();
    fixture.componentInstance.onRedoClick();

    expect(toast.error).toHaveBeenCalledWith('Out of stock');
  });

  it('remove: removes the order and navigates back to the list', () => {
    setUp('u1');
    ordersService.getOne.mockReturnValue(of(deliveredOrder));
    ordersService.remove.mockReturnValue(of(undefined));

    const fixture = TestBed.createComponent(OrderDetails);
    fixture.detectChanges();
    fixture.componentInstance.onRemoveConfirmed();

    expect(toast.success).toHaveBeenCalledWith('Order removed.');
    expect(router.navigate).toHaveBeenCalledWith(['/orders']);
  });
});