import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { computed, signal } from '@angular/core';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { CartPage, QUANTITY_DEBOUNCE_MS } from './cart-page';
import { CartStore } from '../../../core/services/cart.store';
import { ToastService } from '../../../core/services/toast.service';
import { CartItem } from '../../../shared/models/cart-response';

function item(productId: string, quantity: number, overrides: Partial<CartItem> = {}): CartItem {
  return {
    productId,
    quantity,
    name: `Product ${productId}`,
    price: 10,
    imageUrl: null,
    stock: 10,
    available: true,
    lineTotal: 10 * quantity,
    addedAt: '2026-11-04T10:00:00Z',
    ...overrides,
  };
}

describe('CartPage', () => {
  const items = signal<CartItem[]>([]);
  let store: {
    items: typeof items;
    subtotal: ReturnType<typeof signal<number>>;
    hasUnavailableItems: ReturnType<typeof computed<boolean>>;
    load: ReturnType<typeof vi.fn>;
    updateQuantity: ReturnType<typeof vi.fn>;
    remove: ReturnType<typeof vi.fn>;
  };
  let toast: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };

  function render() {
    TestBed.configureTestingModule({
      imports: [CartPage],
      providers: [
        provideRouter([]),
        { provide: CartStore, useValue: store },
        { provide: ToastService, useValue: toast },
      ],
    });
    const fixture = TestBed.createComponent(CartPage);
    fixture.detectChanges(); // runs ngOnInit -> load()
    return fixture;
  }

  const text = (f: ReturnType<typeof render>) => (f.nativeElement as HTMLElement).textContent ?? '';
  const checkout = (f: ReturnType<typeof render>) =>
    (f.nativeElement as HTMLElement).querySelector('a[href="/checkout"]') as HTMLAnchorElement;

  beforeEach(() => {
    vi.useFakeTimers();
    items.set([]);
    store = {
      items,
      subtotal: signal(0),
      hasUnavailableItems: computed(() => items().some((i) => !i.available)),
      load: vi.fn().mockReturnValue(of(undefined)),
      // Like the real store: the server's answer replaces the cart
      updateQuantity: vi.fn().mockImplementation((productId: string, quantity: number) => {
        items.update((list) =>
          list.map((i) => (i.productId === productId ? { ...i, quantity } : i)),
        );
        return of(undefined);
      }),
      remove: vi.fn().mockImplementation((productId: string) => {
        items.update((list) => list.filter((i) => i.productId !== productId));
        return of(undefined);
      }),
    };
    toast = { success: vi.fn(), error: vi.fn() };
  });

  afterEach(() => vi.useRealTimers());

  // ---- loading

  it('reloads the cart on opening, for live prices and stock', () => {
    render();

    expect(store.load).toHaveBeenCalledTimes(1);
  });

  it('shows the empty state with a way back to the products', () => {
    const f = render();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    expect(text(f)).toContain('Your cart is empty');

    const button = (f.nativeElement as HTMLElement).querySelector(
      'app-empty-state button',
    ) as HTMLButtonElement;
    button.click();

    expect(navigate).toHaveBeenCalledWith(['/search']);
  });

  it('shows an error when the cart cannot be loaded', () => {
    store.load.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    const f = render();

    expect(text(f)).toContain("couldn't load your cart");
  });

  // ---- quantity stepper

  it('sends one request after several quick clicks, with the final quantity', () => {
    items.set([item('p1', 1)]);
    const page = render().componentInstance;

    for (let i = 0; i < 4; i++) {
      page.changeQuantity(items()[0], 1);
    }
    expect(store.updateQuantity).not.toHaveBeenCalled();

    vi.advanceTimersByTime(QUANTITY_DEBOUNCE_MS);

    expect(store.updateQuantity).toHaveBeenCalledTimes(1);
    expect(store.updateQuantity).toHaveBeenCalledWith('p1', 5);
  });

  it('debounces each product separately', () => {
    items.set([item('p1', 1), item('p2', 1)]);
    const page = render().componentInstance;

    page.changeQuantity(items()[0], 1);
    page.changeQuantity(items()[1], 1);
    vi.advanceTimersByTime(QUANTITY_DEBOUNCE_MS);

    expect(store.updateQuantity).toHaveBeenCalledWith('p1', 2);
    expect(store.updateQuantity).toHaveBeenCalledWith('p2', 2);
  });

  it('shows the clicked quantity immediately, before the server answers', () => {
    items.set([item('p1', 1)]);
    const page = render().componentInstance;

    page.changeQuantity(items()[0], 1);

    expect(page.displayedQuantity(items()[0])).toBe(2);
  });

  it('does not go below 1 or above the stock', () => {
    items.set([item('p1', 1, { stock: 1 })]);
    const page = render().componentInstance;

    page.changeQuantity(items()[0], -1);
    page.changeQuantity(items()[0], 1);
    vi.advanceTimersByTime(QUANTITY_DEBOUNCE_MS);

    expect(store.updateQuantity).not.toHaveBeenCalled();
  });

  it('blocks checkout while a change is not saved yet', () => {
    items.set([item('p1', 1)]);
    const f = render();
    const page = f.componentInstance;

    page.changeQuantity(items()[0], 1);
    expect(page.canCheckout()).toBe(false);

    vi.advanceTimersByTime(QUANTITY_DEBOUNCE_MS);
    expect(page.canCheckout()).toBe(true);
  });

  it('shows the backend message and goes back to the server quantity when refused', () => {
    items.set([item('p1', 1)]);
    store.updateQuantity.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { success: false, message: 'Only 1 left in stock for product p1' },
          }),
      ),
    );
    const page = render().componentInstance;

    page.changeQuantity(items()[0], 1);
    vi.advanceTimersByTime(QUANTITY_DEBOUNCE_MS);

    expect(toast.error).toHaveBeenCalledWith('Only 1 left in stock for product p1');
    expect(page.displayedQuantity(items()[0])).toBe(1);
    expect(page.canCheckout()).toBe(true);
  });

  it('keeps working after a refused change', () => {
    items.set([item('p1', 1)]);
    store.updateQuantity.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 409 })),
    );
    const page = render().componentInstance;

    page.changeQuantity(items()[0], 1);
    vi.advanceTimersByTime(QUANTITY_DEBOUNCE_MS);
    page.changeQuantity(items()[0], 1);
    vi.advanceTimersByTime(QUANTITY_DEBOUNCE_MS);

    expect(store.updateQuantity).toHaveBeenCalledTimes(2);
  });

  // ---- problems and checkout

  it('warns about unavailable items and blocks checkout', () => {
    items.set([item('p1', 5, { stock: 2, available: false })]);
    const f = render();

    expect(text(f)).toContain('no longer available');
    expect(text(f)).toContain('Only 2 left in stock');
    expect(f.componentInstance.canCheckout()).toBe(false);
    expect(checkout(f).getAttribute('aria-disabled')).toBe('true');
  });

  it('shows a deleted product so the user can remove it', () => {
    items.set([item('gone', 1, { name: null, price: null, lineTotal: null, available: false })]);
    const f = render();

    expect(text(f)).toContain('no longer sold');
  });

  it('allows checkout when every line is available', () => {
    items.set([item('p1', 1)]);
    const f = render();

    expect(f.componentInstance.canCheckout()).toBe(true);
    expect(checkout(f).getAttribute('aria-disabled')).toBe('false');
  });

  // ---- remove

  it('removes an item and confirms it', () => {
    items.set([item('p1', 1)]);
    const page = render().componentInstance;

    page.remove(items()[0]);

    expect(store.remove).toHaveBeenCalledWith('p1');
    expect(toast.success).toHaveBeenCalled();
  });

  it('shows the backend message when a remove fails', () => {
    items.set([item('p1', 1)]);
    store.remove.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 404,
            error: { success: false, message: 'Product p1 is not in your cart' },
          }),
      ),
    );
    const page = render().componentInstance;

    page.remove(items()[0]);

    expect(toast.error).toHaveBeenCalledWith('Product p1 is not in your cart');
  });

  it('does not go above 99, the order limit, even with more stock', () => {
    items.set([item('p1', 99, { stock: 150 })]);
    const page = render().componentInstance;

    page.changeQuantity(items()[0], 1);
    vi.advanceTimersByTime(QUANTITY_DEBOUNCE_MS);

    expect(store.updateQuantity).not.toHaveBeenCalled();
  });
});
