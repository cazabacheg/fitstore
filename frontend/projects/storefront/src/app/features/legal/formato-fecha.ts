const ZONA = 'America/Lima';
const SOLO_FECHA = /^\d{4}-\d{2}-\d{2}$/;
const LARGA = new Intl.DateTimeFormat('es-PE', {
  timeZone: ZONA,
  day: 'numeric',
  month: 'long',
  year: 'numeric',
});

/** "19 de octubre de 2026" en hora de Lima; una fecha sin hora (`2026-11-09`) se toma tal cual. */
export function fechaLarga(valor: string | null | undefined): string {
  if (!valor) return '';
  const fecha = SOLO_FECHA.test(valor) ? new Date(`${valor}T12:00:00-05:00`) : new Date(valor);
  return Number.isNaN(fecha.getTime()) ? '' : LARGA.format(fecha);
}
