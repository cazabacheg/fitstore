import { Routes } from '@angular/router';
import { Proximamente } from '../../ui/proximamente/proximamente';

/**
 * Marcador "Próximamente" (CA-0b-15): la spec dueña reemplaza el contenido de esta carpeta.
 * Cada pantalla de la carpeta tiene su propio arreglo: el shell (`app.routes.ts`) carga cada uno con su URL
 * y su guarda de rol, así dos pantallas de la misma carpeta nunca reciben la misma ruta.
 */
export const PRODUCTOS_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { pantalla: 'productos' } },
  { path: '**', component: Proximamente, data: { pantalla: 'productos' } },
];

export const CARGA_MASIVA_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { pantalla: 'carga-masiva' } },
  { path: '**', component: Proximamente, data: { pantalla: 'carga-masiva' } },
];

export const CATEGORIAS_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { pantalla: 'categorias' } },
  { path: '**', component: Proximamente, data: { pantalla: 'categorias' } },
];

export const MAESTROS_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { pantalla: 'maestros' } },
  { path: '**', component: Proximamente, data: { pantalla: 'maestros' } },
];

export const DESCUENTOS_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { pantalla: 'descuentos' } },
  { path: '**', component: Proximamente, data: { pantalla: 'descuentos' } },
];
