import { Directive, signal } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';

import { OrderSearchFilters } from '../../core/services/orders.service';
import { OrderPageResponse } from '../../shared/models/order-page-response';
import { Order, OrderStatus } from '../../shared/models/order.model';

const PAGE_SIZE = 10;

/**
 * Shared by the client's "My Orders" page and the seller's "Orders to
 * fulfill" page: same filters, same pagination, same fetch-and-render
 * shape. Each subclass only has to say which endpoint to call.
 */
@Directive()
export abstract class OrdersListBase {
  readonly statuses: OrderStatus[] = ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

  readonly filtersForm = new FormGroup({
    status: new FormControl<OrderStatus | ''>(''),
    from: new FormControl(''),
    to: new FormControl(''),
    q: new FormControl(''),
  });

  readonly orders = signal<Order[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);

  readonly currentPage = signal(0);
  readonly totalPages = signal(0);

  // Set once by initializeOrdersList() before fetchOrders() is ever called.
  private fetchPage!: (
    page: number,
    size: number,
    filters: OrderSearchFilters,
  ) => Observable<OrderPageResponse>;

  /** Call once from the subclass's own constructor, after its services are injected. */
  protected initializeOrdersList(
    fetchPage: (page: number, size: number, filters: OrderSearchFilters) => Observable<OrderPageResponse>,
  ): void {
    this.fetchPage = fetchPage;
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

  protected fetchOrders(): void {
    this.loading.set(true);
    this.error.set(false);

    const { status, from, to, q } = this.filtersForm.getRawValue();

    this.fetchPage(this.currentPage(), PAGE_SIZE, {
      status: status || undefined,
      from: from || undefined,
      to: to || undefined,
      q: q?.trim() || undefined,
    }).subscribe({
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
