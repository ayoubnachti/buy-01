import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { Router, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { CheckoutPage, notBlank } from './checkout-page';
import { CartStore } from '../../../core/services/cart.store';
import { OrdersService } from '../../../core/services/orders.service';
import { ToastService } from '../../../core/services/toast.service';
import { CartItem } from '../../../shared/models/cart-response';
import { FormControl } from '@angular/forms';

const items: CartItem[] = [
  {
    productId: 'p1',
    name: 'Mouse',
    price: 10,
    imageUrl: null,
    stock: 5,
    quantity: 2,
    available: true,
    lineTotal: 20,
    addedAt: 'x',
  },
  {
    productId: 'p2',
    name: 'Lamp',
    price: 5,
    imageUrl: null,
    stock: 5,
    quantity: 1,
    available: true,
    lineTotal: 5,
    addedAt: 'x',
  },
];

const validAddress = {
  fullName: '  Ayoub Nachti ',
  phone: '+212 600 000 000',
  street: '1 Main St',
  city: 'Oujda',
  postalCode: '60000',
  country: 'Morocco',
};

describe('CheckoutPage', () => {
  let cartStore: {
    items: ReturnType<typeof signal<CartItem[]>>;
    subtotal: ReturnType<typeof signal<number>>;
    clear: ReturnType<typeof vi.fn>;
  };
  let ordersService: { create: ReturnType<typeof vi.fn> };
  let toast: {
    success: ReturnType<typeof vi.fn>;
    error: ReturnType<typeof vi.fn>;
    info: ReturnType<typeof vi.fn>;
  };
  let page: CheckoutPage;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    cartStore = {
      items: signal(items),
      subtotal: signal(25),
      clear: vi.fn().mockReturnValue(of(undefined)),
    };
    ordersService = {
      create: vi.fn().mockReturnValue(
        of([
          { id: 'o1', totalAmount: 20 },
          { id: 'o2', totalAmount: 5 },
        ]),
      ),
    };
    toast = { success: vi.fn(), error: vi.fn(), info: vi.fn() };

    TestBed.configureTestingModule({
      imports: [CheckoutPage],
      providers: [
        provideRouter([]),
        { provide: CartStore, useValue: cartStore },
        { provide: OrdersService, useValue: ordersService },
        { provide: ToastService, useValue: toast },
      ],
    });
    page = TestBed.createComponent(CheckoutPage).componentInstance;
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  const fillAndConfirm = () => {
    page.address.setValue(validAddress);
    page.goToReview();
    page.goTo('confirm');
  };

  // ---- step 1: address

  it('does not continue with an invalid address, and shows every error', () => {
    page.goToReview();

    expect(page.step()).toBe('address');
    expect(page.errorFor('fullName')).toBe('This field is required.');
    expect(page.errorFor('country')).toBe('This field is required.');
  });

  it('rejects a value made of spaces only, like the backend does', () => {
    expect(notBlank(new FormControl('   '))).toEqual({ blank: true });
    expect(notBlank(new FormControl('Oujda'))).toBeNull();
  });

  it('enforces the backend length limits', () => {
    page.address.controls.postalCode.setValue('1'.repeat(21));
    page.address.controls.postalCode.markAsTouched();

    expect(page.errorFor('postalCode')).toBe('At most 20 characters.');
  });

  it('continues to the review with a valid address', () => {
    page.address.setValue(validAddress);
    page.goToReview();

    expect(page.step()).toBe('review');
  });

  it('offers Pay on delivery as the only, preselected method', () => {
    expect(page.paymentMethods.map((m) => m.value)).toEqual(['PAY_ON_DELIVERY']);
    expect(page.paymentMethod.value).toBe('PAY_ON_DELIVERY');
  });

  // ---- step 3: placing the order

  it('sends the trimmed address and the cart items, never prices', () => {
    fillAndConfirm();
    page.placeOrder();

    expect(ordersService.create).toHaveBeenCalledWith({
      shippingAddress: { ...validAddress, fullName: 'Ayoub Nachti' },
      items: [
        { productId: 'p1', quantity: 2 },
        { productId: 'p2', quantity: 1 },
      ],
    });
  });

  it('shows one order per seller and clears the cart on success', () => {
    fillAndConfirm();
    page.placeOrder();

    expect(page.step()).toBe('done');
    expect(page.orders().map((o) => o.id)).toEqual(['o1', 'o2']);
    expect(cartStore.clear).toHaveBeenCalled();
  });

  it('still shows success if clearing the cart fails (the orders exist)', () => {
    cartStore.clear.mockReturnValue(throwError(() => new Error('down')));
    fillAndConfirm();
    page.placeOrder();

    expect(page.step()).toBe('done');
    expect(toast.info).toHaveBeenCalled();
  });

  it('sends only one order on a double click', () => {
    ordersService.create.mockReturnValue(new Subject()); // request still running
    fillAndConfirm();

    page.placeOrder();
    page.placeOrder();

    expect(ordersService.create).toHaveBeenCalledTimes(1);
    expect(page.submitting()).toBe(true);
  });

  it('goes back to the cart with the backend message when out of stock (409)', () => {
    ordersService.create.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { success: false, message: 'Only 1 left in stock for Mouse' },
          }),
      ),
    );
    fillAndConfirm();
    page.placeOrder();

    expect(toast.error).toHaveBeenCalledWith('Only 1 left in stock for Mouse');
    expect(navigate).toHaveBeenCalledWith(['/cart']);
    expect(cartStore.clear).not.toHaveBeenCalled();
  });

  it('keeps the cart and allows a retry when the server is unavailable (503)', () => {
    ordersService.create.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 503 })));
    fillAndConfirm();
    page.placeOrder();

    expect(page.step()).toBe('confirm');
    expect(page.submitError()).toContain('Your cart is kept');
    expect(page.submitting()).toBe(false);
    expect(toast.error).not.toHaveBeenCalled();
    expect(cartStore.clear).not.toHaveBeenCalled();
  });

  it('shows the backend message inline for a rejected request (400)', () => {
    ordersService.create.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { success: false, message: 'Phone is too long' },
          }),
      ),
    );
    fillAndConfirm();
    page.placeOrder();

    expect(page.submitError()).toBe('Phone is too long');
  });
});
