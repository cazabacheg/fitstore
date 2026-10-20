import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApplicationInitStatus } from '@angular/core';
import { provideFitstoreHttp } from '../http/proveedores';
import { ToastService } from '../http/toast.service';
import { PALETA_FITSTORE } from '../testing/paletas';
import { CLAVE_RESPALDO_CONFIG, TemaService } from './tema.service';
import { provideTema } from './proveedores';

const RUTA = '/api/v1/settings/public';

function configuracion(colorPrimario: string, extra: Record<string, unknown> = {}) {
  return {
    success: true,
    mensaje: 'Operación exitosa',
    data: {
      identidad: {
        nombre: 'Moda Activa',
        razonSocial: 'Moda Activa S.A.C.',
        ruc: '20123456789',
        direccionFiscal: 'Jr. Gamarra 123, La Victoria',
        logoClaroUrl: '/media/tienda/logo-claro.webp',
        logoOscuroUrl: '/media/tienda/logo-oscuro.webp',
        faviconUrl: '/media/tienda/favicon.png',
        ...extra,
      },
      tema: {
        colorPrimario,
        colorSecundario: '#111827',
        colorAcento: '#F59E0B',
        colorFondo: '#FFFFFF',
        colorTexto: '#111827',
      },
      contenido: {},
      parametros: {},
      politicas: {},
    },
  };
}

function variable(nombre: string): string {
  return document.documentElement.style.getPropertyValue(nombre);
}

function configurar() {
  TestBed.configureTestingModule({
    providers: [provideFitstoreHttp({ carrito: false }), provideHttpClientTesting()],
  });
  return { servicio: TestBed.inject(TemaService), ctrl: TestBed.inject(HttpTestingController) };
}

