import {
  EnvironmentProviders,
  inject,
  makeEnvironmentProviders,
  provideAppInitializer,
} from '@angular/core';
import { TemaService } from './tema.service';

/** Carga la marca de la tienda al arrancar (R4, `provideAppInitializer`): no bloquea más de unos segundos. */
export function provideTema(): EnvironmentProviders {
  return makeEnvironmentProviders([provideAppInitializer(() => inject(TemaService).iniciar())]);
}
