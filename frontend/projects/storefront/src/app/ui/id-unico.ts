let contador = 0;

/** Identificador único para enlazar etiquetas, ayudas y errores con su control. */
export function idUnico(prefijo: string): string {
  return `${prefijo}-${++contador}`;
}
