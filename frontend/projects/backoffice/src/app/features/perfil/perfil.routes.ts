import { Routes } from '@angular/router';
import { PerfilStaffPage } from './perfil-staff.page';

/**
 * Mi perfil del backoffice (P-1-10). El shell (`app.routes.ts`) carga este arreglo y aplica la guarda de rol;
 * `pantalla` identifica la pantalla de la carpeta (R2).
 */
export const PERFIL_ROUTES: Routes = [
  { path: '', component: PerfilStaffPage, data: { pantalla: 'perfil' } },
  { path: '**', redirectTo: '' },
];
