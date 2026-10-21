import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';

import { OrdersService } from '../../../core/services/orders.service';
import { Order } from '../../../shared/models/order.model';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-orders-list',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, LoadingSpinner, EmptyState],
  templateUrl: './orders-list.html',
})
export class OrdersList {
  private readonly ordersService = inject(OrdersService);

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

  private fetchOrders(): void {
    this.loading.set(true);
    this.error.set(false);

    this.ordersService.getMy(this.currentPage(), PAGE_SIZE).subscribe({
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
