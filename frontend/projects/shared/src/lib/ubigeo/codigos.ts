export type NivelUbigeo = 'departamento' | 'provincia' | 'distrito';

/** Nivel de un código INEI: 2 dígitos departamento, 4 provincia, 6 distrito. `null` si el formato no es válido. */
export function nivelDe(codigo: string): NivelUbigeo | null {
  if (!/^\d+$/.test(codigo)) return null;
  switch (codigo.length) {
    case 2:
      return 'departamento';
    case 4:
      return 'provincia';
    case 6:
      return 'distrito';
    default:
      return null;
  }
}

/** `150122` → `15`. */
export function departamentoDe(codigo: string): string {
  return codigo.slice(0, 2);
}

/** `150122` → `1501`. */
export function provinciaDe(codigo: string): string {
  return codigo.slice(0, 4);
}
