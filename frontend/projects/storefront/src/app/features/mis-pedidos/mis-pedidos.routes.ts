import { Routes } from '@angular/router';
import { DetallePedidoPage } from './detalle-pedido.page';
import { MisPedidosPage } from './mis-pedidos.page';

/** Mis pedidos (P-4-03) y el detalle de un pedido (P-4-04), dentro de "Mi cuenta". */
export const MIS_PEDIDOS_ROUTES: Routes = [
  { path: '', component: MisPedidosPage },
  { path: ':codigo', component: DetallePedidoPage },
];
