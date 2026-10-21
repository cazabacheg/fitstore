import { Routes } from '@angular/router';
import { Proximamente } from '../../ui/proximamente/proximamente';
import { DetallePagoPage } from './detalle-pago.page';
import { PagosPage } from './pagos.page';

/**
 * Pagos del backoffice (solo ADMIN): la lista (P-5-03) y el detalle de un pago (P-5-04).
 * El shell (`app.routes.ts`) carga cada arreglo con su URL y su guarda de rol.
 */
export const PAGOS_ROUTES: Routes = [
  { path: '', component: PagosPage },
  { path: ':id', component: DetallePagoPage },
];

/** Reembolsos (P-5-05) es extensión de alcance: se queda con su "Próximamente" (docs/implementacion/extension-alcance.md). */
export const REEMBOLSOS_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { pantalla: 'reembolsos' } },
  { path: '**', component: Proximamente, data: { pantalla: 'reembolsos' } },
];
