import { inject, isDevMode } from '@angular/core';
import { CanMatchFn, Route, Routes } from '@angular/router';
import { CONFIG_SESION, SesionStore, autenticadoGuard, rolGuard } from 'shared';
import { Pagina403 } from './ui/estados/pagina-403';
import { Pagina404 } from './ui/estados/pagina-404';

/** Datos de la ruta: título de la página y migas (después de "Inicio"). */
function datos(titulo: string, ...migas: string[]): Route['data'] {
  return { titulo, migas: migas.length > 0 ? migas : [titulo] };
}

const OPERADOR = [rolGuard('OPERADOR')];
const ADMIN = [rolGuard('ADMIN')];

/** El shell (y con él Angular Material) se carga de forma diferida: el bundle inicial queda por debajo del presupuesto. */
const cargaShell = () => import('./layout/backoffice-shell').then((m) => m.BackofficeShell);

/**
 * Cada pantalla de R2 carga de forma diferida su arreglo de rutas, exportado por la carpeta de su funcionalidad.
 * Dos pantallas de la misma carpeta (Staff y Clientes, Pagos y Reembolsos…) tienen arreglos distintos, para que
 * cada una reciba su propia ruta.
 */
const usuarios = () => import('./features/usuarios/usuarios.routes');
const catalogo = () => import('./features/catalogo/catalogo.routes');
const pagos = () => import('./features/pagos/pagos.routes');
const entregas = () => import('./features/entregas/entregas.routes');
const maestrosEnvio = () => import('./features/maestros-envio/maestros-envio.routes');

/** Rutas protegidas del backoffice (tabla R2). Las más específicas van primero. */
const rutasDelShell: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadChildren: () =>
      import('./features/dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
    data: datos('Dashboard'),
  },
  {
    path: 'perfil',
    loadChildren: () => import('./features/perfil/perfil.routes').then((m) => m.PERFIL_ROUTES),
    data: datos('Mi perfil'),
  },
  {
    path: 'staff',
    canActivate: ADMIN,
    loadChildren: () => usuarios().then((m) => m.STAFF_ROUTES),
    data: datos('Staff'),
  },
  {
    path: 'clientes',
    canActivate: OPERADOR,
    loadChildren: () => usuarios().then((m) => m.CLIENTES_ROUTES),
    data: datos('Clientes'),
  },
  {
    path: 'catalogo/productos/carga-masiva',
    canActivate: ADMIN,
    loadChildren: () => catalogo().then((m) => m.CARGA_MASIVA_ROUTES),
    data: datos('Carga masiva', 'Catálogo', 'Productos', 'Carga masiva'),
  },
  {
    path: 'catalogo/productos',
    canActivate: OPERADOR,
    loadChildren: () => catalogo().then((m) => m.PRODUCTOS_ROUTES),
    data: datos('Productos', 'Catálogo', 'Productos'),
  },
  {
    path: 'catalogo/categorias',
    canActivate: ADMIN,
    loadChildren: () => catalogo().then((m) => m.CATEGORIAS_ROUTES),
    data: datos('Categorías', 'Catálogo', 'Categorías'),
  },
  {
    path: 'catalogo/maestros',
    canActivate: ADMIN,
    loadChildren: () => catalogo().then((m) => m.MAESTROS_ROUTES),
    data: datos('Tallas y colores', 'Catálogo', 'Tallas y colores'),
  },
  {
    path: 'catalogo/descuentos',
    canActivate: ADMIN,
    loadChildren: () => catalogo().then((m) => m.DESCUENTOS_ROUTES),
    data: datos('Descuentos', 'Catálogo', 'Descuentos'),
  },
  { path: 'catalogo', pathMatch: 'full', redirectTo: 'catalogo/productos' },
  {
    path: 'inventario',
    canActivate: OPERADOR,
    loadChildren: () =>
      import('./features/inventario/inventario.routes').then((m) => m.INVENTARIO_ROUTES),
    data: datos('Inventario'),
  },
  {
    path: 'pedidos',
    canActivate: OPERADOR,
    loadChildren: () => import('./features/pedidos/pedidos.routes').then((m) => m.PEDIDOS_ROUTES),
    data: datos('Pedidos'),
  },
  {
    path: 'entregas',
    canActivate: OPERADOR,
    loadChildren: () => entregas().then((m) => m.ENTREGAS_ROUTES),
    data: datos('Entregas'),
  },
  {
    path: 'repartidores',
    canActivate: OPERADOR,
    loadChildren: () => entregas().then((m) => m.REPARTIDORES_ROUTES),
    data: datos('Repartidores'),
  },
  {
    path: 'tarifas',
    canActivate: ADMIN,
    loadChildren: () => maestrosEnvio().then((m) => m.TARIFAS_ROUTES),
    data: datos('Tarifas', 'Tarifas y agencias', 'Tarifas'),
  },
  {
    path: 'agencias',
    canActivate: ADMIN,
    loadChildren: () => maestrosEnvio().then((m) => m.AGENCIAS_ROUTES),
    data: datos('Agencias', 'Tarifas y agencias', 'Agencias'),
  },
  {
    path: 'pagos',
    canActivate: ADMIN,
    loadChildren: () => pagos().then((m) => m.PAGOS_ROUTES),
    data: datos('Pagos', 'Pagos', 'Pagos'),
  },
  {
    path: 'reembolsos',
    canActivate: ADMIN,
    loadChildren: () => pagos().then((m) => m.REEMBOLSOS_ROUTES),
    data: datos('Reembolsos', 'Pagos', 'Reembolsos'),
  },
  {
    path: 'notificaciones',
    canActivate: ADMIN,
    loadChildren: () =>
      import('./features/notificaciones/notificaciones.routes').then(
        (m) => m.NOTIFICACIONES_ROUTES,
      ),
    data: datos('Notificaciones'),
  },
  {
    path: 'configuracion',
    canActivate: ADMIN,
    loadChildren: () =>
      import('./features/configuracion/configuracion.routes').then((m) => m.CONFIGURACION_ROUTES),
    data: datos('Configuración de tienda', 'Configuración de tienda'),
  },
  {
    path: 'reclamos',
    canActivate: OPERADOR,
    loadChildren: () =>
      import('./features/reclamos/reclamos.routes').then((m) => m.RECLAMOS_ROUTES),
    data: datos('Reclamos'),
  },
  { path: '403', component: Pagina403 },
];

