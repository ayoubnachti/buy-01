import { Component, signal } from '@angular/core';
import { Product } from '../../shared/models/product.model';
import { ProductForm } from '../products/product-form/product-form';
import { ProductTableRow } from '../products/product-table-row/product-table-row';
import { CreateProductRequest } from '../../shared/models/create-product-request';

const MOCK_PRODUCTS: Product[] = [
  { id: '1212', name: 'chair', description: 'A sturdy wooden chair.', price: 21, quantity: 20 },
];

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [ProductForm, ProductTableRow],
  templateUrl: './seller-dashboard.html',
})
export class SellerDashboard {
  products = signal<Product[]>(MOCK_PRODUCTS);
  editingProduct = signal<Product | null>(null);

  focusTrigger = signal(0);

  onCreateClick(): void {
    this.editingProduct.set(null);
    this.focusTrigger.update((n) => n + 1);
  }

  onEditClick(product: Product): void {
    this.editingProduct.set(product);
    this.focusTrigger.update((n) => n + 1);
  }

  onDeleteClick(product: Product): void {
    this.products.update((list) => list.filter((p) => p.id !== product.id));
  }

  onFormSave(request: CreateProductRequest): void {
    const editing = this.editingProduct();
    if (editing) {
      this.products.update((list) =>
        list.map((p) => (p.id === editing.id ? { ...p, ...request } : p))
      );
    } else {
      this.products.update((list) => [...list, { ...request, id: crypto.randomUUID() }]);
    }
    this.editingProduct.set(null);
  }
}
