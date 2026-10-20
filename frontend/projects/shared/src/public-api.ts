/*
 * API pública de shared.
 * Cada servicio generado se expone en su propio espacio de nombres porque los 7 clientes repiten los modelos
 * comunes (ApiResponse, ApiError…): `authApi.AutenticacionService`, `settingsApi.UbigeoService`, etc.
 */
export * as authApi from './lib/api/auth-service';
export * as catalogApi from './lib/api/catalog-service';
export * as deliveryApi from './lib/api/delivery-service';
export * as notificationApi from './lib/api/notification-service';
export * as orderApi from './lib/api/order-service';
export * as paymentApi from './lib/api/payment-service';
export * as settingsApi from './lib/api/settings-service';