/** Rutas públicas de `features/acceso`. El `canMatch` evita que `/` caiga en esta carpeta (ruta vacía sin hijas). */
const RUTAS_PUBLICAS = ['ingresar', 'recuperar', 'restablecer'];
const esRutaPublica: CanMatchFn = (_ruta, segmentos) =>
  RUTAS_PUBLICAS.includes(segmentos[0]?.path ?? '');

/**
 * Catálogo de componentes: solo en desarrollo, fuera del shell y sin sesión (no depende de la API).
 * Se declara con `ngDevMode` para que el build de producción elimine la ruta y su chunk (CA-0b-25).
 */
declare const ngDevMode: unknown;
const catalogoUi: Route | null =
  typeof ngDevMode === 'undefined' || ngDevMode
    ? {
        path: '_ui',
        loadComponent: () => import('./ui/catalogo-ui/catalogo-ui').then((m) => m.CatalogoUi),
      }
    : null;

/** Hay una sesión del tipo que admite la app (STAFF): decide si la 404 se muestra dentro del shell. */
const conSesionDeLaApp: CanMatchFn = () => {
  const sesion = inject(SesionStore);
  const { tipo } = inject(CONFIG_SESION);
  return sesion.autenticado() && (tipo === undefined || sesion.tipo() === tipo);
};

/**
 * Rutas de la app. `/_ui` (catálogo de componentes) solo existe en desarrollo (CA-0b-25).
 * Los dos comodines: con sesión, la 404 va dentro del shell; sin sesión, fuera de él (CA-0b-19).
 */
export function crearRutas(desarrollo: boolean): Routes {
  return [
    ...(desarrollo && catalogoUi ? [catalogoUi] : []),
    {
      path: '',
      canMatch: [esRutaPublica],
      loadChildren: () => import('./features/acceso/acceso.routes').then((m) => m.ACCESO_ROUTES),
    },
    {
      path: '',
      loadComponent: cargaShell,
      canActivate: [autenticadoGuard],
      children: rutasDelShell,
    },
    {
      path: '**',
      loadComponent: cargaShell,
      canMatch: [conSesionDeLaApp],
      children: [{ path: '**', component: Pagina404 }],
    },
    { path: '**', component: Pagina404 },
  ];
}

export const routes: Routes = crearRutas(isDevMode());
