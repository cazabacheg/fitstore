import { Routes } from '@angular/router';
import { rolGuard } from 'shared';
import { Proximamente } from '../../ui/proximamente/proximamente';

/**
 * Cada pantalla de la carpeta tiene su propio arreglo: el shell (`app.routes.ts`) carga cada uno con su URL y su guarda.
 * La carga masiva (P-2-09) pasó a extensión de alcance y sigue con "Próximamente".
 */
export const PRODUCTOS_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./productos.page').then((m) => m.ProductosPage) },
  // Crear es solo del ADMIN; `nuevo` va antes de `:id`.
  {
    path: 'nuevo',
    canActivate: [rolGuard('ADMIN')],
    loadComponent: () => import('./producto-form.page').then((m) => m.ProductoFormPage),
  },
  {
    path: ':id',
    loadComponent: () => import('./producto-form.page').then((m) => m.ProductoFormPage),
  },
];

export const CARGA_MASIVA_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { pantalla: 'carga-masiva' } },
  { path: '**', component: Proximamente, data: { pantalla: 'carga-masiva' } },
];

export const CATEGORIAS_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./categorias.page').then((m) => m.CategoriasPage) },
];

export const MAESTROS_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { pantalla: 'maestros' } },
  { path: '**', component: Proximamente, data: { pantalla: 'maestros' } },
];

export const DESCUENTOS_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { pantalla: 'descuentos' } },
  { path: '**', component: Proximamente, data: { pantalla: 'descuentos' } },
];
