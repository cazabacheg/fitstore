import { Routes } from '@angular/router';
import { DetallePedidoAdminPage } from './detalle-pedido-admin.page';
import { PedidosPage } from './pedidos.page';

/**
 * Pedidos del backoffice: la lista (P-4-05) y el detalle de un pedido (P-4-06).
 * El shell (`app.routes.ts`) carga este arreglo y aplica la guarda de rol.
 */
export const PEDIDOS_ROUTES: Routes = [
  { path: '', component: PedidosPage, data: { pantalla: 'pedidos' } },
  {
    path: ':id',
    component: DetallePedidoAdminPage,
    data: { pantalla: 'pedidos', titulo: 'Pedido', migas: ['Pedidos', 'Detalle'] },
  },
];
