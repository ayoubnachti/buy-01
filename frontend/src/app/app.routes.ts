import { Routes } from '@angular/router';
import { Login } from './features/auth/components/login/login';
import { Register } from './features/auth/components/register/register';

export const routes: Routes = [
  {
    path: 'system-design',
    loadComponent: () =>
      import('./features/system-design/system-design').then((m) => m.SystemDesign),
  },

  {
    path: 'login',
    component: Login
  },

  {
    path: 'register',
    component: Register
  }


];
