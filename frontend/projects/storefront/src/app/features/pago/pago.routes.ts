import { Routes } from '@angular/router';
import { Proximamente } from '../../ui/estados/proximamente';

/** Marcador "Próximamente" (R2): la spec dueña reemplaza esta carpeta. */
export const PAGO_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { seccion: 'El pago' } },
];
