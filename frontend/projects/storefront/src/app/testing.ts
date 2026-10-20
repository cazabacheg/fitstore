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

import { Provider, Type, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

/** Monta un componente sin zona con sus entradas y espera a que se estabilice. */
export async function montar<T>(
  tipo: Type<T>,
  entradas: Record<string, unknown> = {},
  proveedores: Provider[] = [],
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
