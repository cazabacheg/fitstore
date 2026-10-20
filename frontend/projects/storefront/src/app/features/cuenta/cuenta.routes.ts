import { Routes } from '@angular/router';
import { Proximamente } from '../../ui/estados/proximamente';

/** Marcador "Próximamente" (R2): la spec dueña reemplaza esta carpeta. */
export const CUENTA_ROUTES: Routes = [
  { path: 'perfil', component: Proximamente, data: { seccion: 'Tu perfil' } },
  { path: 'direcciones', component: Proximamente, data: { seccion: 'Tus direcciones' } },
];
