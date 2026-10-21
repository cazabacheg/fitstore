import { Routes } from '@angular/router';
import { sinSesionGuard } from './sin-sesion.guard';

/** Pantallas públicas de acceso de la tienda: P-1-01 a P-1-04. */
export const ACCESO_ROUTES: Routes = [
  {
    path: 'ingresar',
    canActivate: [sinSesionGuard],
    loadComponent: () => import('./ingresar/ingresar.page').then((m) => m.IngresarPage),
  },
  {
    path: 'registro',
    loadComponent: () => import('./registro/registro.page').then((m) => m.RegistroPage),
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
