import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { AuthService } from './auth.service';
import { CartService } from './cart.service';
import { CartResponse } from '../../shared/models/cart-response';

const EMPTY_CART: CartResponse = { items: [], subtotal: 0, updatedAt: null };

/**
 * The cart as the whole app sees it. Badge, product pages and the cart page all read it here,
 * so one change updates every view. The server stays the source of truth: every method
 * stores what the API returned, never a locally computed guess.
 */
@Injectable({ providedIn: 'root' })
export class CartStore {
  private readonly cartService = inject(CartService);
  private readonly authService = inject(AuthService);

  private readonly cartSignal = signal<CartResponse>(EMPTY_CART);

  readonly cart = this.cartSignal.asReadonly();
  readonly items = computed(() => this.cartSignal().items);
  readonly count = computed(() => this.items().reduce((sum, item) => sum + item.quantity, 0));
  readonly subtotal = computed(() => this.cartSignal().subtotal);
  readonly hasUnavailableItems = computed(() => this.items().some((item) => !item.available));

  constructor() {
    effect(() => {
      const isClient = this.authService.user()?.role === 'CLIENT';
      untracked(() => {
        if (isClient) {
          this.load().subscribe({
            error: () => {
            },
          });
        } else {
          this.cartSignal.set(EMPTY_CART);
        }
      });
    });
  }

  load(): Observable<CartResponse> {
    return this.cartService.getCart().pipe(tap((cart) => this.cartSignal.set(cart)));
  }

  add(productId: string, quantity = 1): Observable<CartResponse> {
    return this.cartService.addItem(productId, quantity).pipe(tap((cart) => this.cartSignal.set(cart)));
  }

  updateQuantity(productId: string, quantity: number): Observable<CartResponse> {
    return this.cartService.updateQuantity(productId, quantity).pipe(tap((cart) => this.cartSignal.set(cart)));
  }

  remove(productId: string): Observable<CartResponse> {
    return this.cartService.removeItem(productId).pipe(tap((cart) => this.cartSignal.set(cart)));
  }

  clear(): Observable<void> {
    return this.cartService.clear().pipe(tap(() => this.cartSignal.set(EMPTY_CART)));
  }
}