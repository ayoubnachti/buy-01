import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { convertToParamMap } from '@angular/router';
import { BehaviorSubject, finalize, of, Subject } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { ProductService } from '../../../core/services/product.service';
import { ToastService } from '../../../core/services/toast.service';
import { ProductSearch } from './product-search';

describe('ProductSearch', () => {
  let routeParams: BehaviorSubject<ParamMap>;
  let productService: {
    getSearchFacets: ReturnType<typeof vi.fn>;
    search: ReturnType<typeof vi.fn>;
  };
  let router: { navigate: ReturnType<typeof vi.fn> };

  const facets = {
    categories: [{ category: 'HOME' as const, count: 2 }],
    minPrice: 10,
    maxPrice: 100,
  };

  const emptyResults = {
    content: [],
    page: 0,
    size: 12,
    totalElements: 0,
    totalPages: 0,
  };

  beforeEach(() => {
    routeParams = new BehaviorSubject(convertToParamMap({}));
    productService = {
      getSearchFacets: vi.fn(() => of(facets)),
      search: vi.fn(() => of(emptyResults)),
    };
    router = { navigate: vi.fn(() => Promise.resolve(true)) };

    TestBed.configureTestingModule({
      imports: [ProductSearch],
      providers: [
        provideZonelessChangeDetection(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: convertToParamMap({}) },
            queryParamMap: routeParams,
          },
        },
        { provide: Router, useValue: router },
        { provide: ProductService, useValue: productService },
        { provide: CartService, useValue: { addItem: vi.fn(() => of({})) } },
        { provide: AuthService, useValue: { user: () => null } },
        { provide: ToastService, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('hydrates the reactive filters and search params from the URL', () => {
    const urlParams = convertToParamMap({
      q: 'lamp',
      category: ['BOOKS', 'HOME'],
      minPrice: '20',
      sort: 'price_asc',
      page: '1',
    });
    routeParams.next(urlParams);
    TestBed.overrideProvider(ActivatedRoute, {
      useValue: {
        snapshot: { queryParamMap: urlParams },
        queryParamMap: routeParams,
      },
    });

    const fixture = TestBed.createComponent(ProductSearch);
    fixture.detectChanges();

    expect(fixture.componentInstance.filtersForm.getRawValue()).toEqual({
      q: 'lamp',
      category: ['BOOKS', 'HOME'],
      minPrice: 20,
      maxPrice: null,
      sort: 'price_asc',
      page: 1,
    });
    expect(productService.search).toHaveBeenCalledWith({
      q: 'lamp',
      category: ['BOOKS', 'HOME'],
      minPrice: 20,
      sort: 'price_asc',
      page: 1,
      size: 12,
    });
  });

  it('writes form changes to the URL and searches with the updated parameters', async () => {
    const fixture = TestBed.createComponent(ProductSearch);
    fixture.detectChanges();
    vi.useFakeTimers();
    fixture.componentInstance.filtersForm.controls.q.setValue('coat');

    await vi.advanceTimersByTimeAsync(300);

    expect(router.navigate).toHaveBeenCalledWith(
      ['/search'],
      expect.objectContaining({
        queryParams: expect.objectContaining({ q: 'coat' }),
        replaceUrl: true,
      }),
    );

    routeParams.next(convertToParamMap({ q: 'coat' }));
    expect(productService.search).toHaveBeenLastCalledWith({
      q: 'coat',
      page: 0,
      size: 12,
    });
  });

  it('renders the empty-results message with a clear-filters action', () => {
    const fixture = TestBed.createComponent(ProductSearch);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No products found.');
    expect(fixture.nativeElement.textContent).toContain('Clear filters');
  });

  it('cancels the previous search when the URL filters change', () => {
    const firstSearch = new Subject<typeof emptyResults>();
    let firstSearchCancelled = false;
    productService.search
      .mockImplementationOnce(() =>
        firstSearch.pipe(finalize(() => (firstSearchCancelled = true))),
      )
      .mockImplementationOnce(() => of(emptyResults));

    const fixture = TestBed.createComponent(ProductSearch);
    fixture.detectChanges();
    expect(productService.search).toHaveBeenCalledTimes(1);

    routeParams.next(convertToParamMap({ q: 'new query' }));

    expect(firstSearchCancelled).toBe(true);
    expect(productService.search).toHaveBeenCalledTimes(2);
    fixture.destroy();
  });
});
