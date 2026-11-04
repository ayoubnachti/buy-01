import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { LoginRequest } from '../../features/auth/models/login-request';
import { AuthResponse } from '../../features/auth/models/auth-response';
import { RegisterRequest } from '../../features/auth/models/register-request';
import { RegisterResponse } from '../../features/auth/models/register-response';
import { User } from '../../features/auth/models/user.modelt';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = `${environment.apiUrl}/auth`;

  private readonly userSignal = signal<User | null>(null);

  readonly user = this.userSignal.asReadonly();

  login(request: LoginRequest) {
    return this.http.post<AuthResponse<string>>(`${this.apiUrl}/login`, request);
  }

  register(request: RegisterRequest) {
    return this.http.post<AuthResponse<RegisterResponse>>(`${this.apiUrl}/register`, request);
  }

  logout(): void {
    localStorage.removeItem('jwt');
    this.userSignal.set(null);
  }

  setUserFromToken(token: string): void {
    const user = this.decodeToken(token);
    if (!user) {
      return;
    }
    localStorage.setItem('jwt', token);
    this.userSignal.set(user);
  }

  public initializeUser(): void {
    const token = localStorage.getItem('jwt');
    if (!token) {
      return;
    }

    const user = this.decodeToken(token);
    if (user) {
      this.userSignal.set(user);
    } else {
      this.logout();
    }
  }

  private decodeToken(token: string): User | null {
    try {
      const payload = JSON.parse(this.base64UrlDecode(token.split('.')[1]));

      if (typeof payload.exp === 'number' && payload.exp * 1000 <= Date.now()) {
        return null;
      }

      return { id: payload.sub, role: payload.role };
    } catch {
      return null;
    }
  }

  private base64UrlDecode(value: string): string {
    const base64 = value.replace(/-/g, '+').replace(/_/g, '/');
    return atob(base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '='));
  }
}
