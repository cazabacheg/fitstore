import { Routes } from '@angular/router';
import { Proximamente } from '../../ui/estados/proximamente';

/** Marcador "Próximamente" (R2): la spec dueña reemplaza esta carpeta. */
export const LEGAL_ROUTES: Routes = [
  {
    path: 'libro-de-reclamaciones',
    component: Proximamente,
    data: { seccion: 'El Libro de Reclamaciones' },
  },
  { path: 'politicas/:tipo', component: Proximamente, data: { seccion: 'La política' } },
];
