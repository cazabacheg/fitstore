import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { SesionStore } from '../auth/sesion.store';
import { SESION_NUEVA, SESION_STAFF } from '../testing/datos';
import { ToastService } from './toast.service';
import { provideFitstoreHttp } from './proveedores';

@Component({ template: '' })
class Vacio {}

function configurar() {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([
        { path: 'ingresar', component: Vacio },
        { path: 'cuenta/pedidos', component: Vacio },
        { path: '**', component: Vacio },
      ]),
      provideFitstoreHttp({ carrito: false }),
      provideHttpClientTesting(),
    ],
  });
  const store = TestBed.inject(SesionStore);
  store.iniciarSesion({ ...SESION_STAFF, accessToken: 'token-vencido' });
  return {
    http: TestBed.inject(HttpClient),
    ctrl: TestBed.inject(HttpTestingController),
    store,
    router: TestBed.inject(Router),
    toasts: TestBed.inject(ToastService),
  };
}

const NO_AUTENTICADO = {
  success: false,
  mensaje: 'Tu sesión venció, vuelve a ingresar',
  codigo: 'NO_AUTENTICADO',
};

describe('Interceptor de refresco de sesión', () => {
  it('CA-0b-10 tres 401 simultáneos provocan UN solo POST /auth/refresh y se reintentan con el token nuevo', () => {
    const { http, ctrl, store } = configurar();
    const recibidos: unknown[] = [];

    for (const ruta of ['/api/v1/pedidos', '/api/v1/clientes', '/api/v1/staff']) {
      http.get(ruta).subscribe((r) => recibidos.push(r));
    }
    for (const ruta of ['/api/v1/pedidos', '/api/v1/clientes', '/api/v1/staff']) {
      const req = ctrl.expectOne(ruta);
      expect(req.request.headers.get('Authorization')).toBe('Bearer token-vencido');
      req.flush(NO_AUTENTICADO, { status: 401, statusText: 'Unauthorized' });
    }

    const refresh = ctrl.expectOne('/api/v1/auth/refresh');
    expect(refresh.request.method).toBe('POST');
    refresh.flush({ success: true, mensaje: 'ok', data: SESION_NUEVA });

    const reintentos = ctrl.match((r) => r.url !== '/api/v1/auth/refresh');
    expect(reintentos.map((r) => r.request.url).sort()).toEqual([
      '/api/v1/clientes',
      '/api/v1/pedidos',
      '/api/v1/staff',
    ]);
    for (const r of reintentos) {
      expect(r.request.headers.get('Authorization')).toBe('Bearer token-renovado');
      r.flush({ success: true, mensaje: 'ok', data: r.request.url });
    }
    ctrl.verify();
    expect(recibidos).toHaveLength(3);
    expect(store.token()).toBe('token-renovado');
  });

  it('CA-0b-11 si el refresco falla se cierra la sesión y se navega al login recordando la ruta', async () => {
    const { http, ctrl, store, router } = configurar();
    await router.navigateByUrl('/cuenta/pedidos?pagina=2');
    let error: HttpErrorResponse | undefined;

    http.get('/api/v1/pedidos').subscribe({ error: (e) => (error = e) });
    ctrl.expectOne('/api/v1/pedidos').flush(NO_AUTENTICADO, { status: 401, statusText: 'x' });
    ctrl.expectOne('/api/v1/auth/refresh').flush(NO_AUTENTICADO, { status: 401, statusText: 'x' });
    await new Promise((r) => setTimeout(r));
    await router.navigateByUrl(router.url); // deja terminar la navegación pendiente

    expect(store.autenticado()).toBe(false);
    expect(error?.status).toBe(401);
    expect(router.url).toBe('/ingresar?returnUrl=%2Fcuenta%2Fpedidos%3Fpagina%3D2');
    ctrl.verify();
  });

  it('CA-0b-10 un reintento que vuelve a dar 401 no entra en bucle: falla y cierra la sesión', () => {
    const { http, ctrl, store } = configurar();
    let error: HttpErrorResponse | undefined;

    http.get('/api/v1/pedidos').subscribe({ error: (e) => (error = e) });
    ctrl.expectOne('/api/v1/pedidos').flush(NO_AUTENTICADO, { status: 401, statusText: 'x' });
    ctrl
      .expectOne('/api/v1/auth/refresh')
      .flush({ success: true, mensaje: 'ok', data: SESION_NUEVA });
    ctrl.expectOne('/api/v1/pedidos').flush(NO_AUTENTICADO, { status: 401, statusText: 'x' });

    expect(error?.status).toBe(401);
    expect(store.autenticado()).toBe(false);
    ctrl.expectNone('/api/v1/auth/refresh');
  });

  it.each([
    ['login de cliente', '/api/v1/auth/login'],
    ['login de staff', '/api/v1/auth/staff/login'],
    ['refresh', '/api/v1/auth/refresh'],
    ['logout', '/api/v1/auth/logout'],
  ])('CA-0b-11 un 401 en %s no dispara refresco ni toast', (_nombre, ruta) => {
    const { http, ctrl, toasts } = configurar();
    let error: HttpErrorResponse | undefined;

    http.post(ruta, {}).subscribe({ error: (e) => (error = e) });
    ctrl
      .expectOne(ruta)
      .flush(
        { success: false, mensaje: 'Credenciales inválidas', codigo: 'CREDENCIALES_INVALIDAS' },
        { status: 401, statusText: 'x' },
      );

    expect(error?.status).toBe(401);
    expect(toasts.toasts()).toHaveLength(0);
    ctrl.verify(); // ninguna petición extra (ni /auth/refresh)
  });
});
