import { DestroyRef, Component, ViewChild, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { of } from 'rxjs';

import { Product } from '../../shared/models/product.model';
import { CreateProductRequest } from '../../shared/models/create-product-request';
import { ProductForm } from '../products/product-form/product-form';
import { ProductTableRow } from '../products/product-table-row/product-table-row';
import { LoadingSpinner } from '../../shared/components/loading-spinner/loading-spinner';
import { EmptyState } from '../../shared/components/empty-state/empty-state';
import { ConfirmationModal } from '../../shared/components/confirmation-modal/confirmation-modal';
import { ProductService } from '../../core/services/product.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [ProductForm, ProductTableRow, LoadingSpinner, EmptyState, ConfirmationModal],
  templateUrl: './seller-dashboard.html',
})
export class SellerDashboard {
  private readonly productService = inject(ProductService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild(ProductForm) private productFormRef?: ProductForm;

  products = signal<Product[]>([]);
  loading = signal(true);
  saving = signal(false);
  editingProduct = signal<Product | null>(null);
  focusTrigger = signal(0);

  productPendingDelete = signal<Product | null>(null);

  constructor() {
    this.fetchProducts();
  }

  onCreateClick(): void {
    this.editingProduct.set(null);
    this.focusTrigger.update((n) => n + 1);
  }

  onEditClick(product: Product): void {
    this.editingProduct.set(product);
    this.focusTrigger.update((n) => n + 1);
  }

  onDeleteClick(product: Product): void {
    this.productPendingDelete.set(product);
  }

  onCancelDelete(): void {
    this.productPendingDelete.set(null);
  }

  onConfirmDelete(): void {
    const product = this.productPendingDelete();
    if (!product) {
      return;
    }

    this.productService
      .delete(product.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.products.update((list) => list.filter((p) => p.id !== product.id));
          this.toastService.success('Product deleted.');
          this.productPendingDelete.set(null);
        },
        error: () => {
          this.toastService.error('Could not delete this product. Try again.');
          this.productPendingDelete.set(null);
        },
      });
  }

  onFormSave(request: CreateProductRequest): void {
    if (this.saving()) {
      return;
    }

    const editing = this.editingProduct();
    const save$ = editing
      ? this.productService.update(editing.id, request)
      : this.productService.create(request);

    this.saving.set(true);

    // Product first: only attempt the media upload once the product itself
    // is confirmed saved (and we have a real product id to attach images to).
    save$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (savedProduct) => this.uploadPendingImages(savedProduct, editing),

      error: () => {
        this.saving.set(false);
        this.toastService.error(
          editing ? 'Could not update this product.' : 'Could not create this product.',
        );
      },
    });
  }

  private uploadPendingImages(savedProduct: Product, editing: Product | null): void {
    const commitImages$ = this.productFormRef?.commitImages(savedProduct.id) ?? of([]);

    commitImages$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (imageUrls) => {
        const finalProduct = imageUrls.length ? { ...savedProduct, imageUrls } : savedProduct;

        this.saving.set(false);
        this.applySavedProduct(finalProduct, editing);
        this.toastService.success(editing ? 'Product updated.' : 'Product created.');
      },

      error: () => {
        // The product itself is already saved at this point; only the
        // image attachment failed, so keep the product and let the user
        // retry the images from the edit form instead of losing the save.
        this.saving.set(false);
        this.applySavedProduct(savedProduct, editing);
        this.toastService.error(
          'Product saved, but the image(s) failed to upload. You can retry from the edit form.',
        );
      },
    });
  }

  private applySavedProduct(savedProduct: Product, editing: Product | null): void {
    this.products.update((list) =>
      editing ? list.map((p) => (p.id === editing.id ? savedProduct : p)) : [...list, savedProduct],
    );

    // Reset the form (same mechanism as Create/Edit clicks) so a
    // second, accidental Submit can't re-post the same data and
    // create a duplicate.
    this.editingProduct.set(null);
    this.focusTrigger.update((n) => n + 1);
  }

  private fetchProducts(): void {
    this.loading.set(true);
    this.productService
      .getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (products) => {
          this.products.set(products);
          this.loading.set(false);
        },
        error: () => {
          this.toastService.error('Could not load your products. Try refreshing.');
          this.loading.set(false);
        },
      });
  }
}