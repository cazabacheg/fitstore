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
    const ruta = req.request.url.split('?')[0];
    if (error?.[ruta]) req.flush({ success: false, mensaje: 'error' }, { status: error[ruta], statusText: 'x' });
    else if (ruta in respuestas) req.flush(sobre(respuestas[ruta]));
    else req.flush(sobre(null));
    atendidas.push(req);
  }
  return atendidas;
}
