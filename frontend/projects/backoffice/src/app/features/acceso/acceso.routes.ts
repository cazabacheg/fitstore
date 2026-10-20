import { Routes } from '@angular/router';
import { Proximamente } from '../../ui/proximamente/proximamente';

/** Marcador "Próximamente" (CA-0b-15): la spec 1 reemplaza el contenido de esta carpeta. */
export default [
  { path: 'ingresar', component: Proximamente, data: { titulo: 'Ingresar' } },
  { path: 'recuperar', component: Proximamente, data: { titulo: 'Recuperar contraseña' } },
  { path: 'restablecer', component: Proximamente, data: { titulo: 'Restablecer contraseña' } },
] satisfies Routes;
