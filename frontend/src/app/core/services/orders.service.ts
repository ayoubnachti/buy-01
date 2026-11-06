import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';

import { Order, OrderStatus } from '../../shared/models/order.model';
import { OrderPageResponse } from '../../shared/models/order-page-response';
import { ResponseData } from '../../shared/models/response-data';
import { environment } from '../../../environments/environment.prod';
import { CreateOrderRequest } from '../../shared/models/create-order-request';

export interface OrderSearchFilters {
  status?: OrderStatus;
  from?: string;
  to?: string;
  q?: string;
}

@Injectable({ providedIn: 'root' })
export class OrdersService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/orders`;

  create(request: CreateOrderRequest) {
    return this.http
      .post<ResponseData<Order[]>>(this.apiUrl, request)
      .pipe(map((response) => response.data));
  }

  getMy(page: number, size: number, filters: OrderSearchFilters = {}) {
    const params = this.buildParams(page, size, filters);

    return this.http
      .get<ResponseData<OrderPageResponse>>(`${this.apiUrl}/my`, { params })
      .pipe(map((response) => response.data));
  }

  getOne(id: string) {
    return this.http
      .get<ResponseData<Order>>(`${this.apiUrl}/${id}`)
      .pipe(map((response) => response.data));
  }

  cancel(id: string) {
    return this.http
      .post<ResponseData<Order>>(`${this.apiUrl}/${id}/cancel`, {})
      .pipe(map((response) => response.data));
  }

  redo(id: string) {
    return this.http
      .post<ResponseData<Order[]>>(`${this.apiUrl}/${id}/redo`, {})
      .pipe(map((response) => response.data));
  }

  remove(id: string) {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getSellerOrders(page: number, size: number, filters: OrderSearchFilters = {}) {
    const params = this.buildParams(page, size, filters);

    return this.http
      .get<ResponseData<OrderPageResponse>>(`${this.apiUrl}/seller`, { params })
      .pipe(map((response) => response.data));
  }

  updateStatus(id: string, status: OrderStatus) {
    return this.http
      .patch<ResponseData<Order>>(`${this.apiUrl}/${id}/status`, { status })
      .pipe(map((response) => response.data));
  }

  private buildParams(page: number, size: number, filters: OrderSearchFilters): HttpParams {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());

    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.from) {
      params = params.set('from', filters.from);
    }
    if (filters.to) {
      params = params.set('to', filters.to);
    }
    if (filters.q) {
      params = params.set('q', filters.q);
    }
    return params;
  }
}
