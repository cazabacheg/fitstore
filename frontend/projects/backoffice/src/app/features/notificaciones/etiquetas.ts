import type { notificationApi } from 'shared';

export type CodigoPlantilla = notificationApi.CodigoPlantilla;
export type EstadoEnvio = notificationApi.EstadoEnvio;

/** Nombre legible de cada una de las 18 plantillas (micro 4.1). */
export const NOMBRES_PLANTILLA: Record<CodigoPlantilla, string> = {
  BIENVENIDA: 'Bienvenida',
  RECUPERAR_PASSWORD: 'Recupera tu contraseña',
  INVITACION_STAFF: 'Invitación al backoffice',
  PASSWORD_CAMBIADO: 'Contraseña cambiada',
  PEDIDO_CONFIRMADO: 'Pedido confirmado',
  PEDIDO_CANCELADO: 'Pedido cancelado',
  REEMBOLSO_REALIZADO: 'Reembolso realizado',
  EN_CAMINO: 'Pedido en camino',
  ENTREGA_FALLIDA: 'Entrega fallida',
  LISTO_PARA_RECOGER: 'Listo para recoger',
  ENTREGADO: 'Pedido entregado',
  RECLAMO_COPIA: 'Copia del reclamo',
  RECLAMO_RESPUESTA: 'Respuesta al reclamo',
  PEDIDO_DEVUELTO: 'Pedido devuelto a la tienda',
  ALERTA_STOCK_BAJO: 'Alerta: stock bajo',
  ALERTA_SIN_STOCK: 'Alerta: pedido sin stock',
  ALERTA_REEMBOLSO_FALLIDO: 'Alerta: reembolso fallido',
  ALERTA_RECLAMO_NUEVO: 'Alerta: reclamo nuevo',
};

export const CODIGOS_PLANTILLA = Object.keys(NOMBRES_PLANTILLA) as CodigoPlantilla[];

export function nombrePlantilla(codigo: string): string {
  return NOMBRES_PLANTILLA[codigo as CodigoPlantilla] ?? codigo;
}

type Variante = 'exito' | 'aviso' | 'info' | 'error' | 'neutra';

/** Estado del envío: la palabra manda y el color solo la refuerza (ficha `Insignia`). */
export const ESTADOS_ENVIO: Record<EstadoEnvio, { etiqueta: string; variante: Variante }> = {
  PENDIENTE: { etiqueta: 'Pendiente', variante: 'aviso' },
  ENVIANDO: { etiqueta: 'Enviando', variante: 'info' },
  ENVIADO: { etiqueta: 'Enviado', variante: 'exito' },
  FALLIDO: { etiqueta: 'Fallido', variante: 'error' },
};

export function estadoEnvio(codigo: string): { etiqueta: string; variante: Variante } {
  return ESTADOS_ENVIO[codigo as EstadoEnvio] ?? { etiqueta: codigo, variante: 'neutra' };
}

/** Los envíos que el admin puede reenviar: los FALLIDO y los ENVIADO (CA-7-21). */
export function sePuedeReenviar(estado: string): boolean {
  return estado === 'FALLIDO' || estado === 'ENVIADO';
}

/** Dónde se ve la referencia: un pedido (`FS-…`) o un reclamo (`RC-…`); la prueba del editor no enlaza. */
export function enlaceReferencia(referencia: string): { ruta: string[]; params: Record<string, string> } | null {
  if (referencia.startsWith('FS-')) return { ruta: ['/pedidos'], params: { q: referencia } };
  if (referencia.startsWith('RC-')) return { ruta: ['/reclamos'], params: { q: referencia } };
  return null;
}
