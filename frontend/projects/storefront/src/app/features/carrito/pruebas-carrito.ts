import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { orderApi, provideFitstoreHttp, provideSesionConfig } from 'shared';

/** Ayudas de las pruebas de carrito, checkout y mis pedidos (no se usan en el código de producción). */
export const ok = <T>(data: T, mensaje = 'Operación exitosa') => ({ success: true, mensaje, data });

export const errorApi = (codigo: string, mensaje: string, detalles?: Record<string, unknown>) => ({
  success: false,
  mensaje,
  codigo,
  detalles,
});

export const PROVEEDORES = [
  provideRouter([]),
  provideFitstoreHttp({ carrito: false }),
  provideHttpClientTesting(),
  provideSesionConfig({ tipo: 'CLIENTE' }),
];

export const http = () => TestBed.inject(HttpTestingController);

export function linea(
  varianteId: number,
  producto: string,
  cantidad: number,
  extra: Partial<orderApi.LineaCarrito> = {},
): orderApi.LineaCarrito {
  return {
    varianteId,
    producto,
    talla: 'M',
    color: 'Negro',
    imagenUrl: null,
    precioUnitario: 59.9,
    cantidad,
    descuentoPorcentaje: 0,
    descuentoMonto: 0,
    total: 59.9 * cantidad,
    estado: 'OK',
    ...extra,
  };
}

export function carrito(
  lineas: orderApi.LineaCarrito[],
  extra: Partial<orderApi.Carrito> = {},
): orderApi.Carrito {
  const subtotal = lineas.reduce((s, l) => s + (l.precioUnitario ?? 0) * l.cantidad, 0);
  const descuento = lineas.reduce((s, l) => s + (l.descuentoMonto ?? 0), 0);
  return {
    lineas,
    avisosTramo: [],
    subtotal,
    descuentoTotal: descuento,
    total: subtotal - descuento,
    cantidadTotal: lineas.reduce((s, l) => s + l.cantidad, 0),
    preciosPorConfirmar: false,
    puedeIrAlCheckout: lineas.length > 0,
    ...extra,
  };
}
