import { Routes } from '@angular/router';
import { Proximamente } from '../../ui/proximamente/proximamente';

/**
 * Marcador "Próximamente" (CA-0b-15): la spec dueña reemplaza el contenido de esta carpeta.
 * El shell (`app.routes.ts`) carga este arreglo y aplica la guarda de rol.
 */
export const CONFIGURACION_ROUTES: Routes = [
  { path: '', component: Proximamente, data: { pantalla: 'configuracion' } },
  {
    path: 'politicas',
    component: Proximamente,
    data: {
      pantalla: 'politicas',
      titulo: 'Políticas',
      migas: ['Configuración de tienda', 'Políticas'],
    },
  },
  { path: '**', component: Proximamente, data: { pantalla: 'configuracion' } },
];
