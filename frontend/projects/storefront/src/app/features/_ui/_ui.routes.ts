import { Routes } from '@angular/router';
import { Proximamente } from '../../ui/estados/proximamente';

/** Catálogo de componentes de `ui/` (se completa en el paso de `ui/`). */
export const UI_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { seccion: 'El catálogo' } },
];
