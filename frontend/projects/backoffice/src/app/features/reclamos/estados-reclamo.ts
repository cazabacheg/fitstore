/** Etiqueta y variante de insignia de cada estado y tipo de reclamo (siempre con la palabra, el color solo refuerza). */
export type VarianteReclamo = 'aviso' | 'info' | 'exito' | 'error' | 'neutra';

export const ESTADOS_RECLAMO: Record<string, { etiqueta: string; variante: VarianteReclamo }> = {
  REGISTRADO: { etiqueta: 'Registrado', variante: 'aviso' },
  EN_ATENCION: { etiqueta: 'En atención', variante: 'info' },
  RESPONDIDO: { etiqueta: 'Respondido', variante: 'exito' },
};

export const TIPOS_RECLAMO: Record<string, string> = {
  RECLAMO: 'Reclamo',
  QUEJA: 'Queja',
};

/** Clase de la insignia del sistema de diseño. */
export const claseInsignia = (variante: VarianteReclamo): string =>
  `fs-insignia fs-insignia--${variante}`;
