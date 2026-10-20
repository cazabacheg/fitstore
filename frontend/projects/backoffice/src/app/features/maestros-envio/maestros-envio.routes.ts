import { Routes } from '@angular/router';
import { Proximamente } from '../../ui/proximamente/proximamente';

/** Marcador "Próximamente" (CA-0b-15): la spec dueña reemplaza el contenido de esta carpeta. */
export default [
  { path: '', component: Proximamente },
  { path: '**', component: Proximamente },
] satisfies Routes;
