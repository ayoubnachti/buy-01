import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { OrdersService } from '../../../core/services/orders.service';
import { ToastService } from '../../../core/services/toast.service';
import { Order, OrderStatus } from '../../../shared/models/order.model';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';

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
  imports: [CurrencyPipe, DatePipe, RouterLink, LoadingSpinner, EmptyState],
  templateUrl: './seller-orders.html',
})
export class SellerOrders {
  private readonly ordersService = inject(OrdersService);
  private readonly toastService = inject(ToastService);

  orders = signal<Order[]>([]);
  loading = signal(true);
  error = signal(false);

  currentPage = signal(0);
  totalPages = signal(0);

  constructor() {
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

  onMarkAsNext(order: Order): void {
    const next = NEXT_STATUS[order.status];
    if (!next) {
      return;
    }

    this.ordersService.updateStatus(order.id, next).subscribe({
      next: () => {
        this.toastService.success('Order status updated.');
        this.fetchOrders();
      },
      error: () => {
        this.toastService.error('Could not update this order. Try again.');
      },
    });
  }

  private fetchOrders(): void {
    this.loading.set(true);
    this.error.set(false);

    this.ordersService.getSellerOrders(this.currentPage(), PAGE_SIZE).subscribe({
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
