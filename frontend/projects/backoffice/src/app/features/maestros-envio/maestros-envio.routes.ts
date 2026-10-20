import { Routes } from '@angular/router';
import { Proximamente } from '../../ui/proximamente/proximamente';

/**
 * Marcador "Próximamente" (CA-0b-15): la spec dueña reemplaza el contenido de esta carpeta.
 * Cada pantalla de la carpeta tiene su propio arreglo: el shell (`app.routes.ts`) carga cada uno con su URL
 * y su guarda de rol, así dos pantallas de la misma carpeta nunca reciben la misma ruta.
 */
export const TARIFAS_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { pantalla: 'tarifas' } },
  { path: '**', component: Proximamente, data: { pantalla: 'tarifas' } },
];

export const AGENCIAS_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { pantalla: 'agencias' } },
  { path: '**', component: Proximamente, data: { pantalla: 'agencias' } },
];
