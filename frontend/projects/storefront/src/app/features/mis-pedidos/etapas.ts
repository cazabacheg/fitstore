import { orderApi } from 'shared';

/** Una etapa de la línea de tiempo: su nombre, la descripción de la tabla O9 y cuándo pasó (si ya pasó). */
export interface Etapa {
  clave: string;
  nombre: string;
  descripcion: string;
  /** ISO del momento en que el pedido entró a la etapa; ausente en las etapas futuras. */
  fecha?: string;
  /** `hecha` ya pasó, `actual` es donde está el pedido, `futura` falta (se muestra atenuada). */
  situacion: 'hecha' | 'actual' | 'futura';
  /** Etapa que corta la línea: cancelado, expirado o devuelto. */
  final?: boolean;
}

/** Aviso dentro de la línea de tiempo: intento de entrega fallido o pago rechazado. */
export interface AvisoHistorial {
  fecha: string;
  texto: string;
}

/** Textos de la tabla de etapas del seguimiento (SPEC-4, O9). Fijos del frontend, no editables en settings. */
export const DESCRIPCIONES = {
  recibido: 'Apartamos tus prendas mientras completas el pago.',
  pagado: 'Recibimos tu pago y emitimos tu boleta.',
  preparacion: 'Estamos alistando y empacando tus prendas.',
  enviadoLocal: 'Tu pedido va en camino con nuestro repartidor.',
  enviadoAgencia: (agencia: string, codigo: string) =>
    `Lo dejamos en la agencia ${agencia}; síguelo con el código ${codigo}.`,
  listoRecojo: 'Ya puedes recogerlo en la tienda; lleva tu DNI.',
  entregado: '¡Lo recibiste! Gracias por tu compra.',
  entregadoRecojo: 'Recogiste tu pedido en la tienda.',
  cancelado: (motivo: string) => `Cancelado: ${motivo}. Si pagaste, te devolvemos el dinero.`,
  expirado: 'No recibimos el pago a tiempo y liberamos las prendas.',
  devuelto: (motivo: string) =>
    `No pudimos entregarte el pedido y regresó a la tienda: ${motivo}. Te contactaremos para devolverte tu dinero.`,
  devueltoRecojo: 'No recogiste tu pedido a tiempo y regresó al almacén.',
} as const;

type Estado = orderApi.EstadoPedido;

/** Primera vez que el pedido llegó a cada estado, según el historial (los avisos no cuentan). */
function fechasPorEstado(historial: orderApi.HistorialEntrada[]): Map<Estado, string> {
  const fechas = new Map<Estado, string>();
  for (const h of historial) {
    if (h.tipo === 'CAMBIO_ESTADO' && h.estadoNuevo && !fechas.has(h.estadoNuevo)) {
      fechas.set(h.estadoNuevo, h.fecha);
    }
  }
  return fechas;
}

/**
 * Arma la línea de tiempo (CA-4-34): pedido recibido → pago confirmado → en preparación → enviado → entregado
 * (en recojo: … → listo para recoger → recogido). Un pedido cancelado, expirado o devuelto termina en esa etapa con el
 * motivo; las etapas que faltan se muestran atenuadas con su descripción.
 */
export function construirEtapas(pedido: orderApi.PedidoDetalle): Etapa[] {
  const fechas = fechasPorEstado(pedido.historial);
  const recojo = pedido.modalidad === 'RECOJO';
  const agencia = pedido.envio.agenciaNombre;
  const codigo = pedido.envio.codigoSeguimiento;
  const motivoDe = (estado: Estado) => {
    const motivo =
      [...pedido.historial].reverse().find((h) => h.estadoNuevo === estado && h.tipo === 'CAMBIO_ESTADO')
        ?.comentario ?? '';
    return /^Cancelado por el cliente/.test(motivo) ? 'lo cancelaste tú' : motivo;
  };

  const base: Omit<Etapa, 'situacion'>[] = [
    { clave: 'PENDIENTE_PAGO', nombre: 'Pedido recibido', descripcion: DESCRIPCIONES.recibido },
    { clave: 'PAGADO', nombre: 'Pago confirmado', descripcion: DESCRIPCIONES.pagado },
    { clave: 'EN_PREPARACION', nombre: 'En preparación', descripcion: DESCRIPCIONES.preparacion },
    recojo
      ? { clave: 'LISTO_RECOJO', nombre: 'Listo para recoger', descripcion: DESCRIPCIONES.listoRecojo }
      : {
          clave: 'ENVIADO',
          nombre: 'Enviado',
          descripcion:
            agencia && codigo
              ? DESCRIPCIONES.enviadoAgencia(agencia, codigo)
              : DESCRIPCIONES.enviadoLocal,
        },
    {
      clave: 'ENTREGADO',
      nombre: 'Entregado',
      descripcion: recojo ? DESCRIPCIONES.entregadoRecojo : DESCRIPCIONES.entregado,
    },
  ];
  // LISTO_DESPACHO sigue siendo "En preparación" para el cliente
  const fechaDe = (clave: string) =>
    clave === 'EN_PREPARACION'
      ? (fechas.get('EN_PREPARACION') ?? fechas.get('LISTO_DESPACHO'))
      : fechas.get(clave as Estado);

  const final = finalDe(pedido, motivoDe, recojo);
  if (final) {
    const alcanzadas = base.filter((e) => fechaDe(e.clave) !== undefined);
    const etapas: Etapa[] = alcanzadas.map((e) => ({ ...e, fecha: fechaDe(e.clave), situacion: 'hecha' }));
    etapas.push({ ...final, fecha: fechas.get(final.clave as Estado), situacion: 'actual', final: true });
    return etapas;
  }

  const ultimo = base.reduce((acc, e, i) => (fechaDe(e.clave) !== undefined ? i : acc), 0);
  return base.map((e, i) => ({
    ...e,
    fecha: i <= ultimo ? fechaDe(e.clave) : undefined,
    situacion: i < ultimo ? 'hecha' : i === ultimo ? 'actual' : 'futura',
  }));
}

function finalDe(
  pedido: orderApi.PedidoDetalle,
  motivoDe: (estado: Estado) => string,
  recojo: boolean,
): Omit<Etapa, 'situacion'> | null {
  switch (pedido.estado) {
    case 'CANCELADO':
      return {
        clave: 'CANCELADO',
        nombre: 'Cancelado',
        descripcion: DESCRIPCIONES.cancelado(motivoDe('CANCELADO') || 'sin motivo indicado'),
      };
    case 'EXPIRADO':
      return { clave: 'EXPIRADO', nombre: 'Expirado', descripcion: DESCRIPCIONES.expirado };
    case 'DEVUELTO':
      return {
        clave: 'DEVUELTO',
        nombre: 'Devuelto a la tienda',
        descripcion: recojo
          ? DESCRIPCIONES.devueltoRecojo
          : DESCRIPCIONES.devuelto(motivoDe('DEVUELTO') || 'sin motivo indicado'),
      };
    default:
      return null;
  }
}

/** Intentos de entrega fallidos y pagos rechazados, para mostrarlos como avisos de la línea de tiempo. */
export function avisosDelHistorial(historial: orderApi.HistorialEntrada[]): AvisoHistorial[] {
  return historial
    .filter((h) => h.tipo === 'INTENTO_FALLIDO' || h.tipo === 'PAGO_RECHAZADO')
    .map((h) => ({ fecha: h.fecha, texto: h.comentario ?? 'Aviso del pedido' }));
}
