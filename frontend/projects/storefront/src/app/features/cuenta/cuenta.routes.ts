import { Routes } from '@angular/router';

/** Mi cuenta (P-1-05 y P-1-06): `CuentaLayout` con el menú lateral; los pedidos (`/cuenta/pedidos`) son de la spec 4. */
export const CUENTA_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./cuenta-layout').then((m) => m.CuentaLayout),
    children: [
      {
        path: 'perfil',
        loadComponent: () => import('./perfil/perfil.page').then((m) => m.PerfilPage),
      },
      {
        path: 'direcciones',
        loadComponent: () =>
          import('./direcciones/direcciones.page').then((m) => m.DireccionesPage),
      },
      { path: '', pathMatch: 'full', redirectTo: 'perfil' },
    ],
  },
];
