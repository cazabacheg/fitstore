import {
  EnvironmentProviders,
  Provider,
  makeEnvironmentProviders,
  provideAppInitializer,
  inject,
} from '@angular/core';
import { catchError, firstValueFrom, of } from 'rxjs';
import { CONFIG_SESION, ConfigSesion } from './config-sesion';
import { RefrescoSesion } from './refresco-sesion.service';

/** Configura la sesión de la app (tipo de cuenta, ruta del login y de la página 403). */
export function provideSesionConfig(config: Partial<ConfigSesion>): Provider {
  return {
    provide: CONFIG_SESION,
    useValue: { rutaLogin: '/ingresar', rutaSinPermiso: '/403', ...config } satisfies ConfigSesion,
  };
}

/**
 * Configura la sesión y, al arrancar, intenta recuperarla con el refresh por cookie antes de mostrar rutas
 * protegidas (CA-0b-12). Si no hay cookie válida (401) la app arranca igual, sin sesión y sin avisos.
 */
export function provideSesion(config: Partial<ConfigSesion>): EnvironmentProviders {
  return makeEnvironmentProviders([
    provideSesionConfig(config),
    provideAppInitializer(() => {
      const refresco = inject(RefrescoSesion);
      return firstValueFrom(refresco.refrescar().pipe(catchError(() => of(null))));
    }),
  ]);
}
