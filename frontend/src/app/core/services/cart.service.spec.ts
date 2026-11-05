import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { CartService } from './cart.service';
import { environment } from '../../../environments/environment';
import { CartResponse } from '../../shared/models/cart-response';

describe('CartService', () => {
  let service: CartService;
  let http: HttpTestingController;

  const url = `${environment.apiUrl}/cart`;

  const cart: CartResponse = {
    items: [
      {
        productId: 'p1',
        name: 'Mouse',
        price: 10,
        imageUrl: null,
        stock: 5,
        quantity: 2,
        available: true,
        lineTotal: 20,
        addedAt: '2026-11-04T10:00:00Z',
      },
    ],
    subtotal: 20,
    updatedAt: '2026-11-04T10:00:00Z',
  };

  /** The backend wraps every success in ResponseData. */
  const wrapped = (data: CartResponse) => ({ success: true, message: 'ok', data });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CartService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());   // fails the test if a request was made but not checked

  // ---- getCart

  it('getCart: GET /cart and returns the cart inside ResponseData', () => {
    let result: CartResponse | undefined;
    service.getCart().subscribe((c) => (result = c));

    const req = http.expectOne(url);
    expect(req.request.method).toBe('GET');
    req.flush(wrapped(cart));

    expect(result).toEqual(cart);
  });

  // ---- addItem

  it('addItem: POST /cart/items with quantity 1 by default', () => {
    let result: CartResponse | undefined;
    service.addItem('p1').subscribe((c) => (result = c));

    const req = http.expectOne(`${url}/items`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ productId: 'p1', quantity: 1 });
    req.flush(wrapped(cart));

    expect(result).toEqual(cart);
  });

  it('addItem: sends the given quantity', () => {
    service.addItem('p1', 3).subscribe();

    const req = http.expectOne(`${url}/items`);
    expect(req.request.body).toEqual({ productId: 'p1', quantity: 3 });
    req.flush(wrapped(cart));
  });

  // ---- updateQuantity

  it('updateQuantity: PUT /cart/items/{id} with the new quantity', () => {
    let result: CartResponse | undefined;
    service.updateQuantity('p1', 4).subscribe((c) => (result = c));

    const req = http.expectOne(`${url}/items/p1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ quantity: 4 });
    req.flush(wrapped(cart));

    expect(result).toEqual(cart);
  });

  // ---- removeItem

  it('removeItem: DELETE /cart/items/{id} and returns the updated cart', () => {
    let result: CartResponse | undefined;
    service.removeItem('p1').subscribe((c) => (result = c));

    const req = http.expectOne(`${url}/items/p1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(wrapped(cart));

    expect(result).toEqual(cart);
  });

  it('encodes the product id in the path', () => {
    service.removeItem('a/b c').subscribe();

    http.expectOne(`${url}/items/a%2Fb%20c`).flush(wrapped(cart));
  });

  // ---- clear

  it('clear: DELETE /cart and completes on 204 with no body', () => {
    let completed = false;
    service.clear().subscribe({ complete: () => (completed = true) });

    const req = http.expectOne(url);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });

    expect(completed).toBe(true);
  });

  // ---- errors are passed on, not swallowed

  it('passes HTTP errors on to the caller', () => {
    let status: number | undefined;
    service.addItem('p1').subscribe({ error: (e) => (status = e.status) });

    http.expectOne(`${url}/items`).flush(
      { success: false, message: 'Only 2 left in stock', data: null },
      { status: 409, statusText: 'Conflict' },
    );

    expect(status).toBe(409);
  });
});