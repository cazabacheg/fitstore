import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { CONFIG_SESION, SesionStore } from 'shared';

/** Quien ya tiene sesión de staff no necesita el ingreso: va al dashboard (P-1-07). */
export const sinSesionGuard: CanActivateFn = () => {
  const sesion = inject(SesionStore);
  const { tipo } = inject(CONFIG_SESION);
  const propia = sesion.autenticado() && (tipo === undefined || sesion.tipo() === tipo);
  return propia ? inject(Router).createUrlTree(['/']) : true;
};
