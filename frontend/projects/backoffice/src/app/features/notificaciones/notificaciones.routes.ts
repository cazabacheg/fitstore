import { Routes } from '@angular/router';
import { Proximamente } from '../../ui/proximamente/proximamente';

/**
 * Marcador "Próximamente" (CA-0b-15): la spec dueña reemplaza el contenido de esta carpeta.
 * El shell (`app.routes.ts`) carga este arreglo y aplica la guarda de rol.
 */
export const NOTIFICACIONES_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { pantalla: 'notificaciones' } },
  {
    path: 'plantillas',
    component: Proximamente,
    data: { pantalla: 'plantillas', titulo: 'Plantillas', migas: ['Notificaciones', 'Plantillas'] },
  },
  { path: '**', component: Proximamente, data: { pantalla: 'notificaciones' } },
];
