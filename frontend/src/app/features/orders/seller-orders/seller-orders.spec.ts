import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
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

    component.onMarkAsNext(order);

    expect(toast.error).toHaveBeenCalledWith('Cannot move from PENDING to DELIVERED');
  });

  it('does not toast again when the interceptor already did (500)', () => {
    ordersService.updateStatus.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    component.onMarkAsNext(order);

    expect(toast.error).not.toHaveBeenCalled();
  });
});