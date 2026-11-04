import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

export function roleGuard(role: string): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    return authService.user()?.role === role ? true : router.createUrlTree(['/']);
  };
}
