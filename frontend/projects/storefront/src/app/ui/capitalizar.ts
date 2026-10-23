/** Pone la primera letra en mayúscula (solo presentación: no cambia el dato guardado). */
export const capitalizar = (texto: string | null | undefined): string => {
  const t = texto ?? '';
  return t.charAt(0).toLocaleUpperCase('es-PE') + t.slice(1);
};
