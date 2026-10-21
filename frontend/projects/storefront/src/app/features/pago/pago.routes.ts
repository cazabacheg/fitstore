import { Routes } from '@angular/router';
import { PagoPage } from './pago.page';

/** `/pago/:codigo`: la página de pago (P-5-01) y su resultado (P-5-02) son la misma ruta (P3). */
export const PAGO_ROUTES: Routes = [{ path: '', component: PagoPage }];
