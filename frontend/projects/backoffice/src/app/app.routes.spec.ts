import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { SesionStore, provideSesionConfig } from 'shared';
import { crearRutas } from './app.routes';
import { SESION_ADMIN, SESION_CLIENTE, SESION_OPERADOR } from './testing/sesiones';

/** Rutas de la tabla R2 del backoffice (con las rutas que fijan las specs dueñas). */
const RUTAS_PROTEGIDAS = [
  '/',
  '/perfil',
  '/staff',
  '/clientes',
  '/catalogo/productos',
  '/catalogo/categorias',
  '/catalogo/maestros',
  '/catalogo/descuentos',
  '/catalogo/productos/carga-masiva',
  '/inventario',
  '/pedidos',
  '/pedidos/FS-000124',
  '/entregas',
  '/entregas/dia',
  '/repartidores',
  '/tarifas',
  '/agencias',
  '/pagos',
  '/reembolsos',
  '/notificaciones',
  '/notificaciones/plantillas',
  '/configuracion',
  '/configuracion/politicas',
  '/reclamos',
];
const RUTAS_PUBLICAS = ['/ingresar', '/recuperar', '/restablecer'];
const SOLO_ADMIN = [
  '/staff',
  '/catalogo/categorias',
  '/catalogo/maestros',
  '/catalogo/descuentos',
  '/catalogo/productos/carga-masiva',
  '/pagos',
  '/reembolsos',
  '/tarifas',
  '/agencias',
  '/notificaciones',
  '/notificaciones/plantillas',
  '/configuracion',
  '/configuracion/politicas',
];
const TAMBIEN_OPERADOR = RUTAS_PROTEGIDAS.filter((r) => !SOLO_ADMIN.includes(r));

async function arrancar(sesion?: Parameters<SesionStore['iniciarSesion']>[0], dev = true) {
  TestBed.configureTestingModule({
    providers: [
      provideRouter(crearRutas(dev)),
      provideHttpClient(),
      provideHttpClientTesting(),
      provideSesionConfig({ tipo: 'STAFF', rutaLogin: '/ingresar', rutaSinPermiso: '/403' }),
    ],
  });
  if (sesion) TestBed.inject(SesionStore).iniciarSesion(sesion);
  return RouterTestingHarness.create();
}

describe('Rutas del backoffice', () => {
  it.each([...RUTAS_PROTEGIDAS, ...RUTAS_PUBLICAS.map((r) => r)])(
    'CA-0b-15 %s carga de forma diferida y muestra "Próximamente"',
    async (ruta) => {
      const publica = RUTAS_PUBLICAS.includes(ruta);
      const harness = await arrancar(publica ? undefined : SESION_ADMIN);
      await harness.navigateByUrl(ruta);
      expect(TestBed.inject(Router).url).toBe(ruta);
      expect(harness.routeNativeElement?.textContent).toContain('Próximamente');
    },
  );

  it('CA-0b-15 cada ruta de R2 se carga con loadChildren (carga diferida)', () => {
    const raiz = crearRutas(true);
    const lista = JSON.stringify(raiz, (_k, v) => (typeof v === 'function' ? 'fn' : v));
    expect(lista).toContain('"loadChildren":"fn"');
    const planas = raiz.flatMap((r) => [r, ...(r.children ?? [])]);
    const conCarga = planas.filter((r) => r.loadChildren).length;
    expect(conCarga).toBeGreaterThanOrEqual(20);
  });

  it.each(RUTAS_PROTEGIDAS)(
    'CA-0b-16 sin sesión, %s lleva al login recordando la ruta',
    async (ruta) => {
      const harness = await arrancar();
      await harness.navigateByUrl('/recuperar'); // parte de otra URL para que '/' no cuente como la misma
      await harness.navigateByUrl(ruta);
      const url = TestBed.inject(Router).url;
      expect(url.startsWith('/ingresar?returnUrl=')).toBe(true);
      expect(decodeURIComponent(url.split('returnUrl=')[1])).toBe(ruta);
    },
  );

  it.each(SOLO_ADMIN)(
    'CA-0b-17 un OPERADOR que entra por URL a %s ve la página 403',
    async (ruta) => {
      const harness = await arrancar(SESION_OPERADOR);
      await harness.navigateByUrl(ruta);
      expect(TestBed.inject(Router).url).toBe('/403');
      expect(harness.routeNativeElement?.textContent).toContain('No tienes permiso');
    },
  );

  it.each(TAMBIEN_OPERADOR)('CA-0b-17 un OPERADOR sí entra a %s', async (ruta) => {
    const harness = await arrancar(SESION_OPERADOR);
    await harness.navigateByUrl(ruta);
    expect(TestBed.inject(Router).url).toBe(ruta);
  });

  it('CA-0b-17 un ADMIN entra a las rutas de ADMIN', async () => {
    const harness = await arrancar(SESION_ADMIN);
    for (const ruta of SOLO_ADMIN) {
      await harness.navigateByUrl(ruta);
      expect(TestBed.inject(Router).url).toBe(ruta);
    }
  });

  it('CA-0b-18 una sesión de CLIENTE no entra al backoffice y va al login de staff', async () => {
    const harness = await arrancar(SESION_CLIENTE);
    await harness.navigateByUrl('/pedidos');
    expect(TestBed.inject(Router).url).toBe('/ingresar?returnUrl=%2Fpedidos');
    await harness.navigateByUrl('/staff');
    expect(TestBed.inject(Router).url).toBe('/ingresar?returnUrl=%2Fstaff');
  });

  it('CA-0b-19 una ruta inexistente con sesión muestra la 404 dentro del shell, con "Volver al inicio"', async () => {
    const harness = await arrancar(SESION_ADMIN);
    await harness.navigateByUrl('/no-existe/nada');
    const el = harness.routeNativeElement as HTMLElement;
    expect(el.textContent).toContain('Página no encontrada');
    expect(
      el.localName === 'fs-backoffice-shell' || el.querySelector('fs-backoffice-shell'),
    ).toBeTruthy();
    const enlace = [...el.querySelectorAll('a')].find((a) =>
      a.textContent?.includes('Volver al inicio'),
    );
    expect(enlace?.getAttribute('href')).toBe('/');
  });

  it('CA-0b-19 una ruta inexistente sin sesión muestra la 404 fuera del shell', async () => {
    const harness = await arrancar();
    await harness.navigateByUrl('/no-existe');
    const el = harness.routeNativeElement as HTMLElement;
    expect(TestBed.inject(Router).url).toBe('/no-existe');
    expect(el.textContent).toContain('Página no encontrada');
    expect(el.textContent).toContain('Volver al inicio');
    expect(
      el.localName === 'fs-backoffice-shell' || el.querySelector('fs-backoffice-shell'),
    ).toBeFalsy();
  });
});
