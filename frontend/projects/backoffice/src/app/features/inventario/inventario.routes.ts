import { Routes } from '@angular/router';

/** Inventario (P-2-07) y kardex de un SKU (P-2-08): OPERADOR y ADMIN (la guarda está en el shell). */
export const INVENTARIO_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./inventario.page').then((m) => m.InventarioPage),
    data: { pantalla: 'inventario' },
  },
  {
    path: ':varianteId',
    loadComponent: () => import('./kardex.page').then((m) => m.KardexPage),
    data: { pantalla: 'inventario' },
  },
];
