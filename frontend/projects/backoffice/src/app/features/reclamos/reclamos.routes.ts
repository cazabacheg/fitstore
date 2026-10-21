import { Routes } from '@angular/router';
import { ReclamoDetallePage } from './reclamo-detalle.page';
import { ReclamosPage } from './reclamos.page';

/**
 * Reclamos (P-3-06): bandeja y detalle. El shell (`app.routes.ts`) carga este arreglo con la guarda de rol OPERADOR;
 * tomar y responder los restringe la propia pantalla (y el servidor) al ADMIN.
 */
export const RECLAMOS_ROUTES: Routes = [
  { path: '', component: ReclamosPage, data: { pantalla: 'reclamos' } },
  {
    path: ':id',
    component: ReclamoDetallePage,
    data: { pantalla: 'reclamos', titulo: 'Detalle del reclamo', migas: ['Reclamos', 'Detalle'] },
  },
];
