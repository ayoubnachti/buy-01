import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Order } from '../../../shared/models/order.model';
import { AuthService } from '../../../core/services/auth.service';
import { OrdersService } from '../../../core/services/orders.service';
import { ToastService } from '../../../core/services/toast.service';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';
import { ConfirmationModal } from '../../../shared/components/confirmation-modal/confirmation-modal';
import { toApiError } from '../../../shared/models/api-error';

@Component({
  selector: 'app-order-details',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, LoadingSpinner, ConfirmationModal],
  templateUrl: './order-details.html',
})
export class OrderDetails implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly ordersService = inject(OrdersService);
  private readonly toastService = inject(ToastService);

  order = signal<Order | null>(null);
  loading = signal(true);
  error = signal(false);
  showCancelConfirm = signal(false);
  showRemoveConfirm = signal(false);
  redoing = signal(false);
  cancelling = signal(false);
  removing = signal(false);

  // Only the client who placed the order can redo/remove it. A seller
  // viewing the same order via /orders/{id} must not see those buttons.
  readonly isOwner = computed(() => this.order()?.userId === this.authService.user()?.id);

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

  canRedoOrRemove(order: Order): boolean {
    return order.status === 'CANCELLED' || order.status === 'DELIVERED';
  }

  onCancelClick(): void {
    this.showCancelConfirm.set(true);
  }

  onCancelDismissed(): void {
    this.showCancelConfirm.set(false);
  }

  onCancelConfirmed(): void {
    const order = this.order();
    if (!order || this.cancelling()) {
      return;
    }

    this.cancelling.set(true);
    this.ordersService.cancel(order.id).subscribe({
      next: (updatedOrder) => {
        this.cancelling.set(false);
        this.order.set(updatedOrder);
        this.showCancelConfirm.set(false);
        this.toastService.success('Order cancelled.');
      },
      error: (e) => {
        this.cancelling.set(false);
        this.showCancelConfirm.set(false);
        const err = toApiError(e, 'Could not cancel this order. Try again.');
        if (!err.handledGlobally) {
          this.toastService.error(err.message);
        }
      },
    });
  }

  onRedoClick(): void {
    const order = this.order();
    if (!order || this.redoing()) {
      return;
    }

    this.redoing.set(true);
    this.ordersService.redo(order.id).subscribe({
      next: (newOrders) => {
        this.redoing.set(false);
        this.toastService.success('Order placed again.');
        this.router.navigate(['/orders', newOrders[0].id]);
      },
      error: (e) => {
        this.redoing.set(false);
        const err = toApiError(e, 'Could not order this again. Try again.');
        if (!err.handledGlobally) {
          this.toastService.error(err.message);
        }
      },
    });
  }

  onRemoveClick(): void {
    this.showRemoveConfirm.set(true);
  }

  onRemoveDismissed(): void {
    this.showRemoveConfirm.set(false);
  }

  onRemoveConfirmed(): void {
    const order = this.order();
    if (!order || this.removing()) {
      return;
    }

    this.removing.set(true);
    this.ordersService.remove(order.id).subscribe({
      next: () => {
        this.removing.set(false);
        this.showRemoveConfirm.set(false);
        this.toastService.success('Order removed.');
        this.router.navigate(['/orders']);
      },
      error: (e) => {
        this.removing.set(false);
        this.showRemoveConfirm.set(false);
        const err = toApiError(e, 'Could not remove this order. Try again.');
        if (!err.handledGlobally) {
          this.toastService.error(err.message);
        }
      },
    });
  }
}
