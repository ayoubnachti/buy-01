import { CurrencyPipe, TitleCasePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, EMPTY, switchMap, tap } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { ProductSearchParams, ProductService } from '../../../core/services/product.service';
import { ToastService } from '../../../core/services/toast.service';
import { Carousel } from '../../../shared/components/carousel/carousel';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';
import { Product } from '../../../shared/models/product.model';
import { ProductSearchFacets } from '../../../shared/models/product-page-response';
import { toApiError } from '../../../shared/models/api-error';

type SearchSort = NonNullable<ProductSearchParams['sort']>;
type SearchFormValue = {
  q: string;
  category: Product['category'][];
  minPrice: number | null;
  maxPrice: number | null;
  sort: SearchSort;
  page: number;
};

const PAGE_SIZE = 12;

@Component({
  selector: 'app-product-search',
  imports: [
    CurrencyPipe,
    TitleCasePipe,
    ReactiveFormsModule,
    RouterLink,
    Carousel,
    EmptyState,
    LoadingSpinner,
  ],
  templateUrl: './product-search.html',
  styleUrl: './product-search.css',
})
export class ProductSearch implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly categories: Product['category'][] = [
    'ELECTRONICS',
    'CLOTHING',
    'BOOKS',
    'HOME',
    'SPORTS',
    'OTHER',
  ];
  readonly products = signal<Product[]>([]);
  readonly facets = signal<ProductSearchFacets | null>(null);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly filtersOpen = signal(false);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly page = signal(0);
  readonly isClient = computed(() => this.authService.user()?.role === 'CLIENT');

  readonly filtersForm = new FormGroup({
    q: new FormControl('', { nonNullable: true }),
    category: new FormControl<Product['category'][]>([], { nonNullable: true }),
    minPrice: new FormControl<number | null>(null),
    maxPrice: new FormControl<number | null>(null),
    sort: new FormControl<SearchSort>('relevance', { nonNullable: true }),
    page: new FormControl(0, { nonNullable: true }),
  });

  ngOnInit(): void {
    this.patchFormFromParams(this.route.snapshot.queryParamMap);

    this.productService
      .getSearchFacets()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (facets) => this.facets.set(facets),
        error: () => this.error.set(true),
      });

    this.route.queryParamMap
      .pipe(
        tap((params) => {
          this.patchFormFromParams(params);
          this.page.set(this.parsePage(params.get('page')));
          this.loading.set(true);
          this.error.set(false);
        }),
        switchMap((params) =>
          this.productService.search(this.searchParamsFromParams(params)).pipe(
            tap((response) => {
              this.products.set(response.content);
              this.totalElements.set(response.totalElements);
              this.totalPages.set(response.totalPages);
              this.loading.set(false);
            }),
            catchError(() => {
              this.error.set(true);
              this.loading.set(false);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.filtersForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(
          (previous, current) => JSON.stringify(previous) === JSON.stringify(current),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((value) => this.updateUrl(value as SearchFormValue));
  }

  get minimumPrice(): number {
    return this.filtersForm.controls.minPrice.value ?? this.facets()?.minPrice ?? 0;
  }

  get maximumPrice(): number {
    return this.filtersForm.controls.maxPrice.value ?? this.facets()?.maxPrice ?? 0;
  }

  get priceFloor(): number {
    return this.facets()?.minPrice ?? 0;
  }

  get priceCeiling(): number {
    return this.facets()?.maxPrice ?? this.priceFloor;
  }

  get hasFilters(): boolean {
    const value = this.filtersForm.getRawValue();
    return Boolean(
      value.q ||
        value.category.length ||
        value.minPrice !== null ||
        value.maxPrice !== null ||
        value.sort !== 'relevance',
    );
  }

  toggleFilters(): void {
    this.filtersOpen.update((open) => !open);
  }

  toggleCategory(category: Product['category'], checked: boolean): void {
    const selected = this.filtersForm.controls.category.value;
    const next = checked
      ? [...new Set([...selected, category])]
      : selected.filter((item) => item !== category);
    this.filtersForm.controls.category.setValue(next);
  }

  onCategoryChange(category: Product['category'], event: Event): void {
    this.toggleCategory(category, (event.target as HTMLInputElement).checked);
  }

  setPriceBound(bound: 'minPrice' | 'maxPrice', value: number): void {
    const otherBound = bound === 'minPrice' ? 'maxPrice' : 'minPrice';
    const otherValue = this.filtersForm.controls[otherBound].value;
    this.filtersForm.controls[bound].setValue(value);

    if (otherValue !== null && (bound === 'minPrice' ? value > otherValue : value < otherValue)) {
      this.filtersForm.controls[otherBound].setValue(value);
    }
  }

  clearFilters(): void {
    this.filtersForm.reset({
      q: '',
      category: [],
      minPrice: null,
      maxPrice: null,
      sort: 'relevance',
      page: 0,
    });
  }

  goToPage(page: number): void {
    this.filtersForm.controls.page.setValue(page);
  }

  addToCart(product: Product): void {
    this.cartService.addItem(product.id).subscribe({
      next: () => this.toastService.success(`${product.name} added to cart.`),
      error: (e) => {
        const err = toApiError(e, `Couldn't add ${product.name} to your cart.`);
        if (!err.handledGlobally) {
          this.toastService.error(err.message);
        }
      },
    });
  }

  private patchFormFromParams(params: ParamMap): void {
    this.filtersForm.patchValue(
      {
        q: params.get('q') ?? '',
        category: this.categories.filter((category) => params.getAll('category').includes(category)),
        minPrice: this.parsePrice(params.get('minPrice')),
        maxPrice: this.parsePrice(params.get('maxPrice')),
        sort: this.parseSort(params.get('sort')),
        page: this.parsePage(params.get('page')),
      },
      { emitEvent: false },
    );
  }

  private searchParamsFromParams(params: ParamMap): ProductSearchParams {
    const search: ProductSearchParams = {
      page: this.parsePage(params.get('page')),
      size: PAGE_SIZE,
    };
    const query = params.get('q')?.trim();
    const categories = this.categories.filter((category) => params.getAll('category').includes(category));
    const minPrice = this.parsePrice(params.get('minPrice'));
    const maxPrice = this.parsePrice(params.get('maxPrice'));
    const sort = this.parseSort(params.get('sort'));

    if (query) search.q = query;
    if (categories.length) search.category = categories;
    if (minPrice !== null) search.minPrice = minPrice;
    if (maxPrice !== null) search.maxPrice = maxPrice;
    if (sort !== 'relevance') search.sort = sort;
    return search;
  }

  private updateUrl(value: SearchFormValue): void {
    const queryParams: Record<string, string | number | string[] | null> = {
      q: value.q.trim() || null,
      category: value.category.length ? value.category : null,
      minPrice: value.minPrice,
      maxPrice: value.maxPrice,
      sort: value.sort === 'relevance' ? null : value.sort,
      page: value.page || null,
    };
    const current = this.route.snapshot.queryParamMap;
    const filtersChanged =
      value.q.trim() !== (current.get('q') ?? '') ||
      value.category.join(',') !== current.getAll('category').join(',') ||
      String(value.minPrice ?? '') !== (current.get('minPrice') ?? '') ||
      String(value.maxPrice ?? '') !== (current.get('maxPrice') ?? '') ||
      value.sort !== this.parseSort(current.get('sort'));

    if (filtersChanged && value.page !== 0) {
      queryParams['page'] = null;
    }

    void this.router.navigate(['/search'], { queryParams, replaceUrl: true });
  }

  private parsePrice(value: string | null): number | null {
    if (value === null || value.trim() === '') return null;
    const parsed = Number(value);
    return Number.isFinite(parsed) && parsed >= 0 ? parsed : null;
  }

  private parsePage(value: string | null): number {
    if (value === null) return 0;
    const parsed = Number(value);
    return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0;
  }

  private parseSort(value: string | null): SearchSort {
    if (value === 'price_asc' || value === 'price_desc' || value === 'newest') return value;
    return 'relevance';
  }
}
