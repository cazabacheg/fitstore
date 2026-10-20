import { Routes } from '@angular/router';
import { Proximamente } from '../../ui/proximamente/proximamente';

/**
 * Marcador "Próximamente" (CA-0b-15): la spec dueña reemplaza el contenido de esta carpeta.
 * Cada pantalla de la carpeta tiene su propio arreglo: el shell (`app.routes.ts`) carga cada uno con su URL
 * y su guarda de rol, así dos pantallas de la misma carpeta nunca reciben la misma ruta.
 * `dia` va antes que cualquier `:id` (SPEC-6), para que no se tome como un id.
 */
export const ENTREGAS_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { pantalla: 'entregas' } },
  {
    path: 'dia',
    component: Proximamente,
    data: { pantalla: 'entregas-dia', titulo: 'Entregas del día', migas: ['Entregas', 'Del día'] },
  },
  { path: '**', component: Proximamente, data: { pantalla: 'entregas' } },
];

export const REPARTIDORES_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { pantalla: 'repartidores' } },
  { path: '**', component: Proximamente, data: { pantalla: 'repartidores' } },
];
