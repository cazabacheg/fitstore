import { InjectionToken } from '@angular/core';

/** Configuración de la sesión de cada app (storefront: CLIENTE; backoffice: STAFF). */
export interface ConfigSesion {
  /** Tipo de cuenta que admite la app. Sin valor, admite cualquiera. */
  tipo?: 'CLIENTE' | 'STAFF';
  /** Ruta del login de la app. */
  rutaLogin: string;
  /** Ruta de la página 403 a la que va quien no tiene el rol. */
  rutaSinPermiso: string;
}

export const CONFIG_SESION = new InjectionToken<ConfigSesion>('CONFIG_SESION', {
  providedIn: 'root',
  factory: () => ({ rutaLogin: '/ingresar', rutaSinPermiso: '/403' }),
});
