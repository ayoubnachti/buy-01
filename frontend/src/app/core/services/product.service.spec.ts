import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ProductService } from './product.service';
import { Product } from '../../shared/models/product.model';
import { ProductPageResponse } from '../../shared/models/product-page-response';
import { environment } from '../../../environments/environment';

describe('ProductService', () => {
  let service: ProductService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ProductService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProductService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('fetches products from the correct endpoint and unwraps the ResponseData/data envelope', () => {
    const mockProducts: Product[] = [
      {
        id: '1',
        name: 'chair',
        description: 'chair to sit on',
        price: 122.21,
        quantity: 12,
        category: 'ELECTRONICS',
        userId: 'u1',
        imageUrls: [],
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    ];

    const mockPageResponse: ProductPageResponse = {
      items: mockProducts,
      currentPage: 0,
      pageSize: 20,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
      hasPrevious: false,
    };

    service.getAll().subscribe((response) => {
      expect(response).toEqual(mockPageResponse);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/products`);
    expect(req.request.method).toBe('GET');
    req.flush({
      success: true,
      message: 'Products retrieved successfully',
      data: mockPageResponse,
    });
  });

  it('propagates an error when the request fails', () => {
    let errored = false;

    service.getAll().subscribe({
      error: () => {
        errored = true;
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/products`);
    req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });

    expect(errored).toBe(true);
  });

  it('sends search filters using the product search API parameters', () => {
    service
      .search({
        q: 'desk',
        category: ['HOME', 'BOOKS'],
        minPrice: 12.5,
        maxPrice: 80,
        sort: 'price_asc',
        page: 1,
        size: 12,
      })
      .subscribe((response) => {
        expect(response.content).toEqual([]);
      });

    const req = httpMock.expectOne((request) => request.url === `${environment.apiUrl}/products/search`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('q')).toBe('desk');
    expect(req.request.params.getAll('category')).toEqual(['HOME', 'BOOKS']);
    expect(req.request.params.get('minPrice')).toBe('12.5');
    expect(req.request.params.get('maxPrice')).toBe('80');
    expect(req.request.params.get('sort')).toBe('price_asc');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('12');
    req.flush({
      success: true,
      message: 'Products retrieved successfully',
      data: { content: [], page: 1, size: 12, totalElements: 0, totalPages: 0 },
    });
  });

  it('fetches search facets from the facets endpoint', () => {
    service.getSearchFacets().subscribe((facets) => {
      expect(facets.minPrice).toBe(5);
      expect(facets.maxPrice).toBe(100);
      expect(facets.categories).toEqual([{ category: 'HOME', count: 2 }]);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/products/facets`);
    req.flush({
      success: true,
      message: 'Product search facets retrieved successfully',
      data: { categories: [{ category: 'HOME', count: 2 }], minPrice: 5, maxPrice: 100 },
    });
  });
});