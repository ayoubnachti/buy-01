import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AddToCartButton } from './add-to-cart-button';
import { AuthService } from '../../../core/services/auth.service';
import { CartStore } from '../../../core/services/cart.store';
import { ToastService } from '../../../core/services/toast.service';

describe('AddToCartButton', () => {
  const user = signal<{ id: string; role: string } | null>(null);
  let cartStore: { add: ReturnType<typeof vi.fn> };
  let toast: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };

  function render(stock = 5) {
    TestBed.configureTestingModule({
      imports: [AddToCartButton],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { user } },
        { provide: CartStore, useValue: cartStore },
        { provide: ToastService, useValue: toast },
      ],
    });
    const fixture = TestBed.createComponent(AddToCartButton);
    fixture.componentRef.setInput('productId', 'p1');
    fixture.componentRef.setInput('productName', 'Mouse');
    fixture.componentRef.setInput('stock', stock);
    fixture.detectChanges();
    return fixture;
  }

  const button = (f: ReturnType<typeof render>) => f.nativeElement.querySelector('button') as HTMLButtonElement | null;
  const link = (f: ReturnType<typeof render>) => f.nativeElement.querySelector('a') as HTMLAnchorElement | null;

  beforeEach(() => {
    user.set(null);
    cartStore = { add: vi.fn().mockReturnValue(of({ items: [], subtotal: 0, updatedAt: null })) };
    toast = { success: vi.fn(), error: vi.fn() };
  });

  it('shows "Add to cart" to a client, and adds through the store', () => {
    user.set({ id: 'c1', role: 'CLIENT' });
    const f = render();

    button(f)!.click();

    expect(cartStore.add).toHaveBeenCalledWith('p1');
    expect(toast.success).toHaveBeenCalledWith('Mouse added to cart.');
  });

  it('shows "Log in to buy" to a visitor', () => {
    const f = render();

    expect(button(f)).toBeNull();
    expect(link(f)?.textContent).toContain('Log in to buy');
  });

  it('shows nothing to a seller', () => {
    user.set({ id: 's1', role: 'SELLER' });
    const f = render();

    expect(button(f)).toBeNull();
    expect(link(f)).toBeNull();
  });

  it('is disabled when out of stock', () => {
    user.set({ id: 'c1', role: 'CLIENT' });
    const f = render(0);

    expect(button(f)!.disabled).toBe(true);
    expect(button(f)!.textContent).toContain('Out of stock');
  });

  it('shows the backend message when the add is refused', () => {
    user.set({ id: 'c1', role: 'CLIENT' });
    cartStore.add.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { success: false, message: 'Only 2 left in stock for product p1' },
    })));
    const f = render();

    button(f)!.click();

    expect(toast.error).toHaveBeenCalledWith('Only 2 left in stock for product p1');
  });

  it('does not toast when the interceptor already did', () => {
    user.set({ id: 'c1', role: 'CLIENT' });
    cartStore.add.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 503 })));
    const f = render();

    button(f)!.click();

    expect(toast.error).not.toHaveBeenCalled();
  });

  it('ignores a second click while the first add is running', () => {
    user.set({ id: 'c1', role: 'CLIENT' });
    cartStore.add.mockReturnValue(new (class { subscribe() { return { unsubscribe() {} }; } })());   // never completes
    const f = render();

    f.componentInstance.add(new Event('click'));
    f.componentInstance.add(new Event('click'));

    expect(cartStore.add).toHaveBeenCalledTimes(1);
  });
});