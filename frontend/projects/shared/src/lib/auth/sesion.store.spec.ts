import { TestBed } from '@angular/core/testing';
import { SesionStore } from './sesion.store';
import { SESION_STAFF } from '../testing/datos';

describe('SesionStore', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('CA-0b-09 guarda la cuenta, el rol y el tipo al iniciar sesión y expone el token', () => {
    const store = TestBed.inject(SesionStore);
    expect(store.autenticado()).toBe(false);
    expect(store.token()).toBeNull();

    store.iniciarSesion(SESION_STAFF);

    expect(store.autenticado()).toBe(true);
    expect(store.cuenta()?.email).toBe('admin@fitstore.pe');
    expect(store.rol()).toBe('ADMIN');
    expect(store.tipo()).toBe('STAFF');
    expect(store.token()).toBe(SESION_STAFF.accessToken);
  });

  it('CA-0b-11 cerrar la sesión borra la cuenta y el token', () => {
    const store = TestBed.inject(SesionStore);
    store.iniciarSesion(SESION_STAFF);

    store.cerrarSesion();

    expect(store.autenticado()).toBe(false);
    expect(store.cuenta()).toBeNull();
    expect(store.rol()).toBeNull();
    expect(store.token()).toBeNull();
  });

  it('CA-0b-09 el access token vive solo en memoria: nunca en localStorage ni sessionStorage', () => {
    const store = TestBed.inject(SesionStore);
    store.iniciarSesion(SESION_STAFF);

    const guardado = JSON.stringify({ ...localStorage }) + JSON.stringify({ ...sessionStorage });
    expect(guardado).not.toContain(SESION_STAFF.accessToken);
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });
});
