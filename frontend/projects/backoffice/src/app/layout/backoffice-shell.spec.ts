import { HttpTestingController } from '@angular/common/http/testing';
import { OverlayContainer } from '@angular/cdk/overlay';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { CLAVE_RESPALDO_CONFIG, SesionStore, TemaService, provideContadorMenu } from 'shared';
import { montar } from '../testing/montar';
import { SESION_ADMIN, SESION_OPERADOR } from '../testing/sesiones';

function raiz(harness: { routeNativeElement: Element | null }): HTMLElement {
  return harness.routeNativeElement as HTMLElement;
}
function menu(el: HTMLElement): HTMLElement {
  return el.querySelector('nav[aria-label="Menú del backoffice"]') as HTMLElement;
}
function textos(el: HTMLElement, selector: string): string[] {
  return [...el.querySelectorAll(selector)].map(
    (n) => n.textContent?.replace(/\s+/g, ' ').trim() ?? '',
  );
}
function porTexto(el: HTMLElement, selector: string, texto: string): HTMLElement {
  const nodo = [...el.querySelectorAll<HTMLElement>(selector)].find((n) =>
    n.textContent?.includes(texto),
  );
  if (!nodo) throw new Error(`No hay ${selector} con "${texto}"`);
  return nodo;
}
async function estable(harness: {
  fixture: { whenStable(): Promise<void>; detectChanges(): void };
}) {
  harness.fixture.detectChanges();
  await harness.fixture.whenStable();
  harness.fixture.detectChanges();
}

