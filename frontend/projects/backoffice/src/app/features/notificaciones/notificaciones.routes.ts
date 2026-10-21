import { Routes } from '@angular/router';
import { Proximamente } from '../../ui/proximamente/proximamente';
import { RegistroEnviosPage } from './registro-envios.page';

/**
 * Notificaciones (solo ADMIN): el registro de envíos (P-7-01). El editor de plantillas (P-7-02 y P-7-03) es extensión
 * de alcance y se queda con su "Próximamente" (docs/implementacion/extension-alcance.md).
 * El shell (`app.routes.ts`) carga este arreglo y aplica la guarda de rol.
 */
export const NOTIFICACIONES_ROUTES: Routes = [
  { path: '', component: RegistroEnviosPage, data: { pantalla: 'notificaciones' } },
  {
    path: 'plantillas',
    component: Proximamente,
    data: { pantalla: 'plantillas', titulo: 'Plantillas', migas: ['Notificaciones', 'Plantillas'] },
  },
  { path: '**', component: Proximamente, data: { pantalla: 'notificaciones' } },
];
