import { Component, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { SesionStore, provideFitstoreHttp, provideSesionConfig } from 'shared';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { SESION_CLIENTE } from '../../testing';
import { http } from './pruebas-cuenta';
import { CuentaLayout } from './cuenta-layout';
import { CUENTA_ROUTES } from './cuenta.routes';

@Component({ template: 'inicio' })
class Inicio {}

async function arrancar() {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([
        { path: '', pathMatch: 'full', component: Inicio },
        { path: 'cuenta', children: CUENTA_ROUTES },
        { path: 'cuenta/pedidos', component: Inicio },
      ]),
      provideFitstoreHttp({ carrito: false }),
      provideHttpClientTesting(),
      provideSesionConfig({ tipo: 'CLIENTE' }),
    ],
  });
  TestBed.inject(SesionStore).iniciarSesion(SESION_CLIENTE);
  return RouterTestingHarness.create();
}

describe('CuentaLayout (P-1-05)', () => {
  it('CA-1-15 el menú lateral lleva a Perfil, Direcciones y Mis pedidos, y /cuenta abre el perfil', async () => {
    const h = await arrancar();
    await h.navigateByUrl('/cuenta');
    http()
      .match(() => true)
      .forEach((r) => r.flush({ success: true, mensaje: '', data: null }));

    expect(TestBed.inject(Router).url).toBe('/cuenta/perfil');
    const enlaces = [...(h.routeNativeElement as HTMLElement).querySelectorAll('nav a')].map(
      (a) => [a.textContent?.trim(), a.getAttribute('href')],
    );
    expect(enlaces).toEqual([
      ['Perfil', '/cuenta/perfil'],
      ['Direcciones', '/cuenta/direcciones'],
      ['Mis pedidos', '/cuenta/pedidos'],
    ]);
  });

  it('CA-1-10 "Cerrar sesión" llama a /auth/logout, borra la sesión local y vuelve al inicio', async () => {
    const h = await arrancar();
    await h.navigateByUrl('/cuenta/direcciones');
    http()
      .match(() => true)
      .forEach((r) => r.flush({ success: true, mensaje: '', data: [] }));
    const boton = [
      ...(h.routeNativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button'),
    ].find((b) => b.textContent?.includes('Cerrar sesión'))!;
    boton.click();

    const peticion = http().expectOne('/api/v1/auth/logout');
    expect(peticion.request.method).toBe('POST');
    expect(TestBed.inject(SesionStore).autenticado()).toBe(true); // se borra cuando el servidor responde
    peticion.flush({ success: true, mensaje: 'Sesión cerrada', data: null });
    await h.fixture.whenStable();

    expect(TestBed.inject(SesionStore).autenticado()).toBe(false);
    expect(TestBed.inject(Router).url).toBe('/');
  });

  it('CA-1-10 aunque el servidor falle, la sesión local se cierra', async () => {
    const h = await arrancar();
    await h.navigateByUrl('/cuenta/direcciones');
    http()
      .match(() => true)
      .forEach((r) => r.flush({ success: true, mensaje: '', data: [] }));
    [...(h.routeNativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button')]
      .find((b) => b.textContent?.includes('Cerrar sesión'))!
      .click();

    http().expectOne('/api/v1/auth/logout').flush(null, { status: 500, statusText: 'Error' });
    await h.fixture.whenStable();

    expect(TestBed.inject(SesionStore).autenticado()).toBe(false);
  });

  it('CA-1-15 CuentaLayout se puede usar suelto', () => {
    expect(CuentaLayout).toBeDefined();
  });
});
