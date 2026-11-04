import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { UserPurchaseStatsResponse } from '../models/user-purchase-stats.model';

@Injectable({
  providedIn: 'root',
})
export class UserPurchaseStatsService {
  private readonly http = inject(HttpClient);

  getStats(from?: string, to?: string): Observable<UserPurchaseStatsResponse> {
    let params = new HttpParams();
    if (from) {
      params = params.set('from', from);
    }
    if (to) {
      params = params.set('to', to);
    }

    return this.http.get<UserPurchaseStatsResponse>(
      `${environment.apiUrl}/profile/user/stats`,
      { params },
    );
  }
}
