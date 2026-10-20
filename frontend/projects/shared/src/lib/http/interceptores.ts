import { HttpErrorResponse, HttpEventType, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, tap, throwError } from 'rxjs';
import { CONFIG_SESION } from '../auth/config-sesion';
import { RefrescoSesion } from '../auth/refresco-sesion.service';
import { SesionStore } from '../auth/sesion.store';
import { comoApiError, esPeticionApi } from './api-error';
import { EstadoServicio } from './estado-servicio';
import { ToastService } from './toast.service';

/** Clave del navegador donde se guarda el UUID del carrito anónimo (R6 de la spec 0b: solo el id, nunca tokens). */
export const CLAVE_CARRITO_ID = 'fs_carrito_id';

/** Estas rutas devuelven 401 por sus propias razones: no disparan refresco ni toast. */
const SIN_REFRESCO = ['/auth/login', '/auth/staff/login', '/auth/refresh', '/auth/logout'];
const RUTA_CARRITO = /^\/api\/v1\/carrito(\/|\?|$)/;

function esRutaSinRefresco(url: string): boolean {
  return SIN_REFRESCO.some((ruta) => url.includes(ruta));
}

/** 1. Agrega `X-Request-Id` (correlación con el gateway y Jaeger). */
export const requestIdInterceptor: HttpInterceptorFn = (req, next) =>
  esPeticionApi(req.url)
    ? next(req.clone({ setHeaders: { 'X-Request-Id': crypto.randomUUID() } }))
    : next(req);

/** 2. Agrega `Authorization: Bearer` cuando hay sesión (no en el refresh, que usa la cookie). */
export const autenticacionInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(SesionStore).token();
  if (!token || !esPeticionApi(req.url) || req.url.includes('/auth/refresh')) return next(req);
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};

function idDeCarrito(): string {
  try {
    const guardado = localStorage.getItem(CLAVE_CARRITO_ID);
    if (guardado) return guardado;
    const nuevo = crypto.randomUUID();
    localStorage.setItem(CLAVE_CARRITO_ID, nuevo);
    return nuevo;
  } catch {
    // Sin almacenamiento disponible: un id para esta sesión del navegador.
    return (idEnMemoria ??= crypto.randomUUID());
  }
}
let idEnMemoria: string | undefined;

/** 3. Solo la tienda: agrega `X-Carrito-Id` en `/carrito/**`, también con sesión (la fusión necesita ambos). */
export const carritoIdInterceptor: HttpInterceptorFn = (req, next) =>
  RUTA_CARRITO.test(req.url)
    ? next(req.clone({ setHeaders: { 'X-Carrito-Id': idDeCarrito() } }))
    : next(req);

/**
 * 4. Ante un 401 hace UN solo refresh compartido y reintenta con el token nuevo (CA-0b-10).
 * Si el refresco falla, cierra la sesión y navega al login recordando la ruta (CA-0b-11).
 */
export const refrescoInterceptor: HttpInterceptorFn = (req, next) => {
  const refresco = inject(RefrescoSesion);
  const store = inject(SesionStore);
  const router = inject(Router);
  const { rutaLogin } = inject(CONFIG_SESION);

  const cerrarYVolverAlLogin = () => {
    store.cerrarSesion();
    if (!router.url.startsWith(rutaLogin)) {
      void router.navigate([rutaLogin], { queryParams: { returnUrl: router.url } });
    }
  };

  return next(req).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401)
        return throwError(() => error);
      if (!esPeticionApi(req.url) || esRutaSinRefresco(req.url)) return throwError(() => error);
      return refresco.refrescar().pipe(
        catchError(() => {
          cerrarYVolverAlLogin();
          return throwError(() => error);
        }),
        switchMap((sesion) => {
          // Se reintenta una sola vez: si vuelve a dar 401 la sesión ya no sirve.
          return next(
            req.clone({ setHeaders: { Authorization: `Bearer ${sesion.accessToken}` } }),
          ).pipe(
            catchError((errorReintento: unknown) => {
              if (errorReintento instanceof HttpErrorResponse && errorReintento.status === 401) {
                cerrarYVolverAlLogin();
              }
              return throwError(() => errorReintento);
            }),
          );
        }),
      );
    }),
  );
};

/** 5. Errores: toast con el mensaje, errores de campo para el formulario y banner con el 503 (CA-0b-13). */
export const erroresInterceptor: HttpInterceptorFn = (req, next) => {
  const toasts = inject(ToastService);
  const estado = inject(EstadoServicio);
  if (!esPeticionApi(req.url)) return next(req);

  return next(req).pipe(
    tap({
      next: (evento) => {
        if (evento.type === HttpEventType.Response) estado.noDisponible.set(false);
      },
      error: (error: unknown) => {
        if (!(error instanceof HttpErrorResponse)) return;
        if (error.status === 503) {
          estado.noDisponible.set(true);
          return;
        }
        if (error.status === 401) return; // lo resuelve el refresco o el formulario de login
        const cuerpo = comoApiError(error);
        if (!cuerpo) return;
        if (error.status === 400 && cuerpo.errores?.length) return; // los muestra el formulario
        toasts.mostrar('error', cuerpo.mensaje);
      },
    }),
  );
};
