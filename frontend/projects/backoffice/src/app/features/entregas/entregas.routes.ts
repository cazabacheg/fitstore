import { Routes } from '@angular/router';
import { Proximamente } from '../../ui/proximamente/proximamente';
import { DetalleEntregaPage } from './detalle-entrega.page';
import { EntregasPage } from './entregas.page';
import { RepartidoresPage } from './repartidores.page';

/**
 * Entregas (P-6-01 y P-6-02) y repartidores (P-6-04). El shell (`app.routes.ts`) carga cada arreglo con su URL y su
 * guarda de rol OPERADOR, así dos pantallas de la misma carpeta nunca reciben la misma ruta.
 * `dia` va antes que `:id` para que no se tome como un id; es la hoja de ruta (P-6-03), extensión de alcance:
 * se queda con su "Próximamente" (extension-alcance.md).
 */
export const ENTREGAS_ROUTES: Routes = [
  { path: '', component: EntregasPage, data: { pantalla: 'entregas' } },
  {
    path: 'dia',
    component: Proximamente,
    data: { pantalla: 'entregas-dia', titulo: 'Entregas del día', migas: ['Entregas', 'Del día'] },
  },
  {
    path: ':id',
    component: DetalleEntregaPage,
    data: { pantalla: 'entregas', titulo: 'Detalle de la entrega', migas: ['Entregas', 'Detalle'] },
  },
];

export const REPARTIDORES_ROUTES: Routes = [
  { path: '', component: RepartidoresPage, data: { pantalla: 'repartidores' } },
];
