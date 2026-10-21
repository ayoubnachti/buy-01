import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Order } from '../../../shared/models/order.model';
import { OrdersService } from '../../../core/services/orders.service';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';

@Component({
  selector: 'app-order-details',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, LoadingSpinner],
  templateUrl: './order-details.html',
})
export class OrderDetails implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly ordersService = inject(OrdersService);

  order = signal<Order | null>(null);
  loading = signal(true);
  error = signal(false);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set(true);
      this.loading.set(false);
      return;
    }

    this.ordersService.getOne(id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  lineTotal(unitPrice: number, quantity: number): number {
    return unitPrice * quantity;
  }
}
