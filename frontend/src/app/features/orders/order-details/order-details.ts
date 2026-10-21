import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Order } from '../../../shared/models/order.model';
import { OrdersService } from '../../../core/services/orders.service';
import { ToastService } from '../../../core/services/toast.service';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';
import { ConfirmationModal } from '../../../shared/components/confirmation-modal/confirmation-modal';

@Component({
  selector: 'app-order-details',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, LoadingSpinner, ConfirmationModal],
  templateUrl: './order-details.html',
})
export class OrderDetails implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly ordersService = inject(OrdersService);
  private readonly toastService = inject(ToastService);

  order = signal<Order | null>(null);
  loading = signal(true);
  error = signal(false);
  showCancelConfirm = signal(false);

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

  onCancelClick(): void {
    this.showCancelConfirm.set(true);
  }

  onCancelDismissed(): void {
    this.showCancelConfirm.set(false);
  }

  onCancelConfirmed(): void {
    const order = this.order();
    if (!order) {
      return;
    }

    this.ordersService.cancel(order.id).subscribe({
      next: (updatedOrder) => {
        this.order.set(updatedOrder);
        this.showCancelConfirm.set(false);
        this.toastService.success('Order cancelled.');
      },
      error: () => {
        this.showCancelConfirm.set(false);
        this.toastService.error('Could not cancel this order. Try again.');
      },
    });
  }
}
