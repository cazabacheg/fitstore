import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, type Routes } from '@angular/router';
import { orderApi, provideFitstoreHttp, provideSesionConfig } from 'shared';

/** Ayudas de las pruebas de pedidos del backoffice (no se usan en el código de producción). */
export const ok = <T>(data: T, mensaje = 'Operación exitosa') => ({ success: true, mensaje, data });

export const errorApi = (codigo: string, mensaje: string) => ({ success: false, mensaje, codigo });

export const proveedores = (rutas: Routes) => [
  provideRouter(rutas),
  provideFitstoreHttp({ carrito: false }),
  provideHttpClientTesting(),
  provideSesionConfig({ tipo: 'STAFF', rutaLogin: '/ingresar', rutaSinPermiso: '/403' }),
];

export const http = () => TestBed.inject(HttpTestingController);

export function fila(codigo: string, extra: Partial<orderApi.PedidoAdminResumen> = {}): orderApi.PedidoAdminResumen {
  return {
    id: `id-${codigo}`,
    codigo,
    creadoEn: '2026-10-20T15:00:00Z',
    clienteNombre: 'Ana Quispe',
    modalidad: 'DELIVERY_LOCAL',
    estado: 'PAGADO',
    total: 129.8,
    ...extra,
  };
}

export function admin(extra: Partial<orderApi.PedidoAdmin> = {}): orderApi.PedidoAdmin {
  return {
    id: '9d3c1f2e-0000-4000-8000-000000000001',
    clienteId: '35a11d4f-054a-44a5-8ac4-cf0ed4020f8c',
    codigo: 'FS-000124',
    estado: 'PAGADO',
    modalidad: 'DELIVERY_LOCAL',
    creadoEn: '2026-10-20T15:00:00Z',
    items: [
      {
        varianteId: 10002,
        sku: 'LEG01-NEG-M',
        producto: 'Legging Pro',
        talla: 'M',
        color: 'Negro',
        imagenUrl: null,
        precioUnitario: 59.9,
        cantidad: 2,
        descuentoPorcentaje: 0,
        descuentoMonto: 0,
        total: 119.8,
      },
    ],
    subtotal: 119.8,
    descuentoTotal: 0,
    costoEnvio: 10,
    total: 129.8,
    montoReembolsado: 0,
    estadoReembolso: 'NINGUNO',
    destinatario: { nombres: 'Ana', apellidos: 'Quispe', dni: '12345678', telefono: '987654321', email: 'ana@correo.pe' },
    direccion: {
      ubigeo: '150122',
      departamento: 'Lima',
      provincia: 'Lima',
      distrito: 'Miraflores',
      direccion: 'Av. Larco 345',
    },
    envio: { modalidad: 'DELIVERY_LOCAL', costo: 10 },
    pago: { proveedor: 'SIMULADO', medio: 'TARJETA', pagadoEn: '2026-10-20T15:05:00Z' },
    boletaUrl: '/api/v1/pagos/FS-000124/boleta',
    historial: [
      { fecha: '2026-10-20T15:00:00Z', tipo: 'CAMBIO_ESTADO', estadoNuevo: 'PENDIENTE_PAGO', actorTipo: 'CLIENTE' },
      { fecha: '2026-10-20T15:05:00Z', tipo: 'CAMBIO_ESTADO', estadoNuevo: 'PAGADO', actorTipo: 'SISTEMA' },
    ],
    puedeCancelar: true,
    puedePagar: false,
    transicionesPermitidas: [{ estado: 'EN_PREPARACION', etiqueta: 'Empezar a preparar' }],
    puedeCancelarStaff: true,
    reembolsoPostventa: { permitido: false, maximo: 0 },
    reembolsos: [],
    version: 3,
    ...extra,
  };
}
