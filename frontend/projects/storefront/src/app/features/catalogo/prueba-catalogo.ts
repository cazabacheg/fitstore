import { HttpTestingController, type TestRequest } from '@angular/common/http/testing';
import type { catalogApi } from 'shared';

/** Datos y ayudas compartidas por las pruebas de la tienda (no forman parte de la aplicación). */
export const sobre = <T>(data: T) => ({ success: true, mensaje: 'Operación exitosa', data });

export const TARJETAS: catalogApi.ProductoTarjeta[] = [
  {
    id: 1,
    slug: 'legging-pro',
    nombre: 'Legging Pro',
    imagenUrl: '/fotos/legging-400.jpg',
    precioDesde: 59.9,
    colores: [
      { id: 1, nombre: 'Negro', hex: '#000000', activo: true },
      { id: 2, nombre: 'Azul', hex: '#1d4ed8', activo: true },
    ],
    etiquetaDescuento: '−10 %',
    agotado: false,
  },
  {
    id: 2,
    slug: 'polo-algodon',
    nombre: 'Polo Algodón',
    imagenUrl: null,
    precioDesde: 35,
    colores: [{ id: 1, nombre: 'Negro', hex: '#000000', activo: true }],
    etiquetaDescuento: null,
    agotado: true,
  },
];

export function pagina<T>(contenido: T[], extra: Partial<{ pagina: number; totalPaginas: number }> = {}) {
  return {
    pagina: extra.pagina ?? 0,
    tamano: 24,
    totalElementos: contenido.length,
    totalPaginas: extra.totalPaginas ?? 1,
    contenido,
  };
}

export const ARBOL = [
  { id: 10, nombre: 'Leggings', slug: 'leggings', hijas: [{ id: 11, nombre: 'Cortos', slug: 'cortos' }] },
];
export const TALLAS = [
  { id: 1, codigo: 'S', orden: 1, activo: true },
  { id: 2, codigo: 'M', orden: 2, activo: true },
];
export const COLORES = [
  { id: 1, nombre: 'Negro', hex: '#000000', activo: true },
  { id: 2, nombre: 'Azul', hex: '#1d4ed8', activo: true },
];

/** Atiende de una vez todas las peticiones pendientes cuya URL (sin consulta) esté en el mapa. */
export function atender(
  ctrl: HttpTestingController,
  respuestas: Record<string, unknown>,
  error?: Record<string, number>,
): TestRequest[] {
  const atendidas: TestRequest[] = [];
  for (const req of ctrl.match(() => true)) {
    const ruta = req.request.url.split('?')[0].replace('/api/v1', '');
    if (error?.[ruta]) req.flush({ success: false, mensaje: 'error' }, { status: error[ruta], statusText: 'x' });
    else if (ruta in respuestas) req.flush(sobre(respuestas[ruta]));
    else req.flush(sobre(null));
    atendidas.push(req);
  }
  return atendidas;
}

/** Producto de prueba del detalle: Blanco sin stock, Negro con stock, Rojo sin stock en ninguna talla. */
export const DETALLE: catalogApi.ProductoDetalle = {
  id: 1,
  slug: 'legging-pro',
  nombre: 'Legging Pro',
  descripcion: 'Tela de compresión.',
  precioDesde: 59.9,
  categorias: [{ id: 11, nombre: 'Cortos', slug: 'cortos' }],
  tallas: [
    { id: 1, codigo: 'S', orden: 1, activo: true },
    { id: 2, codigo: 'M', orden: 2, activo: true },
    { id: 3, codigo: 'L', orden: 3, activo: true },
  ],
  colores: [
    { id: 5, nombre: 'Blanco', hex: '#ffffff', activo: true, conStock: false },
    { id: 1, nombre: 'Negro', hex: '#000000', activo: true, conStock: true },
    { id: 2, nombre: 'Azul', hex: '#1d4ed8', activo: true, conStock: true },
  ],
  variantes: [
    { id: 101, sku: 'LEG-NEG-S', tallaId: 1, colorId: 1, precio: 59.9, disponibilidad: 'AGOTADO' },
    { id: 102, sku: 'LEG-NEG-M', tallaId: 2, colorId: 1, precio: 64.9, disponibilidad: 'ULTIMAS', ultimasUnidades: 4 },
    { id: 103, sku: 'LEG-NEG-L', tallaId: 3, colorId: 1, precio: 59.9, disponibilidad: 'DISPONIBLE' },
    { id: 201, sku: 'LEG-AZU-S', tallaId: 1, colorId: 2, precio: 59.9, disponibilidad: 'DISPONIBLE' },
    { id: 501, sku: 'LEG-BLA-S', tallaId: 1, colorId: 5, precio: 59.9, disponibilidad: 'AGOTADO' },
  ],
  imagenes: [
    { id: 1, colorId: 1, url: '/f/neg-1.jpg', urlMiniatura: '/f/neg-1-400.jpg', orden: 1, principal: true },
    { id: 2, colorId: 1, url: '/f/neg-2.jpg', urlMiniatura: '/f/neg-2-400.jpg', orden: 2, principal: false },
    { id: 3, colorId: 2, url: '/f/azu-1.jpg', urlMiniatura: '/f/azu-1-400.jpg', orden: 1, principal: true },
  ],
  tramos: [
    { unidadesMinimas: 3, porcentaje: 5 },
    { unidadesMinimas: 6, porcentaje: 10 },
    { unidadesMinimas: 12, porcentaje: 15 },
  ],
  tieneGuiaTallas: true,
};
