import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { readFileSync } from 'node:fs';
import {
  CarritoContador,
  EstadoServicio,
  TemaService,
  ToastService,
  provideFitstoreHttp,
  provideSesionConfig,
} from 'shared';
import type { settingsApi } from 'shared';
import { crearRutas } from '../app.routes';
import { BarraAnuncio } from './barra-anuncio';
import { conPantallasDePrueba, montar } from '../testing';

const estilos = readFileSync(`${process.cwd()}/projects/storefront/src/styles.css`, 'utf8');
const ok = <T>(data: T) => ({ success: true, mensaje: 'Operación exitosa', data });

const nodo = (id: number, nombre: string, slug: string, hijas = 0) => ({
  id,
  nombre,
  slug,
  hijas: Array.from({ length: hijas }, (_, i) => ({
    id: id * 100 + i,
    nombre: `Sub ${nombre} ${i}`,
    slug: `${slug}-${i}`,
  })),
});
const CATEGORIAS = [
  nodo(1, 'Nueva colección', 'nueva-coleccion'),
  nodo(2, 'Leggings', 'leggings', 2),
  nodo(3, 'Tops', 'tops'),
  nodo(4, 'Casacas', 'casacas'),
  nodo(5, 'Shorts', 'shorts'),
  nodo(6, 'Accesorios', 'accesorios'),
];

const CONFIG = {
  identidad: {
    nombre: 'FitStore',
    eslogan: 'Ropa deportiva hecha en Gamarra.',
    razonSocial: 'Moda Activa S.A.C.',
    ruc: '20123456789',
    direccionFiscal: 'Jr. Gamarra 700',
    logoClaroUrl: 'https://cdn.fitstore.pe/logo-claro.png',
  },
  tema: {
    colorPrimario: '#C9B28C',
    colorSecundario: '#2B2621',
    colorAcento: '#4E5B45',
    colorFondo: '#FAF9F7',
    colorTexto: '#26221D',
  },
  contenido: {
    heroTitulo: 'Muévete',
    beneficios: [],
    email: 'ventas@tienda.pe',
    telefono: '014567890',
    whatsapp: '987 654 321',
    horario: 'Lun–Sáb 9:00–19:00',
    instagramUrl: 'https://instagram.com/fitstore',
    facebookUrl: 'https://facebook.com/fitstore',
    tiktokUrl: null,
    tiendaFisica: {
      direccion: 'Galería Gamarra, Jr. Gamarra 700, tienda 210',
      ubigeo: '150132',
      ubigeoNombre: 'La Victoria, Lima, Lima',
    },
    imagenesLanding: [],
  },
  parametros: {
    metodosPago: ['CULQI', 'MERCADO_PAGO'],
    modalidadesEntrega: [],
    expiracionPedidoMinutos: 30,
    diasReembolsoPostventa: 7,
    moneda: 'PEN',
  },
  politicas: { terminos: 1, privacidad: 1, cambiosDevoluciones: 1, envios: 1 },
} as unknown as settingsApi.ConfiguracionPublica;

interface Opciones {
  url?: string;
  categorias?: unknown[];
  falloCategorias?: boolean;
  config?: settingsApi.ConfiguracionPublica | null;
}

async function abrir({
  // Una ruta sin pantalla propia (la 404 dentro del shell): las pantallas de las specs no interfieren con el shell.
  url = '/prueba-del-shell',
  categorias = CATEGORIAS,
  falloCategorias = false,
  config = CONFIG,
}: Opciones = {}) {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideFitstoreHttp({ carrito: true }),
      provideHttpClientTesting(),
      provideRouter(conPantallasDePrueba(crearRutas(true))),
      provideSesionConfig({ tipo: 'CLIENTE', rutaLogin: '/ingresar', rutaSinPermiso: '/403' }),
    ],
  });
  localStorage.clear();
  const ctrl = TestBed.inject(HttpTestingController);
  if (config) {
    const carga = TestBed.inject(TemaService).iniciar();
    ctrl.expectOne('/api/v1/settings/public').flush(ok(config));
    await carga;
  }
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  // La cabecera pide las categorías; una pantalla de las specs puede pedirlas también: se responden todas igual.
  const peticiones = ctrl.match((r) => r.url === '/api/v1/categorias');
  expect(peticiones.length).toBeGreaterThan(0);
  for (const peticion of peticiones) {
    if (falloCategorias) {
      peticion.flush(
        { success: false, mensaje: 'Error interno', errores: [] },
        { status: 500, statusText: 'Server Error' },
      );
    } else {
      peticion.flush(ok(categorias));
    }
  }
  const estabilizar = async () => {
    harness.fixture.detectChanges();
    await harness.fixture.whenStable();
  };
  await estabilizar();
  const el = harness.routeNativeElement as HTMLElement;
  return { harness, ctrl, el, estabilizar, router: TestBed.inject(Router) };
}

