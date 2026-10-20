import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { CONFIG_SESION } from './config-sesion';
import { SesionStore } from './sesion.store';

export type RolStaff = 'OPERADOR' | 'ADMIN';

/** ADMIN puede todo lo que puede un OPERADOR. */
const NIVEL: Record<string, number> = { CLIENTE: 0, OPERADOR: 1, ADMIN: 2 };

function irAlLogin(url: string): UrlTree {
  const router = inject(Router);
  const { rutaLogin } = inject(CONFIG_SESION);
  return router.createUrlTree([rutaLogin], { queryParams: { returnUrl: url } });
}

/** Exige una sesión del tipo que admite la app; si no, va al login recordando la ruta (CA-0b-16, CA-0b-18). */
export const autenticadoGuard: CanActivateFn = (_ruta, estado) => {
  const store = inject(SesionStore);
  const { tipo } = inject(CONFIG_SESION);
  const valida = store.autenticado() && (tipo === undefined || store.tipo() === tipo);
  return valida ? true : irAlLogin(estado.url);
};

/** Exige sesión y, además, un rol mínimo; sin él va a la página 403 (CA-0b-17). */
export function rolGuard(minimo: RolStaff): CanActivateFn {
  return (ruta, estado) => {
    const sesion = autenticadoGuard(ruta, estado);
    if (sesion !== true) return sesion;
    const rol = inject(SesionStore).rol();
    if ((NIVEL[rol ?? ''] ?? -1) >= NIVEL[minimo]) return true;
    return inject(Router).createUrlTree([inject(CONFIG_SESION).rutaSinPermiso]);
  };
}
