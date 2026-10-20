import { Routes } from '@angular/router';
import { Proximamente } from '../../ui/estados/proximamente';

/** Marcador "Próximamente" (R2): la spec dueña reemplaza esta carpeta. */
export const MIS_PEDIDOS_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { seccion: 'Tus pedidos' } },
  { path: ':codigo', component: Proximamente, data: { seccion: 'Tu pedido' } },
];
