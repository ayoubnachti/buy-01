import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { throwError } from 'rxjs';
import { vi } from 'vitest';

import { OrderDetails } from './order-details';
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