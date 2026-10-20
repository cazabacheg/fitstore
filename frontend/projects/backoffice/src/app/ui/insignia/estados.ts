export type TipoEstado = 'pedido' | 'pago' | 'entrega' | 'reembolso';
export type VarianteInsignia = 'exito' | 'aviso' | 'info' | 'error' | 'marca' | 'acento' | 'neutra';
export interface DatosInsignia {
  etiqueta: string;
  variante: VarianteInsignia;
}

const i = (etiqueta: string, variante: VarianteInsignia): DatosInsignia => ({ etiqueta, variante });

/**
 * Etiqueta y variante de cada estado de los contratos (order, payment y delivery), según la ficha `Insignia`:
 * aviso = pendiente, éxito = completado, info = en curso, neutra = cerrado sin más, error = problema, marca = reembolsos.
 */
export const ESTADOS: Record<TipoEstado, Record<string, DatosInsignia>> = {
  pedido: {
    PENDIENTE_PAGO: i('Pendiente de pago', 'aviso'),
    PAGADO: i('Pagado', 'exito'),
    EN_PREPARACION: i('En preparación', 'info'),
    LISTO_DESPACHO: i('Listo para despacho', 'info'),
    LISTO_RECOJO: i('Listo para recojo', 'info'),
    ENVIADO: i('Enviado', 'info'),
    ENTREGADO: i('Entregado', 'exito'),
    CANCELADO: i('Cancelado', 'neutra'),
    EXPIRADO: i('Expirado', 'neutra'),
    DEVUELTO: i('Devuelto', 'error'),
  },
  pago: {
    PENDIENTE: i('Pendiente de pago', 'aviso'),
    PAGADA: i('Pagado', 'exito'),
    ANULADA: i('Anulado', 'neutra'),
    REEMBOLSADA_PARCIAL: i('Reembolso parcial', 'marca'),
    REEMBOLSADA: i('Reembolsado', 'marca'),
    PROCESANDO: i('Procesando', 'info'),
    APROBADA: i('Aprobado', 'exito'),
    RECHAZADA: i('Pago rechazado', 'error'),
    ERROR: i('Fallido', 'error'),
  },
  entrega: {
    PENDIENTE_ASIGNACION: i('Por asignar', 'aviso'),
    ASIGNADA: i('Asignada', 'info'),
    EN_RUTA: i('En ruta', 'info'),
    FALLIDA: i('Fallida', 'error'),
    PENDIENTE_ENVIO: i('Pendiente de envío', 'aviso'),
    ENVIADO_AGENCIA: i('Enviado a la agencia', 'info'),
    DISPONIBLE_EN_AGENCIA: i('Disponible en la agencia', 'info'),
    ENTREGADA: i('Entregada', 'exito'),
    DEVUELTA: i('Devuelta', 'error'),
  },
  reembolso: {
    PENDIENTE: i('Reembolso pendiente', 'aviso'),
    COMPLETADO: i('Reembolsado', 'marca'),
    FALLIDO: i('Reembolso fallido', 'error'),
    RESUELTO_MANUAL: i('Resuelto a mano', 'neutra'),
  },
};

/** Etiqueta y variante de un estado; uno desconocido se muestra neutro con su código en palabras, sin romper la pantalla. */
export function insigniaDe(tipo: TipoEstado, codigo: string): DatosInsignia {
  const conocido = ESTADOS[tipo][codigo];
  if (conocido) return conocido;
  const palabras = codigo.toLowerCase().replace(/_/g, ' ');
  return { etiqueta: palabras.charAt(0).toUpperCase() + palabras.slice(1), variante: 'neutra' };
}
