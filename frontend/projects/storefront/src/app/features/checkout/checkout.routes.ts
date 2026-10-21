import { Routes } from '@angular/router';
import { CheckoutPage } from './checkout.page';

/** Checkout (P-4-02): solo clientes con sesión (la guarda la pone `app.routes.ts`). */
export const CHECKOUT_ROUTES: Routes = [{ path: '', component: CheckoutPage }];
