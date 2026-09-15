import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
    AuthResponse,
  ProfileResponse,
  UpdateProfileRequest
} from '../models/profile.model';

@Injectable({
  providedIn: 'root'
})
export class ProfileService {

  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080';

  getProfile(): Observable<AuthResponse<ProfileResponse>> {

    const token = localStorage.getItem('jwt');

    let headers = new HttpHeaders();

    if (token) {
      headers = headers.set(
        'Authorization',
        `Bearer ${token}`
      );
    }

    return this.http.get<AuthResponse<ProfileResponse>>(
      `${this.apiUrl}/MyProfile`,
      { headers }
    );
  }

  updateProfile(
    data: UpdateProfileRequest
  ): Observable<AuthResponse<ProfileResponse>> {

    const token = localStorage.getItem('jwt');

    let headers = new HttpHeaders();

    if (token) {
      headers = headers.set(
        'Authorization',
        `Bearer ${token}`
      );
    }

    return this.http.put<AuthResponse<ProfileResponse>>(
      `${this.apiUrl}/MyProfile`,
      data,
      { headers }
    );
  }
}