import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ProfileResponse, UpdateProfileRequest } from '../models/profile.model';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root',
})
export class ProfileService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080';

  /**
   * Get the currently authenticated user's profile.
   */
  getProfile(): Observable<ApiResponse<ProfileResponse>> {
    return this.http.get<ApiResponse<ProfileResponse>>(`${this.apiUrl}/MyProfile`, {
      headers: this.getAuthHeaders(),
    });
  }

  /**
   * Update the currently authenticated user's profile.
   */
  updateProfile(data: UpdateProfileRequest): Observable<ApiResponse<ProfileResponse>> {
    return this.http.put<ApiResponse<ProfileResponse>>(`${this.apiUrl}/MyProfile`, data, {
      headers: this.getAuthHeaders(),
    });
  }

  /**
   * Add JWT Authorization header.
   */
  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwt');

    let headers = new HttpHeaders();

    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    return headers;
  }
}
