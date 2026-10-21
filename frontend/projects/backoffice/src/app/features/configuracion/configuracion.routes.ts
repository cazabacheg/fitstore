import { Routes } from '@angular/router';
import { ConfiguracionPage } from './configuracion.page';
import { PoliticasPage } from './politicas.page';
import { sinCambiosGuard } from './sin-cambios.guard';

/**
 * Configuración de tienda (P-3-04) y Políticas (P-3-05). El shell (`app.routes.ts`) carga este arreglo y aplica la
 * guarda de rol ADMIN; `data.pantalla` identifica cada pantalla (CA-0b-15).
 */
export const CONFIGURACION_ROUTES: Routes = [
  {
    path: '',
    component: ConfiguracionPage,
    canDeactivate: [sinCambiosGuard],
    data: { pantalla: 'configuracion' },
  },
  {
    path: 'politicas',
    component: PoliticasPage,
    data: {
      pantalla: 'politicas',
      titulo: 'Políticas',
      migas: ['Configuración de tienda', 'Políticas'],
    },
  },
];
