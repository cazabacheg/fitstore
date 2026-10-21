import { authApi } from 'shared';

/** Sesiones de prueba de la tienda (no se usan en el código de producción). */
export const SESION_CLIENTE: authApi.Sesion = {
  accessToken: 'token-cliente-de-prueba',
  expiraEn: '2026-10-20T12:15:00Z',
  cuenta: {
    id: '5b1d7e22-9c40-4f1a-b7d2-3a8e6c0f4d55',
    tipo: 'CLIENTE',
    rol: 'CLIENTE',
    email: 'rosa@correo.pe',
    nombres: 'Rosa',
    apellidos: 'Huamán',
  },
};

export const SESION_STAFF: authApi.Sesion = {
  ...SESION_CLIENTE,
  accessToken: 'token-staff-de-prueba',
  cuenta: { ...SESION_CLIENTE.cuenta, tipo: 'STAFF', rol: 'ADMIN', email: 'admin@fitstore.pe' },
};

import { Routes } from '@angular/router';
import {
  Component,
  EnvironmentProviders,
  Provider,
  Type,
  provideZonelessChangeDetection,
} from '@angular/core';
import { TestBed } from '@angular/core/testing';

/** Monta un componente sin zona con sus entradas y espera a que se estabilice. */
export async function montar<T>(
  tipo: Type<T>,
  entradas: Record<string, unknown> = {},
  proveedores: (Provider | EnvironmentProviders)[] = [],
) {
  TestBed.configureTestingModule({
    providers: [provideZonelessChangeDetection(), ...proveedores],
  });
  const fixture = TestBed.createComponent(tipo);
  for (const [nombre, valor] of Object.entries(entradas)) {
    fixture.componentRef.setInput(nombre, valor);
  }
  await fixture.whenStable();
  const el = fixture.nativeElement as HTMLElement;
  const estabilizar = async () => {
    fixture.detectChanges();
    await fixture.whenStable();
  };
  return { fixture, el, comp: fixture.componentInstance, estabilizar };
}

/** Pantalla neutra para las pruebas del layout: no pide datos ni depende de la spec dueña de la carpeta. */
@Component({ selector: 'fs-pantalla-de-prueba', template: '<p>Pantalla de prueba</p>' })
class PantallaDePrueba {}

function aPrueba(rutas: Routes): Routes {
  return rutas.map((ruta) => ({
    ...ruta,
    ...(ruta.component ? { component: PantallaDePrueba } : {}),
    ...(ruta.loadComponent ? { loadComponent: () => PantallaDePrueba } : {}),
    ...(ruta.children ? { children: aPrueba(ruta.children) } : {}),
  }));
}

/**
 * Rutas de la app con cada pantalla de `features/` cambiada por `PantallaDePrueba`, conservando rutas, guardas y
 * datos. Así las pruebas del layout siguen pasando cuando una spec reemplaza su marcador por una pantalla real que
 * hace sus propias peticiones.
 */
export function conPantallasDePrueba(rutas: Routes): Routes {
  return rutas.map((ruta) => ({
    ...ruta,
    ...(ruta.loadChildren
      ? {
          loadChildren: async () => {
            const cargado = await (ruta.loadChildren as () => Promise<unknown>)();
            return aPrueba(cargado as Routes);
          },
        }
      : {}),
    ...(ruta.children ? { children: conPantallasDePrueba(ruta.children) } : {}),
  }));
}
