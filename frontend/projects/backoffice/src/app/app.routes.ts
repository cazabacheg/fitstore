import { inject, isDevMode } from '@angular/core';
import { CanMatchFn, Route, Routes } from '@angular/router';
import { CONFIG_SESION, SesionStore, autenticadoGuard, rolGuard } from 'shared';
import { BackofficeShell } from './layout/backoffice-shell';
import { Pagina403 } from './ui/estados/pagina-403';
import { Pagina404 } from './ui/estados/pagina-404';

/** Datos de la ruta: título de la página y migas (después de "Inicio"). */
function datos(titulo: string, ...migas: string[]): Route['data'] {
  return { titulo, migas: migas.length > 0 ? migas : [titulo] };
}

const OPERADOR = [rolGuard('OPERADOR')];
const ADMIN = [rolGuard('ADMIN')];

/** Cada ruta de R2 carga de forma diferida la carpeta de su funcionalidad. */
const carga = {
  dashboard: () => import('./features/dashboard/dashboard.routes'),
  perfil: () => import('./features/perfil/perfil.routes'),
  usuarios: () => import('./features/usuarios/usuarios.routes'),
  catalogo: () => import('./features/catalogo/catalogo.routes'),
  inventario: () => import('./features/inventario/inventario.routes'),
  configuracion: () => import('./features/configuracion/configuracion.routes'),
  reclamos: () => import('./features/reclamos/reclamos.routes'),
  pedidos: () => import('./features/pedidos/pedidos.routes'),
  pagos: () => import('./features/pagos/pagos.routes'),
  entregas: () => import('./features/entregas/entregas.routes'),
  maestrosEnvio: () => import('./features/maestros-envio/maestros-envio.routes'),
  notificaciones: () => import('./features/notificaciones/notificaciones.routes'),
};

/** Rutas protegidas del backoffice (tabla R2). Las más específicas van primero. */
const rutasDelShell: Routes = [
  { path: '', pathMatch: 'full', loadChildren: carga.dashboard, data: datos('Dashboard') },
  { path: 'perfil', loadChildren: carga.perfil, data: datos('Mi perfil') },
  { path: 'staff', canActivate: ADMIN, loadChildren: carga.usuarios, data: datos('Staff') },
  {
    path: 'clientes',
    canActivate: OPERADOR,
    loadChildren: carga.usuarios,
    data: datos('Clientes'),
  },
  {
    path: 'catalogo/productos/carga-masiva',
    canActivate: ADMIN,
    loadChildren: carga.catalogo,
    data: datos('Carga masiva', 'Catálogo', 'Productos', 'Carga masiva'),
  },
  {
    path: 'catalogo/categorias',
    canActivate: ADMIN,
    loadChildren: carga.catalogo,
    data: datos('Categorías', 'Catálogo', 'Categorías'),
  },
  {
    path: 'catalogo/maestros',
    canActivate: ADMIN,
    loadChildren: carga.catalogo,
    data: datos('Tallas y colores', 'Catálogo', 'Tallas y colores'),
  },
  {
    path: 'catalogo/descuentos',
    canActivate: ADMIN,
    loadChildren: carga.catalogo,
    data: datos('Descuentos', 'Catálogo', 'Descuentos'),
  },
  {
    path: 'catalogo',
    canActivate: OPERADOR,
    loadChildren: carga.catalogo,
    data: datos('Productos', 'Catálogo', 'Productos'),
  },
  {
    path: 'inventario',
    canActivate: OPERADOR,
    loadChildren: carga.inventario,
    data: datos('Inventario'),
  },
  { path: 'pedidos', canActivate: OPERADOR, loadChildren: carga.pedidos, data: datos('Pedidos') },
  {
    path: 'entregas/dia',
    canActivate: OPERADOR,
    loadChildren: carga.entregas,
    data: datos('Entregas del día', 'Entregas', 'Del día'),
  },
  {
    path: 'entregas',
    canActivate: OPERADOR,
    loadChildren: carga.entregas,
    data: datos('Entregas'),
  },
  {
    path: 'repartidores',
    canActivate: OPERADOR,
    loadChildren: carga.entregas,
    data: datos('Repartidores'),
  },
  {
    path: 'tarifas',
    canActivate: ADMIN,
    loadChildren: carga.maestrosEnvio,
    data: datos('Tarifas', 'Tarifas y agencias', 'Tarifas'),
  },
  {
    path: 'agencias',
    canActivate: ADMIN,
    loadChildren: carga.maestrosEnvio,
    data: datos('Agencias', 'Tarifas y agencias', 'Agencias'),
  },
  {
    path: 'pagos',
    canActivate: ADMIN,
    loadChildren: carga.pagos,
    data: datos('Pagos', 'Pagos', 'Pagos'),
  },
  {
    path: 'reembolsos',
    canActivate: ADMIN,
    loadChildren: carga.pagos,
    data: datos('Reembolsos', 'Pagos', 'Reembolsos'),
  },
  {
    path: 'notificaciones',
    canActivate: ADMIN,
    loadChildren: carga.notificaciones,
    data: datos('Notificaciones'),
  },
  {
    path: 'configuracion',
    canActivate: ADMIN,
    loadChildren: carga.configuracion,
    data: datos('Configuración de tienda', 'Configuración de tienda'),
  },
  {
    path: 'reclamos',
    canActivate: OPERADOR,
    loadChildren: carga.reclamos,
    data: datos('Reclamos'),
  },
  { path: '403', component: Pagina403 },
];

/** Rutas públicas de `features/acceso`. El `canMatch` evita que `/` caiga en esta carpeta (ruta vacía sin hijas). */
const RUTAS_PUBLICAS = ['ingresar', 'recuperar', 'restablecer'];
const esRutaPublica: CanMatchFn = (_ruta, segmentos) =>
  RUTAS_PUBLICAS.includes(segmentos[0]?.path ?? '');

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
export function crearRutas(_desarrollo: boolean): Routes {
  return [
    {
      path: '',
      canMatch: [esRutaPublica],
      loadChildren: () => import('./features/acceso/acceso.routes'),
    },
    {
      path: '',
      component: BackofficeShell,
      canActivate: [autenticadoGuard],
      children: rutasDelShell,
    },
    {
      path: '**',
      component: BackofficeShell,
      canMatch: [conSesionDeLaApp],
      children: [{ path: '**', component: Pagina404 }],
    },
    { path: '**', component: Pagina404 },
  ];
}

export const routes: Routes = crearRutas(isDevMode());
