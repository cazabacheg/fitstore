import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SesionStore } from '../auth/sesion.store';
import { SESION_STAFF } from '../testing/datos';
import { leerCarritoId, olvidarCarritoId } from './interceptores';
import { provideFitstoreHttp } from './proveedores';

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

function configurar(opciones: { carrito: boolean }) {
  localStorage.clear();
  TestBed.configureTestingModule({
    providers: [provideFitstoreHttp(opciones), provideHttpClientTesting()],
  });
  return {
    http: TestBed.inject(HttpClient),
    ctrl: TestBed.inject(HttpTestingController),
    store: TestBed.inject(SesionStore),
  };
}

describe('Interceptores requestId y autenticación', () => {
  it('CA-0b-09 una petición protegida con sesión lleva Authorization: Bearer y X-Request-Id', () => {
    const { http, ctrl, store } = configurar({ carrito: false });
    store.iniciarSesion(SESION_STAFF);

    http.get('/api/v1/pedidos').subscribe();

    const req = ctrl.expectOne('/api/v1/pedidos');
    expect(req.request.headers.get('Authorization')).toBe(`Bearer ${SESION_STAFF.accessToken}`);
    expect(req.request.headers.get('X-Request-Id')).toMatch(UUID);
  });

  it('CA-0b-09 sin sesión no manda Authorization pero sí X-Request-Id, distinto en cada petición', () => {
    const { http, ctrl } = configurar({ carrito: false });

    http.get('/api/v1/productos').subscribe();
    http.get('/api/v1/productos').subscribe();

    const [a, b] = ctrl.match('/api/v1/productos');
    expect(a.request.headers.has('Authorization')).toBe(false);
    expect(a.request.headers.get('X-Request-Id')).toMatch(UUID);
    expect(a.request.headers.get('X-Request-Id')).not.toBe(b.request.headers.get('X-Request-Id'));
  });
});

describe('Interceptor carritoId', () => {
  it('CA-0b-14 y CA-4-09 la tienda sin sesión manda X-Carrito-Id en /carrito, creando y guardando el UUID', () => {
    const { http, ctrl, store } = configurar({ carrito: true });
    expect(store.autenticado()).toBe(false);

    http.get('/api/v1/carrito').subscribe();
    http.post('/api/v1/carrito/items', { varianteId: 1, cantidad: 2 }).subscribe();

    const [lectura, alta] = ctrl.match((r) => r.url.startsWith('/api/v1/carrito'));
    const id = lectura.request.headers.get('X-Carrito-Id');
    expect(id).toMatch(UUID);
    expect(alta.request.headers.get('X-Carrito-Id')).toBe(id);
    expect(localStorage.getItem('fs_carrito_id')).toBe(id);
  });

  it('CA-0b-14 reutiliza el UUID que ya estaba guardado en el navegador', () => {
    localStorage.clear();
    const { http, ctrl } = configurar({ carrito: true });
    localStorage.setItem('fs_carrito_id', '3f6b2a10-7c4e-4a52-9d1b-0e8f5a6c7d21');

    http.delete('/api/v1/carrito').subscribe();

    const req = ctrl.expectOne('/api/v1/carrito');
    expect(req.request.headers.get('X-Carrito-Id')).toBe('3f6b2a10-7c4e-4a52-9d1b-0e8f5a6c7d21');
  });

  it('CA-0b-14 no manda X-Carrito-Id fuera de /carrito', () => {
    const { http, ctrl } = configurar({ carrito: true });

    http.get('/api/v1/productos').subscribe();

    expect(ctrl.expectOne('/api/v1/productos').request.headers.has('X-Carrito-Id')).toBe(false);
  });

  it('CA-0b-14 el backoffice (sin carrito) nunca manda X-Carrito-Id', () => {
    const { http, ctrl } = configurar({ carrito: false });

    http.get('/api/v1/carrito').subscribe();

    expect(ctrl.expectOne('/api/v1/carrito').request.headers.has('X-Carrito-Id')).toBe(false);
  });

  it('CA-0b-14 sigue mandando X-Carrito-Id con sesión (la fusión necesita el JWT y el id)', () => {
    const { http, ctrl, store } = configurar({ carrito: true });
    localStorage.setItem('fs_carrito_id', '3f6b2a10-7c4e-4a52-9d1b-0e8f5a6c7d21');
    store.iniciarSesion({ ...SESION_STAFF, accessToken: 'tok' });

    http.post('/api/v1/carrito/fusionar', {}).subscribe();

    const req = ctrl.expectOne('/api/v1/carrito/fusionar');
    expect(req.request.headers.get('X-Carrito-Id')).toBe('3f6b2a10-7c4e-4a52-9d1b-0e8f5a6c7d21');
    expect(req.request.headers.get('Authorization')).toBe('Bearer tok');
  });

  it('CA-4-10 con sesión no inventa un carritoId nuevo: sin id guardado no manda la cabecera ni lo guarda', () => {
    const { http, ctrl, store } = configurar({ carrito: true });
    store.iniciarSesion({ ...SESION_STAFF, accessToken: 'tok' });

    http.get('/api/v1/carrito').subscribe();

    expect(ctrl.expectOne('/api/v1/carrito').request.headers.has('X-Carrito-Id')).toBe(false);
    expect(localStorage.getItem('fs_carrito_id')).toBeNull();
  });

  it('CA-4-10 olvidarCarritoId borra el id del navegador y leerCarritoId deja de verlo', () => {
    configurar({ carrito: true });
    localStorage.setItem('fs_carrito_id', '3f6b2a10-7c4e-4a52-9d1b-0e8f5a6c7d21');
    expect(leerCarritoId()).toBe('3f6b2a10-7c4e-4a52-9d1b-0e8f5a6c7d21');

    olvidarCarritoId();

    expect(leerCarritoId()).toBeNull();
    expect(localStorage.getItem('fs_carrito_id')).toBeNull();
  });
});
