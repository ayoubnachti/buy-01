import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { CartStore } from './cart.store';
import { CartService } from './cart.service';
import { AuthService } from './auth.service';
import { CartItem, CartResponse } from '../../shared/models/cart-response';

function item(productId: string, quantity: number, available = true): CartItem {
  return {
    productId,
    quantity,
    available,
    name: `Product ${productId}`,
    price: 10,
    imageUrl: null,
    stock: 5,
    lineTotal: 10 * quantity,
    addedAt: '2026-11-04T10:00:00Z',
  };
}

function cart(...items: CartItem[]): CartResponse {
  return {
    items,
    subtotal: items.filter((i) => i.available).reduce((sum, i) => sum + (i.lineTotal ?? 0), 0),
    updatedAt: '2026-11-04T10:00:00Z',
  };
}

describe('CartStore', () => {
  const user = signal<{ id: string; role: string } | null>(null);
  let cartService: Record<
    'getCart' | 'addItem' | 'updateQuantity' | 'removeItem' | 'clear',
    ReturnType<typeof vi.fn>
  >;

  function createStore(): CartStore {
    TestBed.configureTestingModule({
      providers: [
        { provide: CartService, useValue: cartService },
        { provide: AuthService, useValue: { user } },
      ],
    });
    const store = TestBed.inject(CartStore);
    TestBed.tick(); // run the effect
    return store;
  }

  beforeEach(() => {
    user.set(null);
    cartService = {
      getCart: vi.fn().mockReturnValue(of(cart(item('p1', 2)))),
      addItem: vi.fn(),
      updateQuantity: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn(),
    };
  });

  // ---- loading follows the logged-in user

  it('loads the cart when a client is logged in at start (a refresh keeps the cart)', () => {
    user.set({ id: 'c1', role: 'CLIENT' });
    const store = createStore();

    expect(cartService.getCart).toHaveBeenCalledTimes(1);
    expect(store.items()).toHaveLength(1);
  });

  it('does not load for a visitor', () => {
    const store = createStore();

    expect(cartService.getCart).not.toHaveBeenCalled();
    expect(store.count()).toBe(0);
  });

  it('does not load for a seller', () => {
    user.set({ id: 's1', role: 'SELLER' });
    createStore();

    expect(cartService.getCart).not.toHaveBeenCalled();
  });

  it('loads after login and empties on logout', () => {
    const store = createStore();

    user.set({ id: 'c1', role: 'CLIENT' });
    TestBed.tick();
    expect(store.count()).toBe(2);

    user.set(null);
    TestBed.tick();
    expect(store.count()).toBe(0);
  });

  it('keeps the current cart when loading fails', () => {
    cartService.getCart.mockReturnValue(throwError(() => new Error('down')));
    user.set({ id: 'c1', role: 'CLIENT' });

    expect(() => createStore()).not.toThrow();
  });

  // ---- every change stores what the server returned

  it('add: stores the server cart, and the badge counts total quantity', () => {
    const store = createStore();
    cartService.addItem.mockReturnValue(of(cart(item('p1', 2), item('p2', 1))));

    store.add('p2').subscribe();

    expect(cartService.addItem).toHaveBeenCalledWith('p2', 1);
    expect(store.count()).toBe(3); // 2 + 1, not 2 lines
  });

  it('updateQuantity: stores the server cart', () => {
    const store = createStore();
    cartService.updateQuantity.mockReturnValue(of(cart(item('p1', 4))));

    store.updateQuantity('p1', 4).subscribe();

    expect(store.items()[0].quantity).toBe(4);
  });

  it('remove: stores the server cart', () => {
    const store = createStore();
    cartService.removeItem.mockReturnValue(of(cart()));

    store.remove('p1').subscribe();

    expect(store.items()).toHaveLength(0);
  });

  it('clear: empties the cart', () => {
    user.set({ id: 'c1', role: 'CLIENT' });
    const store = createStore();
    cartService.clear.mockReturnValue(of(undefined));

    store.clear().subscribe();

    expect(store.count()).toBe(0);
    expect(store.subtotal()).toBe(0);
  });

  it('a failed change leaves the cart as it was', () => {
    user.set({ id: 'c1', role: 'CLIENT' });
    const store = createStore();
    cartService.addItem.mockReturnValue(throwError(() => new Error('409')));

    store.add('p9').subscribe({ error: () => {} });

    expect(store.count()).toBe(2);
  });

  // ---- derived state

  it('flags unavailable items', () => {
    cartService.getCart.mockReturnValue(of(cart(item('p1', 1), item('p2', 1, false))));
    user.set({ id: 'c1', role: 'CLIENT' });
    const store = createStore();

    expect(store.hasUnavailableItems()).toBe(true);
    expect(store.subtotal()).toBe(10); // only the available line
  });
});
