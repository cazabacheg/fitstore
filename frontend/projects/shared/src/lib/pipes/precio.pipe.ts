import { Pipe, PipeTransform } from '@angular/core';

const FORMATO = new Intl.NumberFormat('es-PE', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/** Redondea a 2 decimales con half-up decimal (no binario): 1.005 → 1.01. */
function redondear(monto: number): number {
  const signo = monto < 0 ? -1 : 1;
  const absoluto = Math.abs(monto);
  const texto = String(absoluto);
  // Los números muy chicos o grandes salen con exponente (1e-7): ahí basta con el redondeo normal.
  if (texto.includes('e')) return (signo * Math.round(absoluto * 100)) / 100;
  return signo * Number(`${Math.round(Number(`${texto}e2`))}e-2`);
}

/** Monto en soles: `332.456 | precio` → `S/ 332.46` (es-PE, half-up, siempre dos decimales). */
@Pipe({ name: 'precio' })
export class PrecioPipe implements PipeTransform {
  transform(monto: number | string | null | undefined): string {
    if (monto === null || monto === undefined || monto === '') return '';
    const valor = typeof monto === 'string' ? Number(monto) : monto;
    if (!Number.isFinite(valor)) return '';
    const redondeado = redondear(valor);
    const texto = FORMATO.format(Math.abs(redondeado));
    return `${redondeado < 0 ? '-' : ''}S/ ${texto}`;
  }
}
