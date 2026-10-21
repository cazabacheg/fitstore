import { Routes } from '@angular/router';
import { ClienteDetallePage } from './cliente-detalle.page';
import { ClientesPage } from './clientes.page';
import { StaffPage } from './staff.page';

/**
 * Staff (P-1-08, ADMIN) y Clientes (P-1-09, OPERADOR y ADMIN). Cada pantalla tiene su propio arreglo: el shell
 * (`app.routes.ts`) los carga con su URL y su guarda de rol (R2). `pantalla` identifica cada una.
 */
export const STAFF_ROUTES: Routes = [
  { path: '', component: StaffPage, data: { pantalla: 'staff' } },
  { path: '**', redirectTo: '' },
];

export const CLIENTES_ROUTES: Routes = [
  { path: '', component: ClientesPage, data: { pantalla: 'clientes' } },
  { path: ':id', component: ClienteDetallePage, data: { pantalla: 'clientes' } },
];
