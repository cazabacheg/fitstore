import { paymentApi } from 'shared';

const PROVEEDORES: Record<string, string> = {
  MERCADO_PAGO: 'Mercado Pago',
  CULQI: 'Culqi',
  SIMULADO: 'Pago de prueba',
};
const MEDIOS: Record<string, string> = {
  TARJETA: 'Tarjeta',
  YAPE: 'Yape',
  SALDO_MP: 'Saldo de Mercado Pago',
};
const MOTIVOS: Record<paymentApi.MotivoReembolso, string> = {
  CANCELACION: 'Cancelación',
  PAGO_NO_APLICABLE: 'Pago no aplicable',
  POSTVENTA: 'Postventa',
};

export function nombreProveedor(proveedor: string | null | undefined): string {
  return proveedor ? (PROVEEDORES[proveedor] ?? proveedor) : '—';
}

export function nombreMedio(medio: string | null | undefined): string {
  return medio ? (MEDIOS[medio] ?? medio) : '—';
}

/** "Mercado Pago · Tarjeta", o un guion mientras no hay un cobro aprobado. */
export function proveedorYMedio(
  proveedor: string | null | undefined,
  medio: string | null | undefined,
): string {
  return proveedor ? `${nombreProveedor(proveedor)} · ${nombreMedio(medio)}` : '—';
}

export function nombreMotivo(motivo: paymentApi.MotivoReembolso): string {
  return MOTIVOS[motivo] ?? motivo;
}
