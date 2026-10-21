import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { ConfiguracionPage } from './configuracion.page';
import { sinCambiosGuard } from './sin-cambios.guard';
import {
  configuracionAdmin,
  errorApi,
  escribir,
  montarComponente,
  porId,
  respuesta,
} from './datos-prueba';

const ADMIN = '/api/v1/admin/settings';

/** Monta la pantalla y responde la configuración y la lectura de inventario de catalog. */
async function abrir(config = configuracionAdmin()) {
  const montaje = await montarComponente(ConfiguracionPage);
  const ctrl = TestBed.inject(HttpTestingController);
  ctrl.expectOne(ADMIN).flush(respuesta(config));
  await montaje.estabilizar();
  ctrl
    .match('/api/v1/admin/inventario/configuracion')
    .forEach((r) => r.flush(respuesta({ stockMinimoPorDefecto: 5, margenReservaMinutos: 10 })));
  await montaje.estabilizar();
  return { ...montaje, ctrl };
}

const visible = (el: HTMLElement, id: string) => !porId(el, id).hasAttribute('hidden');

describe('ConfiguracionPage (P-3-04)', () => {
  it('CA-3-06 carga la configuración completa y muestra las pestañas', async () => {
    const { el } = await abrir();
    for (const id of ['identidad', 'tema', 'contenido', 'parametros', 'politicas']) {
      expect(el.querySelector(`[data-testid="pestana-${id}"]`), id).not.toBeNull();
    }
    expect(visible(el, 'panel-identidad')).toBe(true);
    expect(visible(el, 'panel-tema')).toBe(false);
    expect((porId(el, 'nombre') as HTMLInputElement).value).toBe('FitStore');
  });

  it('CA-3-09 cambiar de pestaña muestra su panel y conserva lo escrito en las otras', async () => {
    const { el, estabilizar } = await abrir();
    escribir(porId(el, 'nombre'), 'Moda Activa');
    porId<HTMLButtonElement>(el, 'pestana-tema').click();
    await estabilizar();
    expect(visible(el, 'panel-tema')).toBe(true);
    expect(visible(el, 'panel-identidad')).toBe(false);
    porId<HTMLButtonElement>(el, 'pestana-identidad').click();
    await estabilizar();
    expect((porId(el, 'nombre') as HTMLInputElement).value).toBe('Moda Activa');
  });

  it('CA-3-20 la pestaña Políticas lleva a la pantalla de políticas', async () => {
    const { el } = await abrir();
    expect(porId(el, 'pestana-politicas').getAttribute('href')).toBe('/configuracion/politicas');
  });

  it('CA-3-06 guardar la identidad envía la versión abierta y actualiza la versión con la respuesta', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    escribir(porId(el, 'nombre'), 'Moda Activa');
    await estabilizar();
    porId<HTMLButtonElement>(el, 'guardar-identidad').click();
    await estabilizar();

    const put = ctrl.expectOne(ADMIN + '/identidad');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toMatchObject({
      nombre: 'Moda Activa',
      ruc: '20123456789',
      version: 4,
    });
    put.flush(
      respuesta(
        configuracionAdmin({
          version: 5,
          identidad: { ...configuracionAdmin().identidad, nombre: 'Moda Activa' },
        }),
      ),
    );
    await estabilizar();

    escribir(porId(el, 'nombre'), 'Moda Activa Perú');
    await estabilizar();
    porId<HTMLButtonElement>(el, 'guardar-identidad').click();
    await estabilizar();
    expect(ctrl.expectOne(ADMIN + '/identidad').request.body.version).toBe(5);
  });

  it('CA-3-08 si otro admin guardó antes, avisa con el mensaje del servidor y no pisa su cambio', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    escribir(porId(el, 'nombre'), 'Mi cambio');
    await estabilizar();
    porId<HTMLButtonElement>(el, 'guardar-identidad').click();
    await estabilizar();
    ctrl
      .expectOne(ADMIN + '/identidad')
      .flush(
        errorApi(
          'CONFLICTO_VERSION',
          'Otro administrador modificó la configuración; recarga la página',
        ),
        { status: 409, statusText: 'Conflict' },
      );
    await estabilizar();

    expect(porId(el, 'conflicto').textContent).toContain(
      'Otro administrador modificó la configuración; recarga la página',
    );
    porId<HTMLButtonElement>(el, 'recargar').click();
    await estabilizar();
    ctrl.expectOne(ADMIN).flush(respuesta(configuracionAdmin({ version: 6 })));
    await estabilizar();
    expect(el.querySelector('[data-testid="conflicto"]')).toBeNull();
    expect((porId(el, 'nombre') as HTMLInputElement).value).toBe('FitStore');
  });

  it('CA-3-06 un 400 por RUC se muestra en el campo RUC', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    porId<HTMLButtonElement>(el, 'guardar-identidad').click();
    await estabilizar();
    ctrl
      .expectOne(ADMIN + '/identidad')
      .flush(
        errorApi('VALIDACION', 'Revisa los datos ingresados', [
          { campo: 'ruc', mensaje: 'El RUC debe tener 11 dígitos' },
        ]),
        { status: 400, statusText: 'Bad Request' },
      );
    await estabilizar();
    expect(porId(el, 'ruc').getAttribute('aria-invalid')).toBe('true');
    expect(el.textContent).toContain('El RUC debe tener 11 dígitos');
  });

  it('CA-3-11 guardar el tema envía los cinco colores con la versión y muestra las advertencias del servidor', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    porId<HTMLButtonElement>(el, 'pestana-tema').click();
    await estabilizar();
    escribir(porId(el, 'color-primario'), '#1E3A8A');
    await estabilizar();
    porId<HTMLButtonElement>(el, 'guardar-tema').click();
    await estabilizar();

    const put = ctrl.expectOne(ADMIN + '/tema');
    expect(put.request.body).toMatchObject({ colorPrimario: '#1E3A8A', version: 4 });
    put.flush(
      respuesta(
        configuracionAdmin({
          version: 5,
          tema: { ...configuracionAdmin().tema, colorPrimario: '#1E3A8A' },
          advertencias: ['Contraste texto/fondo 3.1:1 (recomendado ≥ 4.5:1)'],
        }),
      ),
    );
    await estabilizar();
    expect(el.textContent).toContain('Contraste texto/fondo 3.1:1');
  });

  it('CA-3-17 guardar los parámetros envía los medios como lista JSON con la versión', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    porId<HTMLButtonElement>(el, 'pestana-parametros').click();
    await estabilizar();
    porId<HTMLInputElement>(el, 'pago-MERCADO_PAGO').click();
    await estabilizar();
    porId<HTMLButtonElement>(el, 'guardar-parametros').click();
    await estabilizar();

    const put = ctrl.expectOne(ADMIN + '/parametros');
    expect(Array.isArray(put.request.body.metodosPago)).toBe(true);
    expect([...put.request.body.metodosPago].sort()).toEqual(['CULQI', 'YAPE']);
    expect(put.request.body.version).toBe(4);
    expect(put.request.body.correosAlertas).toEqual(['ventas@fitstore.pe']);
    put.flush(respuesta(configuracionAdmin({ version: 5 })));
    ctrl
      .match('/api/v1/admin/inventario/configuracion')
      .forEach((r) => r.flush(respuesta({ stockMinimoPorDefecto: 5, margenReservaMinutos: 10 })));
  });

  it('CA-3-13 guardar el contenido envía el hero y la versión', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    porId<HTMLButtonElement>(el, 'pestana-contenido').click();
    await estabilizar();
    escribir(porId(el, 'hero-titulo'), 'Nueva colección');
    await estabilizar();
    porId<HTMLButtonElement>(el, 'guardar-contenido').click();
    await estabilizar();
    const put = ctrl.expectOne(ADMIN + '/contenido');
    expect(put.request.body).toMatchObject({ heroTitulo: 'Nueva colección', version: 4 });
    expect(put.request.body.imagenesLanding).toBeUndefined();
  });

  it('CA-3-07 subir un logo manda la imagen como multipart y muestra el nuevo logo', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    const archivo = new File([new Uint8Array(100)], 'logo.png', { type: 'image/png' });
    const entrada = porId<HTMLInputElement>(el, 'archivo-logo-claro');
    Object.defineProperty(entrada, 'files', { value: [archivo], configurable: true });
    entrada.dispatchEvent(new Event('change', { bubbles: true }));
    await estabilizar();

    const post = ctrl.expectOne(ADMIN + '/imagenes');
    expect((post.request.body as FormData).get('tipo')).toBe('LOGO_CLARO');
    post.flush(
      respuesta({
        id: 1,
        tipo: 'LOGO_CLARO',
        url: '/media/tienda/logo_claro/a.png',
        orden: 0,
        textoAlternativo: null,
        enlace: null,
      }),
      { status: 201, statusText: 'Created' },
    );
    await estabilizar();
    expect(porId(el, 'logo-claro-actual').getAttribute('src')).toBe(
      '/media/tienda/logo_claro/a.png',
    );
  });

  it('CA-3-12 avisa de los cambios sin guardar al salir de la pantalla', async () => {
    const { el, estabilizar, comp } = await abrir();
    const evento = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(evento);
    expect(evento.defaultPrevented).toBe(false);

    escribir(porId(el, 'nombre'), 'Cambio sin guardar');
    await estabilizar();
    expect((comp as ConfiguracionPage).haySinGuardar()).toBe(true);
    const evento2 = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(evento2);
    expect(evento2.defaultPrevented).toBe(true);
  });

  it('CA-3-12 la guarda de salida pregunta solo si hay cambios sin guardar', async () => {
    const confirmar = vi.fn();
    const limpio = { haySinGuardar: () => false };
    const sucio = { haySinGuardar: () => true };
    expect(
      TestBed.runInInjectionContext(() =>
        sinCambiosGuard(limpio as never, null as never, null as never, null as never),
      ),
    ).toBe(true);
    void confirmar;
    const resultado = TestBed.runInInjectionContext(() =>
      sinCambiosGuard(sucio as never, null as never, null as never, null as never),
    );
    expect(resultado).not.toBe(true);
  });

  it('CA-3-06 si la carga falla muestra el error con "Reintentar"', async () => {
    const montaje = await montarComponente(ConfiguracionPage);
    const ctrl = TestBed.inject(HttpTestingController);
    ctrl
      .expectOne(ADMIN)
      .flush(errorApi('ERROR_INTERNO', 'Error'), { status: 500, statusText: 'x' });
    await montaje.estabilizar();
    expect(montaje.el.querySelector('fs-estado-error')).not.toBeNull();
    (montaje.el.querySelector('fs-estado-error button') as HTMLButtonElement).click();
    await montaje.estabilizar();
    ctrl.expectOne(ADMIN).flush(respuesta(configuracionAdmin()));
  });
});
