import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { SesionStore, provideFitstoreHttp, provideSesionConfig } from 'shared';
import { SESION_CLIENTE } from '../../testing';
import { ACCESO_ROUTES } from './acceso.routes';

@Component({ template: 'perfil' })
class PerfilFalso {}

async function arrancar(conSesion: boolean) {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([
        { path: 'cuenta/perfil', component: PerfilFalso },
        { path: '', children: ACCESO_ROUTES },
      ]),
      provideFitstoreHttp({ carrito: false }),
      provideHttpClientTesting(),
      provideSesionConfig({ tipo: 'CLIENTE' }),
    ],
  });
  if (conSesion) TestBed.inject(SesionStore).iniciarSesion(SESION_CLIENTE);
  return RouterTestingHarness.create();
}

describe('Rutas de acceso de la tienda', () => {
  it('CA-1-04 /ingresar sin sesión muestra el formulario de ingreso', async () => {
    const h = await arrancar(false);
    await h.navigateByUrl('/ingresar?returnUrl=%2Fcarrito');
    expect((h.routeNativeElement as HTMLElement).textContent).toContain('Ingresa a tu cuenta');
  });

  it('CA-1-04 si ya hay sesión, /ingresar lleva a /cuenta/perfil', async () => {
    const h = await arrancar(true);
    await h.navigateByUrl('/ingresar');
    expect(TestBed.inject(Router).url).toBe('/cuenta/perfil');
  });

  it('CA-1-01 /registro, /recuperar y /restablecer son públicas y cargan su pantalla', async () => {
    const h = await arrancar(false);
    await h.navigateByUrl('/registro');
    TestBed.inject(HttpTestingController).match('/api/v1/settings/public').forEach((r) => r.flush(null, { status: 503, statusText: 'x' }));
    expect((h.routeNativeElement as HTMLElement).textContent).toContain('Crea tu cuenta');
    await h.navigateByUrl('/recuperar');
    expect((h.routeNativeElement as HTMLElement).textContent).toContain('Recupera tu contraseña');
    await h.navigateByUrl('/restablecer');
    expect((h.routeNativeElement as HTMLElement).textContent).toContain('Enlace inválido o expirado');
  });
});
