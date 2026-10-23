import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideFitstoreHttp, provideLocaleEsPe, provideSesion, provideTema } from 'shared';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // Los clientes de API llaman a /api del mismo dominio; el backoffice no usa el carrito.
    provideFitstoreHttp({ carrito: false, avisarErroresDeCampos: true }),
    // El backoffice solo admite cuentas STAFF; la sesión se recupera con el refresh por cookie al arrancar.
    provideSesion({ tipo: 'STAFF', rutaLogin: '/ingresar', rutaSinPermiso: '/403' }),
    provideTema(),
    provideLocaleEsPe(),
  ],
};
