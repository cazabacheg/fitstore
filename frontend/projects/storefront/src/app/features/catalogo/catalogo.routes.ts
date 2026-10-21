import { Routes } from '@angular/router';

/** Rutas públicas del catálogo (R2): listado, categoría y detalle. */
export const CATALOGO_ROUTES: Routes = [
  { path: 'productos', loadComponent: () => import('./listado.page').then((m) => m.ListadoPage) },
  {
    path: 'categoria/:slug',
    loadComponent: () => import('./listado.page').then((m) => m.ListadoPage),
  },
  {
    path: 'producto/:slug',
    loadComponent: () => import('./detalle.page').then((m) => m.DetallePage),
  },
];
