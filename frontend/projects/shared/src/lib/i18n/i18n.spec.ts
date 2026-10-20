import { formatCurrency, formatDate, formatNumber } from '@angular/common';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideLocaleEsPe } from './proveedores';

describe('Configuración es-PE', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideLocaleEsPe()] }));

  it('CA-0b-27 el idioma de la app es es-PE', () => {
    expect(TestBed.inject(LOCALE_ID)).toBe('es-PE');
  });

  it('CA-0b-27 los números, las monedas y las fechas de Angular salen en es-PE', () => {
    const locale = TestBed.inject(LOCALE_ID);
    expect(formatNumber(1234.5, locale, '1.2-2')).toBe('1,234.50');
    expect(formatCurrency(59.9, locale, 'S/', 'PEN', '1.2-2')).toBe('S/59.90');
    expect(formatDate('2026-10-22T12:00:00', "EEEE d 'de' MMMM", locale)).toBe(
      'jueves 22 de octubre',
    );
  });
});
