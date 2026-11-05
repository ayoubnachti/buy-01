import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { Observable, firstValueFrom, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { cartReadyGuard } from './cart-ready.guard';
import { CartStore } from '../services/cart.store';
import { CartItem, CartResponse } from '../../shared/models/cart-response';

const line = (available = true): CartItem => ({
  productId: 'p1', name: 'Mouse', price: 10, imageUrl: null, stock: 5, quantity: 1,
  available, lineTotal: 10, addedAt: '2026-11-05T10:00:00Z',
});
const cart = (...items: CartItem[]): CartResponse => ({ items, subtotal: 10, updatedAt: null });

describe('cartReadyGuard', () => {
  let load: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    load = vi.fn();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: CartStore, useValue: { load } }],
    });
  });

  async function run(): Promise<string | true> {
    const result = TestBed.runInInjectionContext(() => cartReadyGuard({} as never, {} as never));
    const value = await firstValueFrom(result as Observable<boolean | UrlTree>);
    return value === true ? true : TestBed.inject(Router).serializeUrl(value as UrlTree);
  }

  it('lets a cart with available items through (loads it first, so a refresh works)', async () => {
    load.mockReturnValue(of(cart(line())));
    expect(await run()).toBe(true);
    expect(load).toHaveBeenCalled();
  });

  it('sends an empty cart back to /cart', async () => {
    load.mockReturnValue(of(cart()));
    expect(await run()).toBe('/cart');
  });

  it('sends a cart with an unavailable item back to /cart', async () => {
    load.mockReturnValue(of(cart(line(), line(false))));
    expect(await run()).toBe('/cart');
  });

  it('sends the user to /cart when the cart cannot be loaded', async () => {
    load.mockReturnValue(throwError(() => new Error('down')));
    expect(await run()).toBe('/cart');
  });
});