import { Pipe, PipeTransform } from '@angular/core';

export type FormatoFecha = 'corta' | 'larga' | 'fechaHora';

const ZONA = 'America/Lima';
const CORTA = new Intl.DateTimeFormat('es-PE', {
  timeZone: ZONA,
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
});
const LARGA = new Intl.DateTimeFormat('es-PE', {
  timeZone: ZONA,
  weekday: 'long',
  day: 'numeric',
  month: 'long',
});
const HORA = new Intl.DateTimeFormat('es-PE', {
  timeZone: ZONA,
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});
const SOLO_FECHA = /^\d{4}-\d{2}-\d{2}$/;

function parte(
  formato: Intl.DateTimeFormat,
  fecha: Date,
  tipo: Intl.DateTimeFormatPartTypes,
): string {
  return formato.formatToParts(fecha).find((p) => p.type === tipo)?.value ?? '';
}

/**
 * Fechas en es-PE y hora de Lima. `corta`: 22/10/2026; `larga`: jueves 22 de octubre; `fechaHora`: 22/10/2026 10:30.
 * Una fecha sin hora (`2026-10-22`) se toma tal cual, sin correrla de día por la zona horaria.
 */
@Pipe({ name: 'fecha' })
export class FechaPipe implements PipeTransform {
  transform(valor: string | Date | null | undefined, formato: FormatoFecha = 'corta'): string {
    if (valor === null || valor === undefined || valor === '') return '';
    const fecha =
      typeof valor === 'string' && SOLO_FECHA.test(valor)
        ? new Date(`${valor}T12:00:00-05:00`)
        : new Date(valor);
    if (Number.isNaN(fecha.getTime())) return '';

    if (formato === 'larga') {
      return `${parte(LARGA, fecha, 'weekday')} ${parte(LARGA, fecha, 'day')} de ${parte(LARGA, fecha, 'month')}`;
    }
    const dia = `${parte(CORTA, fecha, 'day')}/${parte(CORTA, fecha, 'month')}/${parte(CORTA, fecha, 'year')}`;
    if (formato === 'fechaHora')
      return `${dia} ${parte(HORA, fecha, 'hour')}:${parte(HORA, fecha, 'minute')}`;
    return dia;
  }
}
