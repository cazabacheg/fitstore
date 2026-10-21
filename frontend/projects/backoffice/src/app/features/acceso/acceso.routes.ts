import { Routes } from '@angular/router';
import { sinSesionGuard } from './sin-sesion.guard';

/** Pantallas públicas del backoffice, fuera del shell: P-1-07, P-1-03 y P-1-04 (con los endpoints de staff). */
export const ACCESO_ROUTES: Routes = [
  {
    path: 'ingresar',
    canActivate: [sinSesionGuard],
    loadComponent: () => import('./ingresar/ingresar-staff.page').then((m) => m.IngresarStaffPage),
  },
  {
    path: 'recuperar',
    loadComponent: () => import('./recuperar/recuperar.page').then((m) => m.RecuperarPage),
  },
  {
    path: 'restablecer',
    loadComponent: () => import('./restablecer/restablecer.page').then((m) => m.RestablecerPage),
  },
];
