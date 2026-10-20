import { Routes } from '@angular/router';
import { Proximamente } from '../../ui/estados/proximamente';

/** Marcador "Próximamente" (R2): la spec dueña reemplaza esta carpeta. */
export const CATALOGO_ROUTES: Routes = [
  { path: 'productos', component: Proximamente, data: { seccion: 'El catálogo' } },
  { path: 'categoria/:slug', component: Proximamente, data: { seccion: 'La categoría' } },
  { path: 'producto/:slug', component: Proximamente, data: { seccion: 'El producto' } },
];
