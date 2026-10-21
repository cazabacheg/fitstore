import { Routes } from '@angular/router';
import { DetallePedidoPage } from './detalle-pedido.page';
import { MisPedidosPage } from './mis-pedidos.page';

/**
 * Mis pedidos (P-4-03) y el detalle de un pedido (P-4-04). La lista va dentro del marco de "Mi cuenta", con su menú
 * lateral, como en el diseño; el detalle ocupa la página completa.
 */
export const MIS_PEDIDOS_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('../cuenta/cuenta-layout').then((m) => m.CuentaLayout),
    children: [{ path: '', component: MisPedidosPage }],
  },
  { path: ':codigo', component: DetallePedidoPage },
];
