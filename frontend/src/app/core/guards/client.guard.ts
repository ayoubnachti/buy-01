import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const clientGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const user = authService.user();

  if (!user) {
    return router.createUrlTree(['/login']);
  }

  if (user.role === 'CLIENT') {
    return true;
  }

  return router.createUrlTree(['/']);
};