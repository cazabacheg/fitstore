import { Component, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, Routes, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { SESION_CLIENTE, SESION_OPERADOR } from '../testing/datos';
import { autenticadoGuard, rolGuard } from './guards';
import { provideSesionConfig } from './proveedores';
import { SesionStore } from './sesion.store';

@Component({ template: '<p id="pagina">Próximamente</p>' })
class Proximamente {}

@Component({ template: '<p id="pagina">Página no encontrada</p>' })
class NoEncontrada {}

const RUTAS: Routes = [
  { path: 'ingresar', component: Proximamente },
  { path: 'checkout', canActivate: [autenticadoGuard], loadComponent: async () => Proximamente },
  { path: 'pagos', canActivate: [rolGuard('ADMIN')], loadComponent: async () => Proximamente },
  { path: '403', component: Proximamente },
  { path: '**', component: NoEncontrada },
];

async function navegar(url: string, sesion?: typeof SESION_CLIENTE, tipo?: 'CLIENTE' | 'STAFF') {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter(RUTAS),
      provideSesionConfig({ tipo }),
    ],
  });
  if (sesion) TestBed.inject(SesionStore).iniciarSesion(sesion);
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  return { router: TestBed.inject(Router), harness };
}

describe('Guards dentro de las rutas', () => {
  it('CA-0b-15 una ruta protegida con carga diferida se abre cuando hay sesión', async () => {
    const { router, harness } = await navegar('/checkout', SESION_CLIENTE, 'CLIENTE');
    expect(router.url).toBe('/checkout');
    expect(harness.routeNativeElement?.querySelector('#pagina')?.textContent).toBe('Próximamente');
  });

  it('CA-0b-16 sin sesión, una ruta protegida lleva al login y recuerda la ruta', async () => {
    const { router } = await navegar('/checkout', undefined, 'CLIENTE');
    expect(router.url).toBe('/ingresar?returnUrl=%2Fcheckout');
  });

  it('CA-0b-17 un OPERADOR que entra por URL a una ruta de ADMIN ve la página 403', async () => {
    const { router } = await navegar('/pagos', SESION_OPERADOR, 'STAFF');
    expect(router.url).toBe('/403');
  });

  it('CA-0b-19 una ruta que no existe no pasa por los guards y muestra la página 404', async () => {
    const { router, harness } = await navegar('/no-existe');
    expect(router.url).toBe('/no-existe');
    expect(harness.routeNativeElement?.querySelector('#pagina')?.textContent).toBe(
      'Página no encontrada',
    );
  });
});
