import { Routes } from '@angular/router';
import { Login } from './features/auth/components/login/login';
import { Register } from './features/auth/components/register/register';
import { Profile } from './features/profile/profile_component/profile';
import { guestGuard } from './core/guards/guest.guard';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/products/product-list/product-list').then(
        (m) => m.ProductList
      ),
  },
  {
    path: 'system-design',
    loadComponent: () =>
      import('./features/system-design/system-design').then(
        (m) => m.SystemDesign
      ),
  },

  {
    path: 'login', canActivate: [guestGuard],
    component: Login
  },
  {
    path: 'register', canActivate: [guestGuard],
    component: Register
  },
  {
    path: 'profile', canActivate: [authGuard],
    component: Profile
  }
];
