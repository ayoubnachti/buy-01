import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { CreateProductRequest } from '../../shared/models/create-product-request';
import { Product } from '../../shared/models/product.model';


@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/products';

  getAll() {
    return this.http.get<Product[]>(this.apiUrl);
  }

  create(request: CreateProductRequest) {
    return this.http.post<Product>(this.apiUrl, request);
  }

  update(id: string, request: CreateProductRequest) {
    return this.http.put<Product>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: string) {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}