import { Routes } from '@angular/router';

/**
 * Dashboard del backoffice (P-8-01): OPERADOR y ADMIN (la guarda de rol está en el shell, `app.routes.ts`).
 */
export const DASHBOARD_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./dashboard.page').then((m) => m.DashboardPage),
    data: { pantalla: 'dashboard' },
  },
  { path: '**', redirectTo: '' },
];
