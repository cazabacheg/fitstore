import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { CONFIG_SESION, SesionStore } from 'shared';

/**
 * Quien ya tiene sesión de la tienda no necesita el ingreso: va a su perfil (P-1-01). Una sesión de otro tipo
 * (staff) no cuenta, o se redirigiría en bucle con `autenticadoGuard`.
 */
export const sinSesionGuard: CanActivateFn = () => {
  const sesion = inject(SesionStore);
  const { tipo } = inject(CONFIG_SESION);
  const propia = sesion.autenticado() && (tipo === undefined || sesion.tipo() === tipo);
  return propia ? inject(Router).createUrlTree(['/cuenta/perfil']) : true;
};
