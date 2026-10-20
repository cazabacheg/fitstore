import { isDevMode } from '@angular/core';
import { Routes } from '@angular/router';
import { autenticadoGuard } from 'shared';
import { TiendaShell } from './layout/tienda-shell';
import { Pagina404 } from './ui/estados/pagina-404';

/**
 * Rutas de la tienda (R2): carga diferida, "Próximamente" hasta que la spec dueña las implemente.
 * Las que exigen sesión de CLIENTE van con `autenticadoGuard` (→ /ingresar?returnUrl=). `/_ui` solo en desarrollo.
 */
export function crearRutas(dev: boolean): Routes {
  const hijas: Routes = [
    {
      path: '',
      pathMatch: 'full',
      loadChildren: () => import('./features/inicio/inicio.routes').then((m) => m.INICIO_ROUTES),
    },
    {
      path: '',
      loadChildren: () =>
        import('./features/catalogo/catalogo.routes').then((m) => m.CATALOGO_ROUTES),
    },
    {
      path: 'carrito',
      loadChildren: () => import('./features/carrito/carrito.routes').then((m) => m.CARRITO_ROUTES),
    },
    {
      path: 'checkout',
      canActivate: [autenticadoGuard],
      loadChildren: () =>
        import('./features/checkout/checkout.routes').then((m) => m.CHECKOUT_ROUTES),
    },
    {
      path: 'pago/:codigo',
      canActivate: [autenticadoGuard],
      loadChildren: () => import('./features/pago/pago.routes').then((m) => m.PAGO_ROUTES),
    },
    {
      path: 'cuenta/pedidos',
      canActivate: [autenticadoGuard],
      loadChildren: () =>
        import('./features/mis-pedidos/mis-pedidos.routes').then((m) => m.MIS_PEDIDOS_ROUTES),
    },
    {
      path: 'cuenta',
      canActivate: [autenticadoGuard],
      loadChildren: () => import('./features/cuenta/cuenta.routes').then((m) => m.CUENTA_ROUTES),
    },
    {
      path: '',
      loadChildren: () => import('./features/acceso/acceso.routes').then((m) => m.ACCESO_ROUTES),
    },
    {
      path: '',
      loadChildren: () => import('./features/legal/legal.routes').then((m) => m.LEGAL_ROUTES),
    },
    ...(dev
      ? [
          {
            path: '_ui',
            loadChildren: () => import('./features/_ui/_ui.routes').then((m) => m.UI_ROUTES),
          },
        ]
      : []),
    { path: '**', component: Pagina404 },
  ];
  return [{ path: '', component: TiendaShell, children: hijas }];
}

export const routes: Routes = crearRutas(isDevMode());
