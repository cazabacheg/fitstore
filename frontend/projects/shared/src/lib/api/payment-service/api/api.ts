export * from './interno.service';
import { InternoService } from './interno.service';
export * from './pago.service';
import { PagoService } from './pago.service';
export * from './pagosAdmin.service';
import { PagosAdminService } from './pagosAdmin.service';
export * from './webhooks.service';
import { WebhooksService } from './webhooks.service';
export const APIS = [InternoService, PagoService, PagosAdminService, WebhooksService];
