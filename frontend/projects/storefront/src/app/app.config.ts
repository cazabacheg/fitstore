import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideFitstoreHttp, provideLocaleEsPe, provideSesion, provideTema } from 'shared';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // Los clientes de API llaman a /api del mismo dominio; la tienda agrega X-Carrito-Id en /carrito.
    provideFitstoreHttp({ carrito: true }),
    // La tienda solo admite cuentas CLIENTE; la sesión se recupera con el refresh por cookie al arrancar.
    provideSesion({ tipo: 'CLIENTE', rutaLogin: '/ingresar', rutaSinPermiso: '/403' }),
    provideTema(),
    provideLocaleEsPe(),
  ],
};
