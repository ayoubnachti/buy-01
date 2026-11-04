import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { SellerSalesStatsResponse } from '../models/seller-sales-stats.model';

@Injectable({
  providedIn: 'root',
})
export class SellerSalesStatsService {
  private readonly http = inject(HttpClient);

  getStats(from?: string, to?: string): Observable<SellerSalesStatsResponse> {
    let params = new HttpParams();
    if (from) {
      params = params.set('from', from);
    }
    if (to) {
      params = params.set('to', to);
    }

    return this.http.get<SellerSalesStatsResponse>(
      `${environment.apiUrl}/profile/seller/stats`,
      { params },
    );
  }
}
