/*
 * API pública de shared.
 * Cada servicio generado se expone en su propio espacio de nombres porque los 7 clientes repiten los modelos
 * comunes (ApiResponse, ApiError…): `authApi.AutenticacionService`, `settingsApi.UbigeoService`, etc.
 * Los tipos comunes propios (ApiResponse, ApiError, Pagina) están en `models`.
 * Los servicios, en cambio, se importan desde 'shared/api/<servicio>': usarlos como valor desde el espacio de
 * nombres mete el cliente entero en el bundle inicial.
 */
export * as authApi from './lib/api/auth-service';
export * as catalogApi from './lib/api/catalog-service';
export * as deliveryApi from './lib/api/delivery-service';
export * as notificationApi from './lib/api/notification-service';
export * as orderApi from './lib/api/order-service';
export * as paymentApi from './lib/api/payment-service';
export * as settingsApi from './lib/api/settings-service';

export * from './lib/models';

export { SesionStore } from './lib/auth/sesion.store';
export { RefrescoSesion } from './lib/auth/refresco-sesion.service';
export { CONFIG_SESION, type ConfigSesion } from './lib/auth/config-sesion';
export { autenticadoGuard, rolGuard, type RolStaff } from './lib/auth/guards';
export { provideSesion, provideSesionConfig } from './lib/auth/proveedores';

export { provideFitstoreHttp, type OpcionesHttp } from './lib/http/proveedores';
export { ToastService, type Toast, type TipoToast } from './lib/http/toast.service';
export { EstadoServicio } from './lib/http/estado-servicio';
export { camposConError, comoApiError } from './lib/http/api-error';
export { CLAVE_CARRITO_ID, SIN_TOAST, leerCarritoId, olvidarCarritoId } from './lib/http/interceptores';

export * from './lib/theme/colores';
export { TemaService, CLAVE_RESPALDO_CONFIG } from './lib/theme/tema.service';
export { provideTema } from './lib/theme/proveedores';

export { provideLocaleEsPe } from './lib/i18n/proveedores';
export { PrecioPipe } from './lib/pipes/precio.pipe';
export { FechaPipe, type FormatoFecha } from './lib/pipes/fecha.pipe';
export { UbigeoCatalogo, CLAVE_CACHE_UBIGEO } from './lib/ubigeo/ubigeo-catalogo';
export { departamentoDe, nivelDe, provinciaDe, type NivelUbigeo } from './lib/ubigeo/codigos';
export { CarritoContador } from './lib/carrito/carrito-contador';
export { CONTADOR_MENU, provideContadorMenu, type ContadorMenu } from './lib/menu/contador-menu';
