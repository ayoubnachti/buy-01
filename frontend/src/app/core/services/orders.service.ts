import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';

import { OrderPageResponse } from '../../shared/models/order-page-response';
import { ResponseData } from '../../shared/models/response-data';
import { environment } from '../../../environments/environment.prod';

@Injectable({ providedIn: 'root' })
export class OrdersService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/orders`;

  getMy(page: number, size: number) {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());

    return this.http
      .get<ResponseData<OrderPageResponse>>(`${this.apiUrl}/my`, { params })
      .pipe(map((response) => response.data));
  }
}
