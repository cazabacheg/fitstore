import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationInitStatus, LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { CONFIG_SESION, SesionStore } from 'shared';
import { appConfig } from './app.config';

async function arrancar() {
  TestBed.configureTestingModule({
    providers: [...appConfig.providers, provideHttpClientTesting()],
  });
  const ctrl = TestBed.inject(HttpTestingController);
  const init = TestBed.inject(ApplicationInitStatus);
  // Al arrancar: recupera la sesión por cookie y carga la marca. Sin cookie ni settings, la app arranca igual.
  ctrl.expectOne('/api/v1/auth/refresh').flush(null, { status: 401, statusText: 'Unauthorized' });
  ctrl.expectOne('/api/v1/settings/public').error(new ProgressEvent('error'));
  await init.donePromise;
  return ctrl;
}

describe('Configuración de la tienda (app.config)', () => {
  beforeEach(() => localStorage.clear());

  it('CA-0b-12 arranca recuperando la sesión y la marca, sin bloquearse si no hay cookie ni settings', async () => {
    await arrancar();
    expect(TestBed.inject(SesionStore).autenticado()).toBe(false);
  });

  it('CA-0b-18 la tienda solo admite cuentas CLIENTE y su login es /ingresar', async () => {
    await arrancar();
    expect(TestBed.inject(CONFIG_SESION)).toMatchObject({
      tipo: 'CLIENTE',
      rutaLogin: '/ingresar',
    });
  });

  it('CA-0b-14 la tienda manda X-Carrito-Id y X-Request-Id en /carrito', async () => {
    const ctrl = await arrancar();
    TestBed.inject(HttpClient).get('/api/v1/carrito').subscribe();
    const req = ctrl.expectOne('/api/v1/carrito');
    expect(req.request.headers.get('X-Carrito-Id')).toBeTruthy();
    expect(req.request.headers.get('X-Request-Id')).toBeTruthy();
  });

  it('CA-0b-27 el idioma de la tienda es es-PE', async () => {
    await arrancar();
    expect(TestBed.inject(LOCALE_ID)).toBe('es-PE');
  });
});
