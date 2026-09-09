import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'system-design',
    loadComponent: () =>
      import('./features/system-design/system-design').then((m) => m.SystemDesign),
  },
];
