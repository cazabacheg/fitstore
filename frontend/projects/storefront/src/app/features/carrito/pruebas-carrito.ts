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

/** Legging Pro (producto 10) con tramo del 10 % desde 3 unidades y del 15 % desde 6. */
export function conTramo(unidades: number, extra: Partial<orderApi.LineaCarrito> = {}) {
  const pct = unidades >= 3 ? 10 : 0;
  const descuento = Math.round(59.9 * unidades * pct) / 100;
  return carrito(
    [
      linea(101, 'Legging Pro', unidades, {
        productoId: 10,
        descuentoPorcentaje: pct,
        descuentoMonto: descuento,
        total: Math.round((59.9 * unidades - descuento) * 100) / 100,
        ...extra,
      }),
    ],
    {
      avisosTramo: [
        {
          productoId: 10,
          producto: 'Legging Pro',
          unidades,
          tramoAlcanzado: pct ? { unidadesMinimas: 3, porcentaje: 10 } : null,
          siguienteTramo: pct
            ? { unidadesMinimas: 6, porcentaje: 15, faltan: Math.max(0, 6 - unidades) }
            : { unidadesMinimas: 3, porcentaje: 10, faltan: 3 - unidades },
        },
      ],
    },
  );
}
