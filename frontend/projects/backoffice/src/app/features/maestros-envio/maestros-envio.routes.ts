import { Routes } from '@angular/router';
import { AgenciasPage } from './agencias.page';
import { TarifasPage } from './tarifas.page';

/**
 * Tarifas (P-6-05) y agencias (P-6-06). El shell (`app.routes.ts`) carga cada arreglo con su URL y su guarda de rol
 * ADMIN, así dos pantallas de la misma carpeta nunca reciben la misma ruta.
 */
export const TARIFAS_ROUTES: Routes = [
  { path: '', component: TarifasPage, data: { pantalla: 'tarifas' } },
];

export const AGENCIAS_ROUTES: Routes = [
  { path: '', component: AgenciasPage, data: { pantalla: 'agencias' } },
];