describe('TemaService', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('style');
    document.title = 'FitStore';
    document.head.querySelectorAll('link[rel~="icon"]').forEach((l) => l.remove());
  });

  afterEach(() => vi.useRealTimers());

  it('CA-0b-05 si settings devuelve #E11D48, escribe el primario y sus derivados como variables CSS en :root', async () => {
    const { servicio, ctrl } = configurar();

    const listo = servicio.iniciar();
    ctrl.expectOne(RUTA).flush(configuracion('#E11D48'));
    await listo;

    expect(variable('--fs-color-primario')).toBe('#E11D48');
    expect(variable('--fs-color-secundario')).toBe('#111827');
    expect(variable('--fs-color-acento')).toBe('#F59E0B');
    expect(variable('--fs-color-fondo')).toBe('#FFFFFF');
    expect(variable('--fs-color-texto')).toBe('#111827');
    for (const derivada of [
      'en-primario',
      'en-secundario',
      'en-acento',
      'primario-texto',
      'primario-suave',
      'superficie-alterna',
      'texto-suave',
      'linea',
      'borde-control',
    ]) {
      expect(variable(`--fs-color-${derivada}`), derivada).toMatch(/^#[0-9A-F]{6}$/);
    }
  });

  it('CA-0b-06 al recargar con el color cambiado en settings, se ve el color nuevo (no el guardado)', async () => {
    const primera = configurar();
    const l1 = primera.servicio.iniciar();
    primera.ctrl.expectOne(RUTA).flush(configuracion('#E11D48'));
    await l1;
    expect(variable('--fs-color-primario')).toBe('#E11D48');

    // "Recargar": otra instancia de la app, con el respaldo de la visita anterior en localStorage.
    TestBed.resetTestingModule();
    const segunda = configurar();
    const l2 = segunda.servicio.iniciar();
    expect(variable('--fs-color-primario')).toBe('#E11D48'); // mientras responde, usa el respaldo
    segunda.ctrl.expectOne(RUTA).flush(configuracion('#0EA5E9'));
    await l2;

    expect(variable('--fs-color-primario')).toBe('#0EA5E9');
    expect(JSON.parse(localStorage.getItem(CLAVE_RESPALDO_CONFIG) ?? '{}').tema.colorPrimario).toBe(
      '#0EA5E9',
    );
  });

  it('CA-0b-07 si /settings/public no responde, usa la última configuración guardada en el navegador', async () => {
    localStorage.setItem(CLAVE_RESPALDO_CONFIG, JSON.stringify(configuracion('#E11D48').data));
    const { servicio, ctrl } = configurar();

    const listo = servicio.iniciar();
    ctrl
      .expectOne(RUTA)
      .flush(
        { success: false, mensaje: 'Servicio no disponible', codigo: 'SERVICIO_NO_DISPONIBLE' },
        { status: 503, statusText: 'Service Unavailable' },
      );
    await listo;

    expect(variable('--fs-color-primario')).toBe('#E11D48');
    expect(servicio.nombre()).toBe('Moda Activa');
  });

  it('CA-0b-07 sin respaldo y sin respuesta, usa el tema FitStore por defecto (sin toast de error)', async () => {
    const { servicio, ctrl } = configurar();

    const listo = servicio.iniciar();
    ctrl.expectOne(RUTA).error(new ProgressEvent('error'));
    await listo;

    expect(variable('--fs-color-primario')).toBe(PALETA_FITSTORE.entrada.colorPrimario);
    expect(variable('--fs-color-primario-texto')).toBe(PALETA_FITSTORE.derivados.primarioTexto);
    expect(variable('--fs-color-linea')).toBe(PALETA_FITSTORE.derivados.linea);
    expect(TestBed.inject(ToastService).toasts()).toHaveLength(0);
  });

  it('CA-0b-07 un error del servidor tampoco muestra un toast por la configuración', async () => {
    const { servicio, ctrl } = configurar();

    const listo = servicio.iniciar();
    ctrl
      .expectOne(RUTA)
      .flush(
        { success: false, mensaje: 'Error interno', codigo: 'ERROR_INTERNO' },
        { status: 500, statusText: 'Internal Server Error' },
      );
    await listo;

    expect(TestBed.inject(ToastService).toasts()).toHaveLength(0);
  });

  it('CA-0b-07 no bloquea la carga: aplica el tema por defecto de inmediato y no espera más de unos segundos', async () => {
    vi.useFakeTimers();
    const { servicio, ctrl } = configurar();
    let terminado = false;

    const listo = servicio.iniciar().then(() => (terminado = true));
    expect(variable('--fs-color-primario')).toBe(PALETA_FITSTORE.entrada.colorPrimario);
    await vi.advanceTimersByTimeAsync(100);
    expect(terminado).toBe(false);

    await vi.advanceTimersByTimeAsync(5000); // settings nunca respondió
    await listo;
    expect(terminado).toBe(true);

    // Si la respuesta llega tarde, igual se aplica.
    ctrl.expectOne(RUTA).flush(configuracion('#E11D48'));
    expect(variable('--fs-color-primario')).toBe('#E11D48');
  });

  it('CA-0b-08 el título de la pestaña, el favicon y el logo son los de la tienda', async () => {
    const { servicio, ctrl } = configurar();

    const listo = servicio.iniciar();
    ctrl.expectOne(RUTA).flush(configuracion('#E11D48'));
    await listo;

    expect(document.title).toBe('Moda Activa');
    expect(document.head.querySelector('link[rel~="icon"]')?.getAttribute('href')).toBe(
      '/media/tienda/favicon.png',
    );
    expect(servicio.nombre()).toBe('Moda Activa');
    expect(servicio.logoClaroUrl()).toBe('/media/tienda/logo-claro.webp');
    expect(servicio.logoOscuroUrl()).toBe('/media/tienda/logo-oscuro.webp');
  });

  it('CA-0b-08 si la tienda no subió favicon ni logo, conserva el favicon de la app y no hay logo', async () => {
    document.head.insertAdjacentHTML('beforeend', '<link rel="icon" href="favicon.ico">');
    const { servicio, ctrl } = configurar();

    const listo = servicio.iniciar();
    ctrl
      .expectOne(RUTA)
      .flush(
        configuracion('#E11D48', { faviconUrl: null, logoClaroUrl: null, logoOscuroUrl: null }),
      );
    await listo;

    expect(document.head.querySelector('link[rel~="icon"]')?.getAttribute('href')).toBe(
      'favicon.ico',
    );
    expect(servicio.logoClaroUrl()).toBeNull();
    expect(document.title).toBe('Moda Activa');
  });

  it('CA-0b-08 el respaldo en localStorage no guarda tokens', async () => {
    const { servicio, ctrl } = configurar();
    const listo = servicio.iniciar();
    ctrl.expectOne(RUTA).flush(configuracion('#E11D48'));
    await listo;

    expect(Object.keys(localStorage)).toEqual([CLAVE_RESPALDO_CONFIG]);
    expect(localStorage.getItem(CLAVE_RESPALDO_CONFIG)).not.toMatch(/token/i);
  });

  it('CA-0b-07 un respaldo dañado en localStorage se ignora', async () => {
    localStorage.setItem(CLAVE_RESPALDO_CONFIG, '{no es json');
    const { servicio, ctrl } = configurar();

    const listo = servicio.iniciar();
    ctrl.expectOne(RUTA).error(new ProgressEvent('error'));
    await listo;

    expect(variable('--fs-color-primario')).toBe(PALETA_FITSTORE.entrada.colorPrimario);
  });

  it('CA-0b-06 provideTema carga la configuración al arrancar la app', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideFitstoreHttp({ carrito: false }),
        provideHttpClientTesting(),
        provideTema(),
      ],
    });
    const init = TestBed.inject(ApplicationInitStatus);
    TestBed.inject(HttpTestingController).expectOne(RUTA).flush(configuracion('#E11D48'));
    await init.donePromise;

    expect(variable('--fs-color-primario')).toBe('#E11D48');
  });
});
