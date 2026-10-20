import { InjectionToken, Provider } from '@angular/core';

/** Contador de un ítem del menú del backoffice (R3), aportado por la funcionalidad dueña del ítem. */
export interface ContadorMenu {
  /** Clave del ítem del menú (p. ej. `reembolsos`). */
  clave: string;
  /** Devuelve el número que se muestra como insignia; el shell la vuelve a llamar al navegar. */
  contar: () => number;
}

/** *Multi provider*: cada funcionalidad registra el suyo sin tocar el shell. */
export const CONTADOR_MENU = new InjectionToken<ContadorMenu[]>('CONTADOR_MENU');

export function provideContadorMenu(clave: string, contar: () => number): Provider {
  return {
    provide: CONTADOR_MENU,
    multi: true,
    useValue: { clave, contar } satisfies ContadorMenu,
  };
}
