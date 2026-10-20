import { FechaPipe } from './fecha.pipe';
import { PrecioPipe } from './precio.pipe';

describe('PrecioPipe', () => {
  const pipe = new PrecioPipe();

  it('CA-0b-27 formatea 332.456 como "S/ 332.46" (es-PE, half-up)', () => {
    expect(pipe.transform(332.456)).toBe('S/ 332.46');
  });

  it.each([
    [1.005, 'S/ 1.01'], // en coma flotante 1.005 es 1.00499999…: half-up decimal debe dar 1.01
    [2.675, 'S/ 2.68'],
    [59.9, 'S/ 59.90'],
    [0, 'S/ 0.00'],
    [100, 'S/ 100.00'],
    [1234.5, 'S/ 1,234.50'],
    [1234567.891, 'S/ 1,234,567.89'],
    [0.004, 'S/ 0.00'],
    [0.005, 'S/ 0.01'],
  ])('CA-0b-27 formatea %s como %s', (monto, esperado) => {
    expect(pipe.transform(monto)).toBe(esperado);
  });

  it('CA-0b-27 acepta montos como texto numérico', () => {
    expect(pipe.transform('59.9')).toBe('S/ 59.90');
  });

  it('CA-0b-27 un monto negativo lleva el signo delante (reembolsos, descuentos)', () => {
    expect(pipe.transform(-12.345)).toBe('-S/ 12.35');
  });

  it.each([null, undefined, '', 'abc', Number.NaN])('CA-0b-27 devuelve vacío para %s', (valor) => {
    expect(pipe.transform(valor as never)).toBe('');
  });
});

describe('FechaPipe', () => {
  const pipe = new FechaPipe();

  it('HU-0b-06 la fecha corta es día/mes/año en hora de Lima', () => {
    expect(pipe.transform('2026-10-22T15:30:00Z')).toBe('22/10/2026');
    // 03:00 UTC del 23 son las 22:00 del 22 en Lima (UTC-5)
    expect(pipe.transform('2026-10-23T03:00:00Z')).toBe('22/10/2026');
  });

  it('HU-0b-06 una fecha sin hora no se corre de día por la zona horaria', () => {
    expect(pipe.transform('2026-10-22')).toBe('22/10/2026');
    expect(pipe.transform('2026-10-22', 'larga')).toBe('jueves 22 de octubre');
  });

  it('HU-0b-06 la fecha larga se escribe "jueves 22 de octubre"', () => {
    expect(pipe.transform('2026-10-22T15:30:00Z', 'larga')).toBe('jueves 22 de octubre');
  });

  it('HU-0b-06 fecha y hora usa 24 horas en hora de Lima', () => {
    expect(pipe.transform('2026-10-22T15:30:00Z', 'fechaHora')).toBe('22/10/2026 10:30');
  });

  it('HU-0b-06 acepta un Date y devuelve vacío para valores vacíos o inválidos', () => {
    expect(pipe.transform(new Date('2026-10-22T15:30:00Z'))).toBe('22/10/2026');
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
    expect(pipe.transform('no es una fecha')).toBe('');
  });
});
