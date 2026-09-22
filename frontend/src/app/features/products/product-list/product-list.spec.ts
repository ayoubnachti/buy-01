import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideZonelessChangeDetection } from '@angular/core';
import { describe, it, expect, beforeEach } from 'vitest';
import { ProductList } from './product-list';
import { Product } from '../../../shared/models/product.model';
import { environment } from '../../../../environments/environment';

describe('ProductList', () => {
  let httpMock: HttpTestingController;

  const mockProducts: Product[] = [
    {
      id: '1',
      name: 'chair',
      description: 'chair to sit on',
      price: 122.21,
      quantity: 12,
      userId: 'u1',
      imageUrls: [],
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    },
  ];

  const requestUrl = `${environment.apiUrl}/products?page=0&limit=100`;

  function wrapped(items: Product[]) {
    return {
      success: true,
      message: 'Products retrieved successfully',
      data: {
        items,
        currentPage: 0,
        pageSize: 100,
        totalElements: items.length,
        totalPages: 1,
        hasNext: false,
        hasPrevious: false,
      },
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ProductList],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('shows loading state before the response arrives', async () => {
    const fixture = TestBed.createComponent(ProductList);
    fixture.detectChanges();

    expect(fixture.componentInstance.loading()).toBe(true);

    httpMock.expectOne(requestUrl).flush(wrapped(mockProducts));
  });

  it('renders products once loaded, including a null imageUrls entry', async () => {
    const fixture = TestBed.createComponent(ProductList);
    fixture.detectChanges();

    httpMock.expectOne(requestUrl).flush(wrapped(mockProducts));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.loading()).toBe(false);
    expect(fixture.componentInstance.products()).toEqual(mockProducts);

    const cardTitle = fixture.nativeElement.querySelector('.card-title');
    expect(cardTitle?.textContent).toContain('chair');

    const img: HTMLImageElement = fixture.nativeElement.querySelector('img');
    expect(img).not.toBeNull();
    expect(img.alt).toBe('chair');
  });

  it('shows an error message when the request fails', async () => {
    const fixture = TestBed.createComponent(ProductList);
    fixture.detectChanges();

    httpMock
      .expectOne(requestUrl)
      .flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toBe(true);
    const alert = fixture.nativeElement.querySelector('.alert-danger');
    expect(alert).not.toBeNull();
  });
});