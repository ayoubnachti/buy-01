import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { Product } from '../../../shared/models/product.model';
import { ProductService } from '../../../core/services/product.service';
import { Carousel } from '../../../shared/components/carousel/carousel';

@Component({
  selector: 'app-product-list',
  imports: [CurrencyPipe, Carousel],
  templateUrl: './product-list.html',
})
export class ProductList implements OnInit {
  private readonly productService = inject(ProductService);

  products = signal<Product[]>([]);
  loading = signal(true);
  error = signal(false);

  ngOnInit(): void {
    this.productService.getAll(undefined, 0, 100).subscribe({
      next: (response) => {
        this.products.set(response.items);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
}