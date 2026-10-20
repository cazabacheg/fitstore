import { registerLocaleData } from '@angular/common';
import localeEsPe from '@angular/common/locales/es-PE';
import { LOCALE_ID, Provider } from '@angular/core';

/** Idioma de las dos apps: español de Perú (fechas, números). Los montos en soles los da el pipe `precio`. */
export function provideLocaleEsPe(): Provider[] {
  registerLocaleData(localeEsPe, 'es-PE');
  return [{ provide: LOCALE_ID, useValue: 'es-PE' }];
}
