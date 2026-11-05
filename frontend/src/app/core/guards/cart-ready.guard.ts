import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { CartStore } from '../services/cart.store';

export const cartReadyGuard: CanActivateFn = () => {
  const cartStore = inject(CartStore);
  const router = inject(Router);

  return cartStore.load().pipe(
    map((cart) =>
      cart.items.length > 0 && cart.items.every((item) => item.available)
        ? true
        : router.createUrlTree(['/cart']),
    ),
    catchError(() => of(router.createUrlTree(['/cart']))),
  );
};