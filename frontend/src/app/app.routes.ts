import { Routes } from '@angular/router';
import { Login } from './features/auth/components/login/login';
import { Register } from './features/auth/components/register/register';
import { Profile } from './features/profile/profile_component/profile';
import { guestGuard } from './core/guards/guest.guard';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { cartReadyGuard } from './core/guards/cart-ready.guard';

// Protected routes: authGuard first (logged out -> login with returnUrl), then roleGuard (wrong role -> shop).
export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/products/product-search/product-search').then((m) => m.ProductSearch),
  },
  {
    path: 'search',
    redirectTo: '',
    pathMatch: 'full',
  },
  {
    path: 'system-design',
    loadComponent: () =>
      import('./features/system-design/system-design').then((m) => m.SystemDesign),
  },
  {
    path: 'products/:id',
    loadComponent: () =>
      import('./features/products/product-detail/product-detail').then((m) => m.ProductDetail),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard, roleGuard('SELLER')],
    loadComponent: () =>
      import('./features/seller-dashboard/seller-dashboard').then((m) => m.SellerDashboard),
  },
  {
    path: 'orders',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orders/orders-list/orders-list').then((m) => m.OrdersList),
  },
  {
    path: 'orders/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orders/order-details/order-details').then((m) => m.OrderDetails),
  },
  {
    path: 'seller/orders',
    canActivate: [authGuard, roleGuard('SELLER')],
    loadComponent: () =>
      import('./features/orders/seller-orders/seller-orders').then((m) => m.SellerOrders),
  },
  {
    path: 'profile/dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/profile/dashboard/client-dashboard').then((m) => m.ClientDashboard),
  },
  {
    path: 'seller/dashboard',
    canActivate: [authGuard, roleGuard('SELLER')],
    loadComponent: () =>
      import('./features/profile/dashboard/seller-dashboard').then(
        (m) => m.SellerAnalyticsDashboard,
      ),
  },
  {
    path: 'cart',
    canActivate: [authGuard, roleGuard('CLIENT')],
    loadComponent: () => import('./features/cart/cart-page/cart-page').then((m) => m.CartPage),
  },
  {
    path: 'checkout',
    canActivate: [authGuard, roleGuard('CLIENT'), cartReadyGuard],
    loadComponent: () =>
      import('./features/checkout/checkout-page/checkout-page').then((m) => m.CheckoutPage),
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    component: Login,
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    component: Register,
  },
  {
    path: 'profile',
    canActivate: [authGuard, roleGuard('SELLER')],
    component: Profile,
  },
  {
    path: '**',
    redirectTo: '',
  },
];
