import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

const AUTH_ENDPOINTS = ['/auth/login', '/auth/register'];

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const isAuthCall = AUTH_ENDPOINTS.some((path) => req.url.endsWith(path));

      if (error.status === 401 && !isAuthCall) {
        if (!router.url.startsWith('/login')) {
          authService.logout();
          toast.info('Your session has expired. Please log in again.');
          router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
        }
      } else if (error.status === 403) {
        toast.error('You are not allowed to do this.');
      } else if (error.status === 0) {
        toast.error("Can't reach the server. Check your connection and try again.");
      } else if (error.status >= 500) {
        toast.error('Something went wrong on our side. Please try again.');
      }
      // 400, 404, 409 (and 401 on login/register): left to the page.

      return throwError(() => error);
    }),
  );
};