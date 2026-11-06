import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { OrdersService } from '../../../core/services/orders.service';
import { ToastService } from '../../../core/services/toast.service';
import { Order, OrderStatus } from '../../../shared/models/order.model';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { toApiError } from '../../../shared/models/api-error';

const PAGE_SIZE = 10;

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
export class SellerOrders {
  private readonly ordersService = inject(OrdersService);
  private readonly toastService = inject(ToastService);

  readonly statuses: OrderStatus[] = ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

  readonly filtersForm = new FormGroup({
    status: new FormControl<OrderStatus | ''>(''),
    from: new FormControl(''),
    to: new FormControl(''),
    q: new FormControl(''),
  });

  orders = signal<Order[]>([]);
  loading = signal(true);
  error = signal(false);

  currentPage = signal(0);
  totalPages = signal(0);

  // Order ids currently being updated, so each row's own button is disabled
  // only while its own request is in flight — other rows stay usable.
  updatingIds = signal<ReadonlySet<string>>(new Set());

  constructor() {
    this.fetchOrders();
  }

  onSearch(): void {
    this.currentPage.set(0);
    this.fetchOrders();
  }

  onClearFilters(): void {
    this.filtersForm.reset({ status: '', from: '', to: '', q: '' });
    this.currentPage.set(0);
    this.fetchOrders();
  }

  onPreviousPage(): void {
    if (this.currentPage() === 0) {
      return;
    }
    this.currentPage.update((p) => p - 1);
    this.fetchOrders();
  }

  onNextPage(): void {
    if (this.currentPage() + 1 >= this.totalPages()) {
      return;
    }
    this.currentPage.update((p) => p + 1);
    this.fetchOrders();
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

  private fetchOrders(): void {
    this.loading.set(true);
    this.error.set(false);

    const { status, from, to, q } = this.filtersForm.getRawValue();

    this.ordersService
      .getSellerOrders(this.currentPage(), PAGE_SIZE, {
        status: status || undefined,
        from: from || undefined,
        to: to || undefined,
        q: q?.trim() || undefined,
      })
      .subscribe({
        next: (response) => {
          this.orders.set(response.content);
          this.totalPages.set(response.totalPages);
          this.loading.set(false);
        },
        error: () => {
          this.error.set(true);
          this.loading.set(false);
        },
      });
  }
}
