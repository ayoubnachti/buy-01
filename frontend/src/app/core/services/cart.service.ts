import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CartResponse } from '../../shared/models/cart-response';
import { ResponseData } from '../../shared/models/response-data';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/cart`;

  getCart() {
    return this.http.get<ResponseData<CartResponse>>(this.apiUrl).pipe(map((r) => r.data));
  }

  addItem(productId: string, quantity = 1) {
    return this.http
      .post<ResponseData<CartResponse>>(`${this.apiUrl}/items`, { productId, quantity })
      .pipe(map((r) => r.data));
  }

  updateQuantity(productId: string, quantity: number) {
    return this.http
      .put<ResponseData<CartResponse>>(`${this.apiUrl}/items/${encodeURIComponent(productId)}`, { quantity })
      .pipe(map((r) => r.data));
  }

  removeItem(productId: string) {
    return this.http
      .delete<ResponseData<CartResponse>>(`${this.apiUrl}/items/${encodeURIComponent(productId)}`)
      .pipe(map((r) => r.data));
  }

  clear() {
    return this.http.delete<void>(this.apiUrl);
  }
}
