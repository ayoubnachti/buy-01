import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { CartStore } from '../../../core/services/cart.store';
import { OrdersService } from '../../../core/services/orders.service';
import { ToastService } from '../../../core/services/toast.service';
import { Order } from '../../../shared/models/order.model';
import { CreateOrderRequest, ShippingAddress } from '../../../shared/models/create-order-request';
import { toApiError } from '../../../shared/models/api-error';

export function notBlank(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && control.value.trim().length === 0
    ? { blank: true }
    : null;
}

export const PAYMENT_METHODS = [{ value: 'PAY_ON_DELIVERY', label: 'Pay on delivery' }] as const;

type Step = 'address' | 'review' | 'confirm' | 'done';

@Component({
  selector: 'app-checkout-page',
  standalone: true,
  imports: [CurrencyPipe, ReactiveFormsModule, RouterLink],
  templateUrl: './checkout-page.html',
})
export class CheckoutPage {
  private readonly fb = inject(FormBuilder);
  private readonly cartStore = inject(CartStore);
  private readonly ordersService = inject(OrdersService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);

  readonly items = this.cartStore.items;
  readonly subtotal = this.cartStore.subtotal;
  readonly paymentMethods = PAYMENT_METHODS;

  readonly step = signal<Step>('address');
  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);
  readonly orders = signal<Order[]>([]);

  readonly address = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, notBlank, Validators.maxLength(100)]],
    phone: [
      '',
      [
        Validators.required,
        notBlank,
        Validators.maxLength(30),
        Validators.pattern(/^\+?[0-9 ()-]{6,30}$/),
      ],
    ],
    street: ['', [Validators.required, notBlank, Validators.maxLength(200)]],
    city: ['', [Validators.required, notBlank, Validators.maxLength(100)]],
    postalCode: ['', [Validators.required, notBlank, Validators.maxLength(20)]],
    country: ['', [Validators.required, notBlank, Validators.maxLength(100)]],
  });

  readonly paymentMethod = this.fb.nonNullable.control(PAYMENT_METHODS[0].value);

  goToReview(): void {
    if (this.address.invalid) {
      this.address.markAllAsTouched();
      return;
    }
    this.step.set('review');
  }

  goTo(step: Exclude<Step, 'done'>): void {
    this.submitError.set(null);
    this.step.set(step);
  }

  errorFor(field: keyof ShippingAddress): string | null {
    const control = this.address.controls[field];
    if (!control.touched || control.valid) {
      return null;
    }
    if (control.hasError('required') || control.hasError('blank')) {
      return 'This field is required.';
    }
    if (control.hasError('maxlength')) {
      return `At most ${control.getError('maxlength').requiredLength} characters.`;
    }
    if (control.hasError('pattern')) {
      return 'Use digits, spaces and an optional leading +.';
    }
    return 'Invalid value.';
  }

  placeOrder(): void {
    if (this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.submitError.set(null);

    const raw = this.address.getRawValue();
    const request: CreateOrderRequest = {
      shippingAddress: {
        fullName: raw.fullName.trim(),
        phone: raw.phone.trim(),
        street: raw.street.trim(),
        city: raw.city.trim(),
        postalCode: raw.postalCode.trim(),
        country: raw.country.trim(),
      },
      items: this.items().map((item) => ({ productId: item.productId, quantity: item.quantity })),
    };

    this.ordersService.create(request).subscribe({
      next: (orders) => {
        this.submitting.set(false);
        this.orders.set(orders);
        this.step.set('done');
        this.cartStore
          .clear()
          .subscribe({
            error: () =>
              this.toastService.info('Your order was placed. You can empty your cart manually.'),
          });
      },
      error: (e) => {
        this.submitting.set(false);
        const err = toApiError(e, 'Your order was not placed. Please try again.');

        if (err.status === 409) {
          this.toastService.error(err.message);
          this.router.navigate(['/cart']);
          return;
        }

        this.submitError.set(
          err.handledGlobally
            ? 'Your order was not placed. Your cart is kept, so you can try again.'
            : err.message,
        );
      },
    });
  }
}
