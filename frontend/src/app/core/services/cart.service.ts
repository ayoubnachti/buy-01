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

  addItem(productId: string, quantity = 1) {
    return this.http
      .post<ResponseData<CartResponse>>(`${this.apiUrl}/items`, { productId, quantity })
      .pipe(map((response) => response.data));
  }
}
