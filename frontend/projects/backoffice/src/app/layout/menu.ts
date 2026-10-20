import type { RolStaff } from 'shared';

/** Hoja del menú: una ruta a la que se llega (un ítem sin subítems o un subítem). */
export interface HojaMenu {
  /** Clave con la que una funcionalidad registra su contador (`CONTADOR_MENU`). */
  clave: string;
  etiqueta: string;
  ruta: string;
  /** Rol mínimo para ver el subítem. */
  rol: RolStaff;
}

export interface ItemMenu {
  clave: string;
  etiqueta: string;
  /** Nombre del icono de Material Symbols Rounded. */
  icono: string;
  /** Rol mínimo del ítem. */
  rol: RolStaff;
  /** Ruta del ítem cuando no tiene subítems. */
  ruta?: string;
  subitems?: HojaMenu[];
}

const NIVEL: Record<RolStaff, number> = { OPERADOR: 1, ADMIN: 2 };

function sub(clave: string, etiqueta: string, ruta: string, rol: RolStaff = 'ADMIN'): HojaMenu {
  return { clave, etiqueta, ruta, rol };
}

/** Menú del backoffice (R3): ítems, subítems y rol mínimo de cada uno. */
export const MENU: readonly ItemMenu[] = [
  {
    clave: 'dashboard',
    etiqueta: 'Dashboard',
    icono: 'space_dashboard',
    rol: 'OPERADOR',
    ruta: '/',
  },
  {
    clave: 'pedidos',
    etiqueta: 'Pedidos',
    icono: 'receipt_long',
    rol: 'OPERADOR',
    ruta: '/pedidos',
  },
  {
    clave: 'entregas',
    etiqueta: 'Entregas',
    icono: 'local_shipping',
    rol: 'OPERADOR',
    subitems: [
      sub('entregas-lista', 'Lista', '/entregas', 'OPERADOR'),
      sub('entregas-dia', 'Del día', '/entregas/dia', 'OPERADOR'),
    ],
  },
  {
    clave: 'repartidores',
    etiqueta: 'Repartidores',
    icono: 'two_wheeler',
    rol: 'OPERADOR',
    ruta: '/repartidores',
  },
  {
    clave: 'catalogo',
    etiqueta: 'Catálogo',
    icono: 'checkroom',
    rol: 'OPERADOR',
    subitems: [
      sub('productos', 'Productos', '/catalogo/productos', 'OPERADOR'),
      sub('categorias', 'Categorías', '/catalogo/categorias'),
      sub('maestros', 'Tallas y colores', '/catalogo/maestros'),
      sub('descuentos', 'Descuentos', '/catalogo/descuentos'),
      sub('carga-masiva', 'Carga masiva', '/catalogo/productos/carga-masiva'),
    ],
  },
  {
    clave: 'inventario',
    etiqueta: 'Inventario',
    icono: 'inventory_2',
    rol: 'OPERADOR',
    ruta: '/inventario',
  },
  {
    clave: 'pagos',
    etiqueta: 'Pagos',
    icono: 'payments',
    rol: 'ADMIN',
    subitems: [
      sub('pagos-listado', 'Pagos', '/pagos'),
      sub('reembolsos', 'Reembolsos', '/reembolsos'),
    ],
  },
  {
    clave: 'tarifas-agencias',
    etiqueta: 'Tarifas y agencias',
    icono: 'map',
    rol: 'ADMIN',
    subitems: [sub('tarifas', 'Tarifas', '/tarifas'), sub('agencias', 'Agencias', '/agencias')],
  },
  { clave: 'clientes', etiqueta: 'Clientes', icono: 'group', rol: 'OPERADOR', ruta: '/clientes' },
  { clave: 'staff', etiqueta: 'Staff', icono: 'badge', rol: 'ADMIN', ruta: '/staff' },
  {
    clave: 'notificaciones',
    etiqueta: 'Notificaciones',
    icono: 'mail',
    rol: 'ADMIN',
    subitems: [
      sub('notificaciones-registro', 'Registro', '/notificaciones'),
      sub('plantillas', 'Plantillas', '/notificaciones/plantillas'),
    ],
  },
  {
    clave: 'reclamos',
    etiqueta: 'Reclamos',
    icono: 'menu_book',
    rol: 'OPERADOR',
    ruta: '/reclamos',
  },
  {
    clave: 'configuracion',
    etiqueta: 'Configuración de tienda',
    icono: 'storefront',
    rol: 'ADMIN',
    subitems: [
      sub('configuracion-tienda', 'Tienda', '/configuracion'),
      sub('politicas', 'Políticas', '/configuracion/politicas'),
    ],
  },
];

/** Ítems y subítems que el rol puede ver (CA-0b-17). Un rol desconocido no ve nada. */
export function menuParaRol(rol: string | null | undefined): ItemMenu[] {
  const nivel = NIVEL[rol as RolStaff] ?? 0;
  return MENU.filter((item) => NIVEL[item.rol] <= nivel).map((item) =>
    item.subitems
      ? { ...item, subitems: item.subitems.filter((s) => NIVEL[s.rol] <= nivel) }
      : item,
  );
}

/** Todas las rutas del menú, sin los grupos. */
export function hojasDelMenu(items: readonly ItemMenu[]): HojaMenu[] {
  return items.flatMap((item) =>
    item.subitems
      ? item.subitems
      : [{ clave: item.clave, etiqueta: item.etiqueta, ruta: item.ruta ?? '/', rol: item.rol }],
  );
}
