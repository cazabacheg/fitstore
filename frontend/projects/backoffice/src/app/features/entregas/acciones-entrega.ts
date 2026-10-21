import type { deliveryApi } from 'shared';

/** Texto del botón de cada acción de una entrega (E1): el siguiente paso de la máquina de estados. */
export function etiquetaAccion(
  accion: deliveryApi.AccionEntrega | string,
  modalidad: deliveryApi.ModalidadEntrega,
): string {
  switch (accion) {
    case 'ASIGNAR':
      return 'Asignar';
    case 'REPROGRAMAR':
      return 'Reprogramar';
    case 'EN_RUTA':
      return 'En ruta';
    case 'ENTREGADA':
      return modalidad === 'AGENCIA' ? 'Recogido' : 'Entregada';
    case 'FALLIDA':
      return 'No se pudo entregar';
    case 'ENVIADA_AGENCIA':
      return 'Registrar envío';
    case 'DISPONIBLE_AGENCIA':
      return 'Disponible';
    default:
      return accion;
  }
}

/** Hoy en Lima como `AAAA-MM-DD` (E7: la fecha programada es hoy o posterior). */
export function hoyEnLima(): string {
  return new Date().toLocaleDateString('en-CA', { timeZone: 'America/Lima' });
}

/** Estados de cada pestaña, en el orden de los chips de contador. */
export const ESTADOS_POR_MODALIDAD: Record<string, string[]> = {
  DELIVERY_LOCAL: ['PENDIENTE_ASIGNACION', 'ASIGNADA', 'EN_RUTA', 'FALLIDA', 'ENTREGADA'],
  AGENCIA: ['PENDIENTE_ENVIO', 'ENVIADO_AGENCIA', 'DISPONIBLE_EN_AGENCIA', 'ENTREGADA'],
};
