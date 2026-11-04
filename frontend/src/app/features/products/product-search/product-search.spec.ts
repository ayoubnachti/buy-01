import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { convertToParamMap } from '@angular/router';
import { BehaviorSubject, finalize, of, Subject, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { ProductService } from '../../../core/services/product.service';
import { ToastService } from '../../../core/services/toast.service';
import { Product } from '../../../shared/models/product.model';
import { ProductSearch } from './product-search';

describe('ProductSearch', () => {
  let routeParams: BehaviorSubject<ParamMap>;
  let productService: {
    getSearchFacets: ReturnType<typeof vi.fn>;
    search: ReturnType<typeof vi.fn>;
  };
  let cartService: { addItem: ReturnType<typeof vi.fn> };
  let authService: { user: ReturnType<typeof vi.fn> };
  let toastService: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };
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

  const products: Product[] = [
    {
      id: 'p1',
      name: 'Desk lamp',
      description: 'A warm light for a desk',
      price: 25,
      quantity: 3,
      category: 'HOME',
      imageUrls: ['https://example.com/lamp.png'],
    },
    {
      id: 'p2',
      name: 'Book light',
      description: 'A compact lamp',
      price: 10,
      quantity: 0,
      category: 'BOOKS',
    },
  ];

  beforeEach(() => {
    routeParams = new BehaviorSubject(convertToParamMap({}));
    productService = {
      getSearchFacets: vi.fn(() => of(facets)),
      search: vi.fn(() => of(emptyResults)),
    };
    cartService = { addItem: vi.fn(() => of({})) };
    authService = { user: vi.fn(() => null) };
    toastService = { success: vi.fn(), error: vi.fn() };
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
        { provide: CartService, useValue: cartService },
        { provide: AuthService, useValue: authService },
        { provide: ToastService, useValue: toastService },
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
    (fixture.nativeElement.querySelector('app-empty-state button') as HTMLButtonElement).click();
    expect(fixture.componentInstance.filtersForm.controls.q.value).toBe('');
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

  it('renders facets, product cards, client cart controls and pagination', () => {
    authService.user.mockReturnValue({ role: 'CLIENT' });
    productService.search.mockReturnValue(
      of({ ...emptyResults, content: products, totalElements: 2, totalPages: 2 }),
    );
    const fixture = TestBed.createComponent(ProductSearch);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.form-check-input')).toHaveLength(1);
    expect(fixture.nativeElement.querySelectorAll('.card-title')).toHaveLength(2);
    expect(fixture.nativeElement.querySelector('.product-image-placeholder')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-carousel')).not.toBeNull();

    const addButton = fixture.nativeElement.querySelector(
      '.product-card button:not([disabled])',
    ) as HTMLButtonElement;
    expect(addButton.textContent).toContain('Add to cart');
    addButton.click();
    expect(cartService.addItem).toHaveBeenCalledWith('p1');
    expect(toastService.success).toHaveBeenCalledWith('Desk lamp added to cart.');

    const unavailableButton = Array.from(
      fixture.nativeElement.querySelectorAll('.product-card button'),
    ).find((button) => (button as HTMLButtonElement).disabled) as HTMLButtonElement;
    expect(unavailableButton.textContent).toContain('Out of stock');

    const nextPage = fixture.nativeElement.querySelector(
      '[aria-label="Next page"]',
    ) as HTMLButtonElement;
    nextPage.click();
    expect(fixture.componentInstance.filtersForm.controls.page.value).toBe(1);

    routeParams.next(convertToParamMap({ page: '1' }));
    fixture.detectChanges();
    const previousPage = fixture.nativeElement.querySelector(
      '[aria-label="Previous page"]',
    ) as HTMLButtonElement;
    previousPage.click();
    expect(fixture.componentInstance.filtersForm.controls.page.value).toBe(0);
    expect(
      (fixture.nativeElement.querySelector('[aria-label="Next page"]') as HTMLButtonElement)
        .disabled,
    ).toBe(true);

    const filtersToggle = fixture.nativeElement.querySelector(
      '[aria-controls="search-filters"]',
    ) as HTMLButtonElement;
    filtersToggle.click();
    expect(fixture.componentInstance.filtersOpen()).toBe(true);

    const categoryCheckbox = fixture.nativeElement.querySelector(
      '.form-check-input',
    ) as HTMLInputElement;
    categoryCheckbox.checked = true;
    categoryCheckbox.dispatchEvent(new Event('change'));
    expect(fixture.componentInstance.filtersForm.controls.category.value).toEqual(['HOME']);
    categoryCheckbox.checked = false;
    categoryCheckbox.dispatchEvent(new Event('change'));
    expect(fixture.componentInstance.filtersForm.controls.category.value).toEqual([]);

    const minSlider = fixture.nativeElement.querySelector('#minimum-price') as HTMLInputElement;
    minSlider.value = '40';
    minSlider.dispatchEvent(new Event('input'));
    expect(fixture.componentInstance.filtersForm.controls.minPrice.value).toBe(40);
    expect(fixture.componentInstance.filtersForm.controls.maxPrice.value).toBeNull();

    const maxSlider = fixture.nativeElement.querySelector('#maximum-price') as HTMLInputElement;
    maxSlider.value = '30';
    maxSlider.dispatchEvent(new Event('input'));
    expect(fixture.componentInstance.filtersForm.controls.maxPrice.value).toBe(30);
    expect(fixture.componentInstance.filtersForm.controls.minPrice.value).toBe(30);

    fixture.componentInstance.filtersForm.controls.q.setValue('lamp');
    const resetButton = fixture.nativeElement.querySelector(
      '#search-filters button',
    ) as HTMLButtonElement;
    resetButton.click();
    expect(fixture.componentInstance.filtersForm.getRawValue()).toEqual({
      q: '',
      category: [],
      minPrice: null,
      maxPrice: null,
      sort: 'relevance',
      page: 0,
    });
  });

  it('shows the request error state when searching fails', () => {
    productService.search.mockReturnValue(throwError(() => new Error('Search failed')));
    const fixture = TestBed.createComponent(ProductSearch);
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain("Couldn't load products.");
  });

  it('reports add-to-cart failures', () => {
    cartService.addItem.mockReturnValue(throwError(() => new Error('Cart unavailable')));
    const fixture = TestBed.createComponent(ProductSearch);
    fixture.componentInstance.addToCart(products[0]);

    expect(toastService.error).toHaveBeenCalledWith("Couldn't add Desk lamp to your cart.");
  });

  it('normalizes invalid URL values and falls back when facets are unavailable', () => {
    const invalidParams = convertToParamMap({
      q: '  ',
      category: 'UNKNOWN',
      minPrice: '-4',
      maxPrice: 'invalid',
      sort: 'unsupported',
      page: '-2',
    });
    routeParams.next(invalidParams);
    TestBed.overrideProvider(ActivatedRoute, {
      useValue: {
        snapshot: { queryParamMap: invalidParams },
        queryParamMap: routeParams,
      },
    });
    productService.getSearchFacets.mockReturnValue(
      throwError(() => new Error('Facets unavailable')),
    );

    const fixture = TestBed.createComponent(ProductSearch);
    fixture.detectChanges();

    expect(productService.search).toHaveBeenCalledWith({ page: 0, size: 12 });
    expect(fixture.componentInstance.minimumPrice).toBe(0);
    expect(fixture.componentInstance.maximumPrice).toBe(0);
    expect(fixture.nativeElement.textContent).toContain('No categories available.');
  });

  it('resets pagination when filters change and preserves page when they do not', async () => {
    const params = convertToParamMap({
      q: 'lamp',
      category: 'HOME',
      minPrice: '10',
      maxPrice: '100',
      sort: 'newest',
      page: '2',
    });
    routeParams.next(params);
    TestBed.overrideProvider(ActivatedRoute, {
      useValue: { snapshot: { queryParamMap: params }, queryParamMap: routeParams },
    });

    const fixture = TestBed.createComponent(ProductSearch);
    fixture.detectChanges();
    vi.useFakeTimers();
    fixture.componentInstance.filtersForm.controls.page.setValue(2);
    await vi.advanceTimersByTimeAsync(300);
    expect(router.navigate).toHaveBeenLastCalledWith(
      ['/search'],
      expect.objectContaining({ queryParams: expect.objectContaining({ page: 2 }) }),
    );

    fixture.componentInstance.filtersForm.controls.sort.setValue('price_desc');
    await vi.advanceTimersByTimeAsync(300);
    expect(router.navigate).toHaveBeenLastCalledWith(
      ['/search'],
      expect.objectContaining({ queryParams: expect.objectContaining({ page: null }) }),
    );
  });


  
});
