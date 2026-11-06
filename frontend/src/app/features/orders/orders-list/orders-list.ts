import { Component, inject } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';

import { OrdersService } from '../../../core/services/orders.service';
import { OrdersListBase } from '../orders-list-base';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';

@Component({
  selector: 'app-orders-list',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, ReactiveFormsModule, LoadingSpinner, EmptyState],
  templateUrl: './orders-list.html',
})
export class OrdersList extends OrdersListBase {
  private readonly ordersService = inject(OrdersService);

  constructor() {
    super();
    this.initializeOrdersList((page, size, filters) => this.ordersService.getMy(page, size, filters));
  }
}
