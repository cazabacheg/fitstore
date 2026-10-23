import { HttpInterceptorFn, provideHttpClient, withInterceptors } from '@angular/common/http';
import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';
import { BASE_PATH as BASE_AUTH } from '../api/auth-service/variables';
import { BASE_PATH as BASE_CATALOG } from '../api/catalog-service/variables';
import { BASE_PATH as BASE_DELIVERY } from '../api/delivery-service/variables';
import { BASE_PATH as BASE_NOTIFICATION } from '../api/notification-service/variables';
import { BASE_PATH as BASE_ORDER } from '../api/order-service/variables';
import { BASE_PATH as BASE_PAYMENT } from '../api/payment-service/variables';
import { BASE_PATH as BASE_SETTINGS } from '../api/settings-service/variables';
import {
  autenticacionInterceptor,
  carritoIdInterceptor,
  crearErroresInterceptor,
  refrescoInterceptor,
  requestIdInterceptor,
} from './interceptores';

export interface OpcionesHttp {
  /** `true` solo en la tienda: agrega `X-Carrito-Id` en `/carrito/**`. */
  carrito: boolean;
  /** `true` en el backoffice: un 400 con errores por campo sale también en un toast si el formulario no lo pidió con `SIN_TOAST` (CA-0b-34). */
  avisarErroresDeCampos?: boolean;
}

/**
 * `HttpClient` con los interceptores en el orden de R7 (requestId, autenticación, carritoId, refresco, errores)
 * y los clientes generados apuntando al mismo origen: las llamadas van a `/api/...` (proxy en desarrollo, Caddy en nube).
 */
export function provideFitstoreHttp(opciones: OpcionesHttp): EnvironmentProviders {
  const interceptores: HttpInterceptorFn[] = [
    requestIdInterceptor,
    autenticacionInterceptor,
    ...(opciones.carrito ? [carritoIdInterceptor] : []),
    refrescoInterceptor,
    crearErroresInterceptor(opciones.avisarErroresDeCampos ?? false),
  ];
  return makeEnvironmentProviders([
    provideHttpClient(withInterceptors(interceptores)),
    ...[
      BASE_AUTH,
      BASE_CATALOG,
      BASE_DELIVERY,
      BASE_NOTIFICATION,
      BASE_ORDER,
      BASE_PAYMENT,
      BASE_SETTINGS,
    ].map((token) => ({ provide: token, useValue: '' })),
  ]);
}
