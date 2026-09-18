import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { LoginRequest } from '../../features/auth/models/login-request';
import { AuthResponse } from '../../features/auth/models/auth-response';
import { RegisterRequest } from '../../features/auth/models/register-request';
import { RegisterResponse } from '../../features/auth/models/register-response';
import { User } from '../../features/auth/models/user.modelt';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/auth';

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
    localStorage.removeItem('user');

    this.userSignal.set(null);

    window.location.href = '/login';
  }

  setUserFromToken(token: string): void {
    localStorage.setItem('jwt', token);

    const user = this.decodeToken(token);

    if (user) {
      this.userSignal.set(user);
      localStorage.setItem('user', JSON.stringify(user));
    }
  }

  public initializeUser(): void {
    const token = localStorage.getItem('jwt');

    if (!token) {
      return;
    }

    const user = this.decodeToken(token);

    if (user) {
      this.userSignal.set(user);
      localStorage.setItem('user', JSON.stringify(user));
    } else {
      this.logout();
    }
  }

  private decodeToken(token: string): User | null {
    try {
      const payload = token.split('.')[1];

      const decodedPayload = JSON.parse(atob(payload));

      return {
        id: decodedPayload.sub,
        name: decodedPayload.name,
        email: decodedPayload.email,
        role: decodedPayload.role,
      };
    } catch {
      return null;
    }
  }
}
