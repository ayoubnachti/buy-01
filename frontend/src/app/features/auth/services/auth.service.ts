import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { LoginRequest } from '../models/login-request';
import { AuthResponse } from '../models/auth-response';
import { RegisterRequest } from '../models/register-request';
import { RegisterResponse } from '../models/register-response';

@Injectable({
    providedIn: 'root'
})
export class AuthService {

    private readonly http = inject(HttpClient);

    private readonly apiUrl = 'http://localhost:8080/auth';


    logout(): void {
        localStorage.removeItem('jwt');
        // If you store the user separately, clear it here too

        window.location.href = '/login';
    }

    login(request: LoginRequest) {
        return this.http.post<AuthResponse<string>>(
            `${this.apiUrl}/login`,
            request
        );
    }


    register(request: RegisterRequest) {
        return this.http.post<AuthResponse<RegisterResponse>>(
            `${this.apiUrl}/register`,
            request
        );
    }

}