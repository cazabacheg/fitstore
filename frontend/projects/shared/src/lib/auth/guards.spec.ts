import { EnvironmentInjector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { SESION_CLIENTE, SESION_OPERADOR, SESION_STAFF } from '../testing/datos';
import { autenticadoGuard, rolGuard } from './guards';
import { provideSesionConfig } from './proveedores';
import { SesionStore } from './sesion.store';

function ejecutar(guard: typeof autenticadoGuard, url: string) {
  const inyector = TestBed.inject(EnvironmentInjector);
  return runInInjectionContext(inyector, () =>
    guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
  );
}

function aString(r: unknown): string {
  return (r as UrlTree).toString();
}

describe('autenticadoGuard', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideRouter([])] }));

  it('CA-0b-16 sin sesión envía al login recordando la ruta pedida', () => {
    const r = ejecutar(autenticadoGuard, '/checkout?x=1');
    expect(r).toBeInstanceOf(UrlTree);
    expect(aString(r)).toBe('/ingresar?returnUrl=%2Fcheckout%3Fx%3D1');
  });

  it('CA-0b-16 con sesión deja pasar', () => {
    TestBed.inject(SesionStore).iniciarSesion(SESION_CLIENTE);
    expect(ejecutar(autenticadoGuard, '/checkout')).toBe(true);
  });
});

describe('autenticadoGuard del backoffice (tipo STAFF)', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideSesionConfig({ tipo: 'STAFF', rutaLogin: '/ingresar' })],
    }),
  );

  it('CA-0b-18 una sesión de CLIENTE no entra al backoffice: ve el login de staff', () => {
    TestBed.inject(SesionStore).iniciarSesion(SESION_CLIENTE);
    const r = ejecutar(autenticadoGuard, '/pedidos');
    expect(aString(r)).toBe('/ingresar?returnUrl=%2Fpedidos');
  });

  it('CA-0b-16 una sesión de STAFF entra', () => {
    TestBed.inject(SesionStore).iniciarSesion(SESION_OPERADOR);
    expect(ejecutar(autenticadoGuard, '/pedidos')).toBe(true);
  });
});

describe('rolGuard', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideSesionConfig({ tipo: 'STAFF', rutaLogin: '/ingresar' })],
    }),
  );

  it('CA-0b-17 un OPERADOR que entra por URL a una ruta de ADMIN va a la página 403', () => {
    TestBed.inject(SesionStore).iniciarSesion(SESION_OPERADOR);
    expect(aString(ejecutar(rolGuard('ADMIN'), '/pagos'))).toBe('/403');
  });

  it('CA-0b-17 un ADMIN entra a las rutas de ADMIN', () => {
    TestBed.inject(SesionStore).iniciarSesion(SESION_STAFF);
    expect(ejecutar(rolGuard('ADMIN'), '/pagos')).toBe(true);
  });

  it('CA-0b-17 un ADMIN también satisface las rutas de OPERADOR (jerarquía)', () => {
    TestBed.inject(SesionStore).iniciarSesion(SESION_STAFF);
    expect(ejecutar(rolGuard('OPERADOR'), '/pedidos')).toBe(true);
  });

  it('CA-0b-17 un OPERADOR entra a las rutas de OPERADOR', () => {
    TestBed.inject(SesionStore).iniciarSesion(SESION_OPERADOR);
    expect(ejecutar(rolGuard('OPERADOR'), '/pedidos')).toBe(true);
  });

  it('CA-0b-16 sin sesión, el rolGuard envía al login', () => {
    expect(aString(ejecutar(rolGuard('ADMIN'), '/pagos'))).toBe('/ingresar?returnUrl=%2Fpagos');
  });
});
