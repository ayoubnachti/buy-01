import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { SellerOrders } from './seller-orders';
import { OrdersService } from '../../../core/services/orders.service';
import { ToastService } from '../../../core/services/toast.service';
import { Order } from '../../../shared/models/order.model';

describe('SellerOrders – status update errors', () => {
  let ordersService: { getSellerOrders: ReturnType<typeof vi.fn>; updateStatus: ReturnType<typeof vi.fn> };
  let toast: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };
  let component: SellerOrders;

  beforeEach(() => {
    ordersService = {
      getSellerOrders: vi.fn().mockReturnValue(of({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 })),
      updateStatus: vi.fn(),
    };
    toast = { success: vi.fn(), error: vi.fn() };

    TestBed.configureTestingModule({
      imports: [SellerOrders],
      providers: [
        provideRouter([]),
        { provide: OrdersService, useValue: ordersService },
        { provide: ToastService, useValue: toast },
      ],
    });

    component = TestBed.createComponent(SellerOrders).componentInstance;
  });

  const order = { id: 'o1', status: 'PENDING' } as Order;

  it('shows the backend message when the transition is refused (409)', () => {
    ordersService.updateStatus.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { success: false, message: 'Cannot move from PENDING to DELIVERED' },
    })));

    component.onMarkAsNext(order)                                 // ← check: method name

    expect(toast.error).toHaveBeenCalledWith('Cannot move from PENDING to DELIVERED');
  });

  it('does not toast again when the interceptor already did (500)', () => {
    ordersService.updateStatus.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    component.onMarkAsNext(order)                                 // ← check: method name

    expect(toast.error).not.toHaveBeenCalled();
  });

  it('a second click on the same order while it is updating does not send a second request', () => {
    const subject = new Subject<Order>();
    ordersService.updateStatus.mockReturnValue(subject.asObservable());

    component.onMarkAsNext(order);
    expect(component.isUpdating(order)).toBe(true);

    component.onMarkAsNext(order);
    expect(ordersService.updateStatus).toHaveBeenCalledTimes(1);

    subject.next({ ...order, status: 'CONFIRMED' });
    subject.complete();
    expect(component.isUpdating(order)).toBe(false);
  });

  it('updating one order does not disable another order\'s button', () => {
    const subject = new Subject<Order>();
    ordersService.updateStatus.mockReturnValue(subject.asObservable());
    const otherOrder = { id: 'o2', status: 'PENDING' } as Order;

    component.onMarkAsNext(order);

    expect(component.isUpdating(order)).toBe(true);
    expect(component.isUpdating(otherOrder)).toBe(false);
  });
});