import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CurrencyPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { EMPTY, Subject, catchError, debounceTime, groupBy, mergeMap, switchMap, tap } from 'rxjs';

import { CartStore } from '../../../core/services/cart.store';
import { ToastService } from '../../../core/services/toast.service';
import { CartItem } from '../../../shared/models/cart-response';
import { toApiError } from '../../../shared/models/api-error';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';

/** Wait this long after the last click before sending the new quantity. */
export const QUANTITY_DEBOUNCE_MS = 400;
export const MAX_QUANTITY_PER_ITEM = 99;

@Component({
  selector: 'app-cart-page',
  standalone: true,
  imports: [CurrencyPipe, RouterLink, LoadingSpinner, EmptyState],
  templateUrl: './cart-page.html',
})
export class CartPage implements OnInit {
  private readonly cartStore = inject(CartStore);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  readonly items = this.cartStore.items;
  readonly subtotal = this.cartStore.subtotal;
  readonly hasUnavailableItems = this.cartStore.hasUnavailableItems;

  readonly loading = signal(true);
  readonly loadError = signal(false);

  
  private readonly pendingQuantities = signal<Record<string, number>>({});
  readonly hasPendingChanges = computed(() => Object.keys(this.pendingQuantities()).length > 0);

  // Product ids currently being removed, so only that item's own "Remove"
  // button is disabled while its request is in flight.
  private readonly removingIds = signal<ReadonlySet<string>>(new Set());

  readonly canCheckout = computed(
    () => this.items().length > 0 && !this.hasUnavailableItems() && !this.hasPendingChanges(),
  );

  private readonly quantityChanges = new Subject<{ productId: string; quantity: number }>();

  constructor() {
    this.quantityChanges
      .pipe(
        groupBy((change) => change.productId),
        mergeMap((changes) =>
          changes.pipe(
            debounceTime(QUANTITY_DEBOUNCE_MS),
            switchMap(({ productId, quantity }) =>
              this.cartStore.updateQuantity(productId, quantity).pipe(
                tap(() => this.clearPending(productId)),
                catchError((e) => {
                  this.clearPending(productId);
                  const err = toApiError(e, 'Could not update the quantity. Try again.');
                  if (!err.handledGlobally) {
                    this.toastService.error(err.message);
                  }
                  return EMPTY;
                }),
              ),
            ),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  ngOnInit(): void {
    this.cartStore.load().subscribe({
      next: () => this.loading.set(false),
      error: () => {
        this.loading.set(false);
        this.loadError.set(true);
      },
    });
  }

  displayedQuantity(item: CartItem): number {
    return this.pendingQuantities()[item.productId] ?? item.quantity;
  }

  changeQuantity(item: CartItem, delta: number): void {
    const next = this.displayedQuantity(item) + delta;
    if (next < 1 || next > Math.min(item.stock, MAX_QUANTITY_PER_ITEM)) {
      return;
    }
    this.pendingQuantities.update((p) => ({ ...p, [item.productId]: next }));
    this.quantityChanges.next({ productId: item.productId, quantity: next });
  }

  isRemoving(item: CartItem): boolean {
    return this.removingIds().has(item.productId);
  }

  remove(item: CartItem): void {
    if (this.isRemoving(item)) {
      return;
    }

    this.removingIds.update((ids) => new Set(ids).add(item.productId));
    this.cartStore.remove(item.productId).subscribe({
      next: () => {
        this.clearRemoving(item.productId);
        this.toastService.success(`${item.name ?? 'Item'} removed from your cart.`);
      },
      error: (e) => {
        this.clearRemoving(item.productId);
        const err = toApiError(e, 'Could not remove this item. Try again.');
        if (!err.handledGlobally) {
          this.toastService.error(err.message);
        }
      },
    });
  }

  private clearRemoving(productId: string): void {
    this.removingIds.update((ids) => {
      const next = new Set(ids);
      next.delete(productId);
      return next;
    });
  }

  lineTotal(item: CartItem): number | null {
    return item.price === null ? null : item.price * this.displayedQuantity(item);
  }

  private clearPending(productId: string): void {
    this.pendingQuantities.update(({ [productId]: _, ...rest }) => rest);
  }

  goToProducts(): void {
    this.router.navigate(['/search']);
  }
}