const enlacesNav = (el: HTMLElement) => [
  ...el.querySelectorAll<HTMLAnchorElement>('nav[aria-label="Categorías"] a'),
];
const escribir = async (el: HTMLElement, texto: string, tecla = 'Enter') => {
  const campo = el.querySelector('input[type="search"]') as HTMLInputElement;
  campo.value = texto;
  campo.dispatchEvent(new Event('input'));
  campo.dispatchEvent(new KeyboardEvent('keydown', { key: tecla, bubbles: true }));
};

describe('TiendaShell: cabecera', () => {
  it('CA-0b-20 muestra logo, categorías, buscador, cuenta y carrito con contador', async () => {
    const { el } = await abrir();
    expect(el.querySelector('header a[href="/"] img')!.getAttribute('src')).toBe(
      'https://cdn.fitstore.pe/logo-claro.png',
    );
    expect(enlacesNav(el).length).toBeGreaterThan(0);
    expect(el.querySelector('header input[type="search"]')).not.toBeNull();
    expect(el.querySelector('header a[aria-label="Mi cuenta"]')).not.toBeNull();
    expect(el.querySelector('header a[href="/carrito"]')).not.toBeNull();
  });

  it('CA-0b-20 muestra hasta 4 categorías raíz, en el orden de /categorias', async () => {
    const { el } = await abrir();
    const enlaces = enlacesNav(el);
    expect(enlaces.map((a) => a.textContent?.trim())).toEqual([
      'Nueva colección',
      'Leggings',
      'Tops',
      'Casacas',
    ]);
    expect(enlaces[1].getAttribute('href')).toBe('/categoria/leggings');
  });

  it('CA-0b-20 con menos de 4 categorías muestra solo esas', async () => {
    const { el } = await abrir({ categorias: CATEGORIAS.slice(0, 2) });
    expect(enlacesNav(el)).toHaveLength(2);
  });

  it('CA-0b-20 si /categorias falla, la cabecera se muestra sin categorías y sin bloquear nada', async () => {
    const { el } = await abrir({ falloCategorias: true });
    expect(enlacesNav(el)).toHaveLength(0);
    expect(el.querySelector('header input[type="search"]')).not.toBeNull();
    expect(el.querySelector('header a[href="/carrito"]')).not.toBeNull();
    expect(el.querySelector('main')).not.toBeNull();
    expect(el.querySelector('footer')).not.toBeNull();
    expect(el.querySelector('.fs-aviso')).toBeNull();
  });

  it('CA-0b-08 sin logo configurado muestra el nombre de la tienda', async () => {
    const sinLogo = { ...CONFIG, identidad: { ...CONFIG.identidad, logoClaroUrl: null } };
    const { el } = await abrir({ config: sinLogo });
    const logo = el.querySelector('header a[href="/"]')!;
    expect(logo.querySelector('img')).toBeNull();
    expect(logo.textContent).toContain('FitStore');
  });

  it('CA-0b-08 sin configuración (ni respaldo) usa el nombre por defecto', async () => {
    const { el } = await abrir({ config: null });
    expect(el.querySelector('header a[href="/"]')!.textContent).toContain('FitStore');
  });

  it('CA-0b-20c la categoría de la página actual queda marcada y las demás no', async () => {
    const { el } = await abrir({ url: '/categoria/leggings' });
    const actual = enlacesNav(el).filter((a) => a.getAttribute('aria-current') === 'page');
    expect(actual.map((a) => a.textContent?.trim())).toEqual(['Leggings']);
  });

  it('CA-0b-20c fuera de una categoría ninguna queda marcada', async () => {
    const { el } = await abrir({ url: '/carrito' });
    expect(enlacesNav(el).some((a) => a.hasAttribute('aria-current'))).toBe(false);
  });

  it('CA-0b-20c al pasar el mouse o el foco las demás se atenúan al 40 % (regla del menú)', () => {
    expect(estilos).toMatch(/\.fs-nav:hover a,\s*\.fs-nav:focus-within a\s*\{\s*opacity:\s*0\.4/);
    expect(estilos).toMatch(/\.fs-nav a:hover,\s*\.fs-nav a:focus-visible\s*\{\s*opacity:\s*1/);
    expect(estilos).toMatch(/\.fs-nav a\[aria-current=["']page["']\]\s*\{\s*border-bottom-color/);
  });

  it('CA-0b-20d la cabecera usa el color secundario del tema, igual que la vista previa de Configuración', () => {
    const bloque = /\.fs-cabecera\s*\{([^}]*)\}/.exec(estilos)![1];
    expect(bloque).toMatch(/background:\s*var\(--fs-color-secundario\)/);
    expect(bloque).toMatch(/color:\s*var\(--fs-color-en-secundario\)/);
  });

  it('CA-0b-20d el logo, el menú, los íconos y el contador usan el color de texto sobre el secundario', () => {
    for (const selector of [
      '\\.fs-cabecera \\.fs-logo',
      '\\.fs-cabecera \\.fs-nav a',
      '\\.fs-cabecera \\.fs-icono-boton',
    ]) {
      const regla = new RegExp(`${selector}\\s*\\{([^}]*)\\}`).exec(estilos);
      expect(regla, selector).not.toBeNull();
      expect(regla![1]).toMatch(/color:\s*var\(--fs-color-en-secundario\)/);
    }
    const contador = /\.fs-cabecera \.fs-contador\s*\{([^}]*)\}/.exec(estilos)![1];
    expect(contador).toMatch(/background:\s*var\(--fs-color-en-secundario\)/);
    expect(contador).toMatch(/color:\s*var\(--fs-color-secundario\)/);
  });

  it('CA-0b-22 escribir en el buscador y presionar Enter navega a /productos?q=', async () => {
    const { el, estabilizar, router } = await abrir();
    await escribir(el, 'legging negro');
    await estabilizar();
    expect(router.url).toBe('/productos?q=legging%20negro');
  });

  it('CA-0b-22 un texto vacío no navega y las demás teclas tampoco', async () => {
    const { el, estabilizar, router } = await abrir({ url: '/carrito' });
    await escribir(el, '   ');
    await escribir(el, 'tops', 'a');
    await estabilizar();
    expect(router.url).toBe('/carrito');
  });

  it('CA-0b-23 el contador del carrito se actualiza sin recargar', async () => {
    const { el, estabilizar } = await abrir();
    const carrito = () => el.querySelector('header a[href="/carrito"]') as HTMLAnchorElement;
    expect(carrito().querySelector('[data-contador]')).toBeNull();
    expect(carrito().getAttribute('aria-label')).toBe('Carrito');
    TestBed.inject(CarritoContador).establecer(7);
    await estabilizar();
    expect(carrito().querySelector('[data-contador]')!.textContent?.trim()).toBe('7');
    expect(carrito().getAttribute('aria-label')).toBe('Carrito, 7 productos');
    TestBed.inject(CarritoContador).establecer(1);
    await estabilizar();
    expect(carrito().getAttribute('aria-label')).toBe('Carrito, 1 producto');
  });
});

describe('TiendaShell: menú móvil (390 px)', () => {
  it('CA-0b-21 el menú ☰ junto al logo está cerrado al inicio', async () => {
    const { el } = await abrir();
    const boton = el.querySelector('button[aria-label="Abrir menú"]') as HTMLButtonElement;
    expect(boton.classList).toContain('fs-solo-movil');
    expect(boton.getAttribute('aria-expanded')).toBe('false');
    expect(el.querySelector('.fs-menu-movil')).toBeNull();
    // El botón va antes del logo.
    const logo = el.querySelector('header a[href="/"]')!;
    expect(boton.compareDocumentPosition(logo) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('CA-0b-21 al abrirlo lista todas las categorías raíz (sin hijas) y la cuenta', async () => {
    const { el, estabilizar } = await abrir();
    (el.querySelector('button[aria-label="Abrir menú"]') as HTMLButtonElement).click();
    await estabilizar();
    const panel = el.querySelector('.fs-menu-movil')!;
    const textos = [...panel.querySelectorAll('a')].map((a) => a.textContent?.trim());
    expect(textos).toEqual(
      expect.arrayContaining([
        'Nueva colección',
        'Leggings',
        'Tops',
        'Casacas',
        'Shorts',
        'Accesorios',
        'Mi cuenta',
      ]),
    );
    expect(textos.some((t) => t?.startsWith('Sub '))).toBe(false);
    expect(
      (el.querySelector('button[aria-label="Abrir menú"]') as HTMLButtonElement).getAttribute(
        'aria-expanded',
      ),
    ).toBe('true');
  });

  it('CA-0b-21 elegir una categoría navega y cierra el menú', async () => {
    const { el, estabilizar, router } = await abrir();
    (el.querySelector('button[aria-label="Abrir menú"]') as HTMLButtonElement).click();
    await estabilizar();
    const enlace = [...el.querySelectorAll<HTMLAnchorElement>('.fs-menu-movil a')].find((a) =>
      a.textContent?.includes('Shorts'),
    )!;
    enlace.click();
    await estabilizar();
    await estabilizar();
    expect(router.url).toBe('/categoria/shorts');
    expect(el.querySelector('.fs-menu-movil')).toBeNull();
  });

  it('CA-0b-21 Esc y el botón de cerrar lo cierran', async () => {
    const { el, estabilizar } = await abrir();
    (el.querySelector('button[aria-label="Abrir menú"]') as HTMLButtonElement).click();
    await estabilizar();
    el.querySelector('.fs-menu-movil')!.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }),
    );
    await estabilizar();
    expect(el.querySelector('.fs-menu-movil')).toBeNull();
    (el.querySelector('button[aria-label="Abrir menú"]') as HTMLButtonElement).click();
    await estabilizar();
    (
      el.querySelector('.fs-menu-movil button[aria-label="Cerrar menú"]') as HTMLButtonElement
    ).click();
    await estabilizar();
    expect(el.querySelector('.fs-menu-movil')).toBeNull();
  });

  it('CA-0b-21 en móvil las categorías y la cuenta salen de la barra y no hay scroll horizontal', async () => {
    const { el } = await abrir();
    expect(el.querySelector('nav[aria-label="Categorías"]')!.classList).toContain(
      'fs-solo-escritorio',
    );
    expect(el.querySelector('header a[aria-label="Mi cuenta"]')!.classList).toContain(
      'fs-solo-escritorio',
    );
    expect(estilos).toMatch(
      /@media \(max-width: 720px\)[\s\S]*\.fs-solo-escritorio\s*\{\s*display:\s*none/,
    );
    expect(estilos).toMatch(/\.fs-menu-movil__panel\s*\{[^}]*overflow-x:\s*hidden/);
    expect(estilos).toMatch(/\.fs-pagina\s*\{[^}]*box-sizing:\s*border-box/);
  });
});

describe('TiendaShell: pie y contenido', () => {
  it('CA-0b-20 el pie trae contacto, políticas, redes y el Libro de Reclamaciones', async () => {
    const { el } = await abrir();
    const pie = el.querySelector('footer')!;
    expect(pie.textContent).toContain('987 654 321');
    expect(pie.textContent).toContain('ventas@tienda.pe');
    expect(pie.textContent).toContain('Lun–Sáb 9:00–19:00');
    expect(pie.textContent).toContain('Galería Gamarra, Jr. Gamarra 700, tienda 210');
    expect(pie.textContent).toContain('Moda Activa S.A.C.');
    expect(pie.textContent).toContain('20123456789');
    expect(pie.querySelector('a[href="https://instagram.com/fitstore"]')).not.toBeNull();
    expect(pie.querySelector('a[href="https://facebook.com/fitstore"]')).not.toBeNull();
    expect(pie.querySelector('a[href*="tiktok"]')).toBeNull();
    const libro = [...pie.querySelectorAll('a')].find((a) =>
      a.textContent?.includes('Libro de Reclamaciones'),
    )!;
    expect(libro.getAttribute('href')).toBe('/libro-de-reclamaciones');
    const politicas = [...pie.querySelectorAll<HTMLAnchorElement>('a[href^="/politicas/"]')].map(
      (a) => a.getAttribute('href'),
    );
    expect(politicas).toEqual([
      '/politicas/envios',
      '/politicas/cambios-devoluciones',
      '/politicas/terminos',
      '/politicas/privacidad',
    ]);
    expect(pie.textContent).not.toContain('Guía de tallas');
  });

  it('CA-0b-20 el Libro de Reclamaciones está siempre, aunque no haya configuración', async () => {
    const { el } = await abrir({ config: null });
    const libro = [...el.querySelectorAll('footer a')].find((a) =>
      a.textContent?.includes('Libro de Reclamaciones'),
    );
    expect(libro?.getAttribute('href')).toBe('/libro-de-reclamaciones');
  });

  it('CA-0b-20 la leyenda de pagos se arma con los medios habilitados', async () => {
    const { el } = await abrir();
    expect(el.querySelector('footer')!.textContent).toContain(
      'Pagos seguros con Culqi y Mercado Pago',
    );
  });

  it('CA-0b-20 sin medios de pago la leyenda se omite', async () => {
    const sin = {
      ...CONFIG,
      parametros: { ...CONFIG.parametros, metodosPago: [] },
    } as unknown as settingsApi.ConfiguracionPublica;
    const { el } = await abrir({ config: sin });
    expect(el.querySelector('footer')!.textContent).not.toContain('Pagos seguros');
  });

  it('CA-0b-20b el contenido ocupa al menos el alto de la ventana menos la cabecera', async () => {
    const { el } = await abrir();
    expect(el.querySelector('main')!.classList).toContain('fs-contenido');
    expect(estilos).toMatch(/\.fs-contenido\s*\{[^}]*min-height:\s*calc\(100dvh - 80px\)/);
    expect(estilos).toMatch(
      /@media \(max-width: 720px\)[\s\S]*\.fs-contenido\s*\{[^}]*min-height:\s*calc\(100dvh - 64px\)/,
    );
  });

  it('CA-0b-20b el pie va después del contenido, al final del documento', async () => {
    const { el } = await abrir();
    const main = el.querySelector('main')!;
    const pie = el.querySelector('footer')!;
    expect(main.compareDocumentPosition(pie) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(pie.nextElementSibling === null || pie.nextElementSibling.tagName !== 'MAIN').toBe(true);
    expect(pie.querySelector('main')).toBeNull();
  });

  it('CA-0b-20b el pie nunca es fijo ni pegajoso', async () => {
    const { el } = await abrir();
    const pie = el.querySelector('footer') as HTMLElement;
    for (const nodo of [pie, pie.parentElement as HTMLElement]) {
      expect(['fixed', 'sticky']).not.toContain(getComputedStyle(nodo).position);
      expect(nodo.getAttribute('style') ?? '').not.toMatch(/fixed|sticky/);
    }
    expect(estilos).not.toMatch(/\.fs-pie[^{]*\{[^}]*position:\s*(fixed|sticky)/);
    expect(estilos).not.toMatch(/footer[^{]*\{[^}]*position:\s*(fixed|sticky)/);
  });

  it('CA-0b-20b también en una página corta como la 404', async () => {
    const { el } = await abrir({ url: '/ruta-que-no-existe' });
    expect(el.querySelector('main')!.textContent).toContain('Página no encontrada');
    expect(el.querySelector('main')!.classList).toContain('fs-contenido');
    expect(el.querySelector('footer')).not.toBeNull();
  });

  it('CA-0b-19 la 404 conserva la cabecera y el pie de la tienda', async () => {
    const { el } = await abrir({ url: '/ruta-que-no-existe' });
    expect(el.querySelector('header')).not.toBeNull();
    expect(el.querySelector('footer')).not.toBeNull();
    expect(el.textContent).toContain('Volver al inicio');
  });
});

describe('TiendaShell: avisos y banner', () => {
  it('CA-0b-13 un error general se ve en un toast sin romper la página', async () => {
    const { el, estabilizar } = await abrir();
    TestBed.inject(ToastService).mostrar('error', 'Sin conexión');
    await estabilizar();
    expect(el.querySelector('.fs-aviso')!.textContent).toContain('Sin conexión');
    expect(el.querySelector('main')).not.toBeNull();
  });

  it('CA-0b-13 un 503 enciende el banner "Servicio no disponible" y el resto sigue', async () => {
    const { el, estabilizar } = await abrir();
    TestBed.inject(EstadoServicio).noDisponible.set(true);
    await estabilizar();
    expect(el.querySelector('[role="alert"]')!.textContent).toContain('Servicio no disponible');
    expect(el.querySelector('header')).not.toBeNull();
    expect(el.querySelector('main')).not.toBeNull();
    expect(el.querySelector('footer')).not.toBeNull();
  });
});

describe('Barra de anuncio', () => {
  it('CA-0b-20 con texto lo muestra', async () => {
    const { el } = await montar(BarraAnuncio, { texto: 'Envío gratis desde S/ 150' });
    expect(el.textContent).toContain('Envío gratis desde S/ 150');
  });

  it('CA-0b-20 sin texto no se pinta nada', async () => {
    const { el } = await montar(BarraAnuncio);
    expect(el.querySelector('.fs-anuncio')).toBeNull();
  });

  it('CA-0b-20 el shell no la muestra mientras ningún dato defina el anuncio', async () => {
    const { el } = await abrir();
    expect(el.querySelector('.fs-anuncio')).toBeNull();
  });
});
