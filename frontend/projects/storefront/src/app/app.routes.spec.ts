import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { SesionStore, provideSesionConfig } from 'shared';
import { crearRutas } from './app.routes';
import { SESION_CLIENTE, SESION_STAFF } from './testing';

async function navegar(url: string, sesion?: typeof SESION_CLIENTE, dev = true) {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter(crearRutas(dev)),
      provideSesionConfig({ tipo: 'CLIENTE', rutaLogin: '/ingresar', rutaSinPermiso: '/403' }),
    ],
  });
  if (sesion) TestBed.inject(SesionStore).iniciarSesion(sesion);
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  // El shell pide las categorías y la configuración: aquí no importan.
  TestBed.inject(HttpTestingController)
    .match(() => true)
    .forEach((r) => r.flush({ success: true, mensaje: '', data: [] }));
  harness.fixture.detectChanges();
  return { router: TestBed.inject(Router), texto: harness.routeNativeElement?.textContent ?? '' };
}

// Las rutas de acceso y de cuenta (SPEC-1) ya no son marcadores: sus pantallas tienen sus propias pruebas en features/.
const PUBLICAS = [
  '/',
  '/productos',
  '/categoria/leggings',
  '/producto/legging-negro',
  '/carrito',
  '/libro-de-reclamaciones',
  '/politicas/terminos',
];

const PROTEGIDAS_CON_MARCADOR = [
  '/checkout',
  '/pago/FS-000124',
  '/cuenta/pedidos',
  '/cuenta/pedidos/FS-000124',
];

const PROTEGIDAS = [
  '/checkout',
  '/pago/FS-000124',
  '/cuenta/pedidos',
  '/cuenta/pedidos/FS-000124',
  '/cuenta/perfil',
  '/cuenta/direcciones',
];

describe('Rutas de la tienda', () => {
  for (const url of PUBLICAS) {
    it(`CA-0b-15 ${url} carga de forma diferida y muestra "Próximamente"`, async () => {
      const { router, texto } = await navegar(url);
      expect(router.url).toBe(url);
      expect(texto).toContain('Próximamente');
    });
  }

  for (const url of PROTEGIDAS_CON_MARCADOR) {
    it(`CA-0b-15 ${url} con sesión de CLIENTE muestra "Próximamente"`, async () => {
      const { router, texto } = await navegar(url, SESION_CLIENTE);
      expect(router.url).toBe(url);
      expect(texto).toContain('Próximamente');
    });
  }

  for (const url of PROTEGIDAS) {
    it(`CA-0b-16 ${url} sin sesión lleva a /ingresar recordando la ruta`, async () => {
      const { router } = await navegar(url);
      expect(router.url).toBe(`/ingresar?returnUrl=${encodeURIComponent(url)}`);
    });
  }

  it('CA-0b-16 una sesión de staff tampoco abre una ruta de la tienda protegida', async () => {
    const { router } = await navegar('/checkout', SESION_STAFF);
    expect(router.url).toBe('/ingresar?returnUrl=%2Fcheckout');
  });

  it('CA-0b-19 una ruta que no existe muestra la 404 con "Volver al inicio"', async () => {
    const { router, texto } = await navegar('/no-existe/nada');
    expect(router.url).toBe('/no-existe/nada');
    expect(texto).toContain('Página no encontrada');
    expect(texto).toContain('Volver al inicio');
  });

  it('CA-0b-19 "Volver al inicio" lleva a /', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter(crearRutas(true)),
      ],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/ruta-inexistente');
    const enlace = [...harness.routeNativeElement!.querySelectorAll('a')].find((a) =>
      a.textContent?.includes('Volver al inicio'),
    );
    expect(enlace?.getAttribute('href')).toBe('/');
  });

  it('CA-0b-25 en desarrollo existe /_ui', async () => {
    const { router, texto } = await navegar('/_ui', undefined, true);
    expect(router.url).toBe('/_ui');
    expect(texto).not.toContain('Página no encontrada');
  });

  it('CA-0b-25 en producción la ruta /_ui no existe', async () => {
    const { texto } = await navegar('/_ui', undefined, false);
    expect(texto).toContain('Página no encontrada');
  });
});
