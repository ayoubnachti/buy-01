import { Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { CartStore } from '../../../core/services/cart.store';
import { ToastService } from '../../../core/services/toast.service';
import { toApiError } from '../../models/api-error';

@Component({
  selector: 'app-add-to-cart-button',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './add-to-cart-button.html',
})
export class AddToCartButton {
  private readonly authService = inject(AuthService);
  private readonly cartStore = inject(CartStore);
  private readonly toastService = inject(ToastService);

  readonly productId = input.required<string>();
  readonly productName = input.required<string>();
  readonly stock = input.required<number>();
  readonly size = input<'sm' | 'lg'>('sm');

  readonly isClient = computed(() => this.authService.user()?.role === 'CLIENT');
  readonly isVisitor = computed(() => this.authService.user() === null);
  readonly adding = signal(false);

  add(event: Event): void {
    event.preventDefault();
    event.stopPropagation();

    if (this.adding()) {
      return;
    }
    this.adding.set(true);

    this.cartStore.add(this.productId()).subscribe({
      next: () => {
        this.adding.set(false);
        this.toastService.success(`${this.productName()} added to cart.`);
      },
      error: (e) => {
        this.adding.set(false);
        const err = toApiError(e, `Couldn't add ${this.productName()} to your cart.`);
        if (!err.handledGlobally) {
          this.toastService.error(err.message);
        }
      },
    });
  }
}