import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';

import { OrdersService } from '../../../core/services/orders.service';
import { ToastService } from '../../../core/services/toast.service';
import { OrdersListBase } from '../orders-list-base';
import { Order, OrderStatus } from '../../../shared/models/order.model';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { toApiError } from '../../../shared/models/api-error';

// The seller can only move an order one step forward. DELIVERED and
// CANCELLED have no "next" step, so they are left out on purpose.
const NEXT_STATUS: Partial<Record<OrderStatus, OrderStatus>> = {
  PENDING: 'CONFIRMED',
  CONFIRMED: 'SHIPPED',
  SHIPPED: 'DELIVERED',
};

@Component({
  selector: 'app-seller-orders',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, ReactiveFormsModule, LoadingSpinner, EmptyState],
  templateUrl: './seller-orders.html',
})
export class SellerOrders extends OrdersListBase {
  private readonly ordersService = inject(OrdersService);
  private readonly toastService = inject(ToastService);

  // Order ids currently being updated, so each row's own button is disabled
  // only while its own request is in flight — other rows stay usable.
  updatingIds = signal<ReadonlySet<string>>(new Set());

  constructor() {
    super();
    this.initializeOrdersList((page, size, filters) =>
      this.ordersService.getSellerOrders(page, size, filters),
    );
  }

  nextStatusFor(order: Order): OrderStatus | undefined {
    return NEXT_STATUS[order.status];
  }

  isUpdating(order: Order): boolean {
    return this.updatingIds().has(order.id);
  }

  onMarkAsNext(order: Order): void {
    const next = NEXT_STATUS[order.status];
    if (!next || this.isUpdating(order)) {
      return;
    }

    this.updatingIds.update((ids) => new Set(ids).add(order.id));
    this.ordersService.updateStatus(order.id, next).subscribe({
      next: () => {
        this.removeUpdatingId(order.id);
        this.toastService.success('Order status updated.');
        this.fetchOrders();
      },
      error: (e) => {
        this.removeUpdatingId(order.id);
        const err = toApiError(e, 'Could not update this order. Try again.');
        if (!err.handledGlobally) {
          this.toastService.error(err.message);
        }
      },
    });
  }

  private removeUpdatingId(orderId: string): void {
    this.updatingIds.update((ids) => {
      const next = new Set(ids);
      next.delete(orderId);
      return next;
    });
  }
}
