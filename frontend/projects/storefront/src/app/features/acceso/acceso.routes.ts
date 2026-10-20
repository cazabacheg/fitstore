import { Routes } from '@angular/router';
import { Proximamente } from '../../ui/estados/proximamente';

/** Marcador "Próximamente" (R2): la spec dueña reemplaza esta carpeta. */
export const ACCESO_ROUTES: Routes = [
  { path: 'ingresar', component: Proximamente, data: { seccion: 'El ingreso' } },
  { path: 'registro', component: Proximamente, data: { seccion: 'El registro' } },
  {
    path: 'recuperar',
    component: Proximamente,
    data: { seccion: 'La recuperación de contraseña' },
  },
  {
    path: 'restablecer',
    component: Proximamente,
    data: { seccion: 'El restablecimiento de contraseña' },
  },
];