describe('BackofficeShell', () => {
  beforeEach(() => localStorage.clear());

  it('CA-0b-17 el menú del OPERADOR no muestra las secciones de ADMIN', async () => {
    const h = await montar(SESION_OPERADOR, { url: '/pedidos' });
    const items = textos(menu(raiz(h)), '[data-clave] > a, [data-clave] > button');
    for (const prohibida of [
      'Pagos',
      'Tarifas y agencias',
      'Staff',
      'Notificaciones',
      'Configuración de tienda',
    ]) {
      expect(
        items.some((t) => t.includes(prohibida)),
        prohibida,
      ).toBe(false);
    }
    expect(items.some((t) => t.includes('Pedidos'))).toBe(true);
  });

  it.each([
    ['Admin', SESION_ADMIN],
    ['Operador', SESION_OPERADOR],
  ])('CA-0b-17 bajo la marca, el menú muestra el rol de la sesión: %s', async (rol, sesion) => {
    const h = await montar(sesion, { url: '/pedidos' });
    expect(menu(raiz(h)).querySelector('.fs-menu__rol')?.textContent?.trim()).toBe(rol);
  });

  it('CA-0b-17 el ADMIN ve los 13 ítems del menú', async () => {
    const h = await montar(SESION_ADMIN, { url: '/pedidos' });
    expect(raiz(h).querySelectorAll('[data-clave][data-nivel="item"]').length).toBe(13);
  });

  it('CA-0b-24 muestra migas, título de página y el ítem activo resaltado', async () => {
    const h = await montar(SESION_ADMIN, { url: '/catalogo/categorias' });
    const el = raiz(h);
    expect(textos(el, 'nav[aria-label="Migas"] > *').join('|')).toBe(
      'Inicio|/|Catálogo|/|Categorías',
    );
    expect(porTexto(el, 'nav[aria-label="Migas"] a', 'Inicio').getAttribute('href')).toBe('/');
    expect(el.querySelector('h1')?.textContent).toContain('Categorías');
    // El grupo del ítem activo se abre solo y el subítem queda marcado.
    const activos = [...el.querySelectorAll('[aria-current="page"]')];
    expect(activos.length).toBe(1);
    expect(activos[0].textContent).toContain('Categorías');
  });

  it('CA-0b-24 con una ruta hija activa, solo queda marcado el subítem más específico', async () => {
    const h = await montar(SESION_ADMIN, { url: '/catalogo/productos/carga-masiva' });
    const activos = [...raiz(h).querySelectorAll('[aria-current="page"]')].map((a) =>
      a.textContent?.trim(),
    );
    expect(activos).toEqual(['Carga masiva']);
    await h.navigateByUrl('/pedidos/FS-000124');
    const activos2 = [...raiz(h).querySelectorAll('[aria-current="page"]')].map((a) =>
      a.textContent?.trim(),
    );
    expect(activos2.some((t) => t?.includes('Pedidos'))).toBe(true);
    await h.navigateByUrl('/');
    expect(
      [...raiz(h).querySelectorAll('[aria-current="page"]')].map((a) => a.textContent?.trim())[0],
    ).toContain('Dashboard');
  });

  it('CA-0b-24 un grupo se abre y se cierra con su botón y muestra sus subítems', async () => {
    const h = await montar(SESION_ADMIN, { url: '/pedidos' });
    const el = raiz(h);
    const grupo = porTexto(el, '[data-clave="entregas"] > button', 'Entregas');
    expect(grupo.getAttribute('aria-expanded')).toBe('false');
    expect(el.querySelector('[data-clave="entregas"] ul')).toBeNull();
    grupo.click();
    await estable(h);
    expect(grupo.getAttribute('aria-expanded')).toBe('true');
    expect(textos(el, '[data-clave="entregas"] ul a')).toEqual(['Lista', 'Del día']);
    grupo.click();
    await estable(h);
    expect(el.querySelector('[data-clave="entregas"] ul')).toBeNull();
  });

  it('CA-0b-24 un ítem con contador registrado muestra su número y el grupo padre la suma', async () => {
    const fallidos = 2;
    const h = await montar(SESION_ADMIN, {
      url: '/pedidos',
      proveedores: [provideContadorMenu('reembolsos', () => fallidos)],
    });
    const el = raiz(h);
    expect(
      el.querySelector('[data-clave="pagos"] > button .fs-contador')?.textContent?.trim(),
    ).toBe('2');
    porTexto(el, '[data-clave="pagos"] > button', 'Pagos').click();
    await estable(h);
    expect(el.querySelector('[data-clave="reembolsos"] .fs-contador')?.textContent?.trim()).toBe(
      '2',
    );
    expect(el.querySelector('[data-clave="pedidos"] .fs-contador')).toBeNull();
  });

  it('CA-0b-24 el contador se refresca al navegar y se oculta en cero', async () => {
    let fallidos = 2;
    const h = await montar(SESION_ADMIN, {
      url: '/pedidos',
      proveedores: [provideContadorMenu('reembolsos', () => fallidos)],
    });
    const el = raiz(h);
    fallidos = 5;
    await h.navigateByUrl('/inventario');
    await estable(h);
    expect(
      el.querySelector('[data-clave="pagos"] > button .fs-contador')?.textContent?.trim(),
    ).toBe('5');
    fallidos = 0;
    await h.navigateByUrl('/pedidos');
    await estable(h);
    expect(el.querySelector('[data-clave="pagos"] .fs-contador')).toBeNull();
  });

  it('CA-0b-24 el menú de usuario ofrece perfil, cambiar contraseña y salir', async () => {
    const h = await montar(SESION_ADMIN, { url: '/pedidos' });
    const el = raiz(h);
    porTexto(el, 'button', 'Carla Admin').click();
    await estable(h);
    const panel = TestBed.inject(OverlayContainer).getContainerElement();
    const opciones = textos(panel, '[role="menuitem"]');
    expect(opciones).toEqual(['Mi perfil', 'Cambiar contraseña', 'Salir']);
    expect(porTexto(panel, '[role="menuitem"]', 'Mi perfil').getAttribute('href')).toBe('/perfil');
    expect(porTexto(panel, '[role="menuitem"]', 'Cambiar contraseña').getAttribute('href')).toBe(
      '/perfil',
    );
  });

  it('CA-0b-24 el nombre del menú de usuario sale de /staff/me y, si falla, de la sesión', async () => {
    const h = await montar(SESION_ADMIN, { url: '/pedidos' });
    const ctrl = TestBed.inject(HttpTestingController);
    expect(raiz(h).textContent).toContain('Carla Admin');
    ctrl.expectOne('/api/v1/staff/me').flush({
      success: true,
      mensaje: 'Operación exitosa',
      data: {
        id: 'x',
        email: 'a@fitstore.pe',
        nombres: 'Lucía',
        apellidos: 'Quispe',
        rol: 'ADMIN',
        estado: 'ACTIVO',
        version: 1,
      },
    });
    await estable(h);
    expect(raiz(h).textContent).toContain('Lucía Quispe');
    expect(porTexto(raiz(h), 'button', 'Lucía Quispe').textContent).toContain('LQ');
  });

  it('CA-0b-24 salir cierra la sesión (aunque el logout falle) y lleva a /ingresar', async () => {
    const h = await montar(SESION_ADMIN, { url: '/pedidos' });
    const ctrl = TestBed.inject(HttpTestingController);
    porTexto(raiz(h), 'button', 'Carla Admin').click();
    await estable(h);
    porTexto(
      TestBed.inject(OverlayContainer).getContainerElement(),
      '[role="menuitem"]',
      'Salir',
    ).click();
    await estable(h);
    ctrl.expectOne('/api/v1/auth/logout').flush(null, { status: 500, statusText: 'Error' });
    await estable(h);
    expect(TestBed.inject(SesionStore).autenticado()).toBe(false);
    expect(TestBed.inject(Router).url).toBe('/ingresar');
  });

  it('CA-0b-24 en escritorio el menú está fijo; en tablet (800 px) se oculta y el botón ☰ lo abre', async () => {
    const escritorio = await montar(SESION_ADMIN, { url: '/pedidos', ancho: 1280 });
    await estable(escritorio);
    expect(
      raiz(escritorio).querySelector('mat-sidenav')?.classList.contains('mat-drawer-side'),
    ).toBe(true);
    expect(raiz(escritorio).querySelector('button[aria-label="Abrir menú"]')).toBeNull();
    TestBed.resetTestingModule();

    const tablet = await montar(SESION_ADMIN, { url: '/pedidos', ancho: 800 });
    await estable(tablet);
    const lateral = raiz(tablet).querySelector('mat-sidenav') as HTMLElement;
    expect(lateral.classList.contains('mat-drawer-over')).toBe(true);
    expect(lateral.classList.contains('mat-drawer-opened')).toBe(false);
    (raiz(tablet).querySelector('button[aria-label="Abrir menú"]') as HTMLElement).click();
    await estable(tablet);
    expect(lateral.classList.contains('mat-drawer-opened')).toBe(true);
  });

  it('CA-0b-08 el menú muestra el logo y el nombre de la tienda de settings', async () => {
    localStorage.setItem(
      CLAVE_RESPALDO_CONFIG,
      JSON.stringify({
        identidad: {
          nombre: 'Kallpa',
          logoClaroUrl: '/media/tienda/logo-claro.webp',
          logoOscuroUrl: null,
        },
        tema: {
          colorPrimario: '#B3245C',
          colorSecundario: '#22182E',
          colorAcento: '#A3E635',
          colorFondo: '#FFFFFF',
          colorTexto: '#22182E',
        },
      }),
    );
    const h = await montar(SESION_ADMIN, { url: '/pedidos' });
    void TestBed.inject(TemaService).iniciar();
    await estable(h);
    const logo = menu(raiz(h)).querySelector('img') as HTMLImageElement;
    expect(logo.getAttribute('src')).toBe('/media/tienda/logo-claro.webp');
    expect(logo.getAttribute('alt')).toBe('Kallpa');
  });

  it('CA-0b-08 sin logo, el menú escribe el nombre de la tienda', async () => {
    const h = await montar(SESION_ADMIN, { url: '/pedidos' });
    expect(menu(raiz(h)).querySelector('img')).toBeNull();
    expect(menu(raiz(h)).textContent).toContain('FitStore');
  });
});
