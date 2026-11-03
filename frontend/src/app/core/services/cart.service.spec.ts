import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { environment } from '../../../environments/environment';
import { CartService } from './cart.service';

describe('CartService', () => {
  let service: CartService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CartService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CartService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('adds one item to the cart by default and unwraps the response', () => {
    service.addItem('product-1').subscribe((cart) => {
      expect(cart.items).toEqual([
        { productId: 'product-1', quantity: 1, addedAt: '2026-01-01T00:00:00Z' },
      ]);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/cart/items`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ productId: 'product-1', quantity: 1 });
    req.flush({
      success: true,
      message: 'Item added to cart',
      data: {
        items: [{ productId: 'product-1', quantity: 1, addedAt: '2026-01-01T00:00:00Z' }],
        updatedAt: '2026-01-01T00:00:00Z',
      },
    });
  });

  it('sends an explicitly requested quantity', () => {
    service.addItem('product-2', 3).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/cart/items`);
    expect(req.request.body).toEqual({ productId: 'product-2', quantity: 3 });
    req.flush({
      success: true,
      message: 'Item added to cart',
      data: { items: [], updatedAt: '2026-01-01T00:00:00Z' },
    });
  });
});
