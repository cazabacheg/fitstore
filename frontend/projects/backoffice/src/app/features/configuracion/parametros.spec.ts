import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  configuracionAdmin,
  errorApi,
  escribir,
  montarComponente,
  porId,
  respuesta,
} from './datos-prueba';
import { ParametrosTab, type DatosParametros } from './parametros-tab';

const CONFIG = configuracionAdmin();
const URL_INVENTARIO = '/api/v1/admin/inventario/configuracion';

/** Monta la pestaña y responde la lectura de inventario de catalog (o la hace fallar). */
async function montar(opciones: { catalogCaido?: boolean; extra?: Record<string, unknown> } = {}) {
  const montaje = await montarComponente(ParametrosTab, {
    parametros: CONFIG.parametros,
    correosAlertas: CONFIG.correosAlertas,
    ...opciones.extra,
  });
  const ctrl = TestBed.inject(HttpTestingController);
  const lectura = ctrl.expectOne(URL_INVENTARIO);
  if (opciones.catalogCaido) {
    lectura.flush(errorApi('SERVICIO_NO_DISPONIBLE', 'No disponible'), {
      status: 503,
      statusText: 'x',
    });
  } else {
    lectura.flush(respuesta({ stockMinimoPorDefecto: 5, margenReservaMinutos: 10 }));
  }
  await montaje.estabilizar();
  return { ...montaje, ctrl };
}

function emisiones(comp: ParametrosTab): DatosParametros[] {
  const lista: DatosParametros[] = [];
  comp.guardar.subscribe((d) => lista.push(d));
  return lista;
}

const clic = (el: HTMLElement, id: string) => porId<HTMLButtonElement>(el, id).click();

describe('ParametrosTab (P-3-04, pestaña Parámetros)', () => {
  it('CA-3-17 muestra los medios de pago, las modalidades, la expiración, el reembolso y los correos guardados', async () => {
    const { el } = await montar();
    expect((porId(el, 'pago-CULQI') as HTMLInputElement).checked).toBe(true);
    expect((porId(el, 'pago-YAPE') as HTMLInputElement).checked).toBe(true);
    expect((porId(el, 'entrega-RECOJO') as HTMLInputElement).checked).toBe(true);
    expect((porId(el, 'expiracion') as HTMLInputElement).value).toBe('30');
    expect((porId(el, 'reembolso') as HTMLInputElement).value).toBe('30');
    expect((el.querySelector('[data-testid="correo-alerta"]') as HTMLInputElement).value).toBe(
      'ventas@fitstore.pe',
    );
  });

  it('CA-3-17 la moneda se muestra como PEN y no se puede cambiar', async () => {
    const { el } = await montar();
    const moneda = porId<HTMLInputElement>(el, 'moneda');
    expect(moneda.value).toBe('PEN');
    expect(moneda.readOnly || moneda.disabled).toBe(true);
  });

  it('CA-3-17 el reembolso explica que 0 significa sin límite', async () => {
    const { el } = await montar();
    expect(el.textContent).toContain('0 = sin límite');
  });

  it('CA-3-17 guardar emite los parámetros editados', async () => {
    const { el, estabilizar, comp } = await montar();
    const guardadas = emisiones(comp as ParametrosTab);
    porId<HTMLInputElement>(el, 'pago-MERCADO_PAGO').click();
    porId<HTMLInputElement>(el, 'entrega-AGENCIA').click();
    escribir(porId(el, 'expiracion'), '45');
    escribir(porId(el, 'reembolso'), '0');
    await estabilizar();
    clic(el, 'guardar-parametros');
    await estabilizar();

    expect(guardadas).toHaveLength(1);
    expect([...guardadas[0].metodosPago].sort()).toEqual(['CULQI', 'YAPE']);
    expect([...guardadas[0].modalidadesEntrega].sort()).toEqual(['DELIVERY_LOCAL', 'RECOJO']);
    expect(guardadas[0].expiracionPedidoMinutos).toBe(45);
    expect(guardadas[0].diasReembolsoPostventa).toBe(0);
    expect(guardadas[0].correosAlertas).toEqual(['ventas@fitstore.pe']);
  });

  it('CA-3-17 debe haber al menos un medio de pago y una modalidad de entrega', async () => {
    const { el, estabilizar, comp } = await montar();
    const guardadas = emisiones(comp as ParametrosTab);
    for (const id of ['pago-CULQI', 'pago-YAPE', 'pago-MERCADO_PAGO'])
      porId<HTMLInputElement>(el, id).click();
    await estabilizar();
    clic(el, 'guardar-parametros');
    await estabilizar();
    expect(el.textContent).toContain('Habilita al menos un medio de pago');
    expect(guardadas).toEqual([]);

    for (const id of [
      'pago-CULQI',
      'entrega-DELIVERY_LOCAL',
      'entrega-AGENCIA',
      'entrega-RECOJO',
    ]) {
      porId<HTMLInputElement>(el, id).click();
    }
    await estabilizar();
    clic(el, 'guardar-parametros');
    await estabilizar();
    expect(el.textContent).toContain('Habilita al menos una modalidad de entrega');
    expect(guardadas).toEqual([]);
  });

  it('CA-3-17 la expiración del pedido va de 10 a 120 minutos', async () => {
    const { el, estabilizar, comp } = await montar();
    const guardadas = emisiones(comp as ParametrosTab);
    for (const valor of ['9', '121', '']) {
      escribir(porId(el, 'expiracion'), valor);
      await estabilizar();
      clic(el, 'guardar-parametros');
      await estabilizar();
      expect(porId(el, 'expiracion').getAttribute('aria-invalid'), valor).toBe('true');
    }
    expect(guardadas).toEqual([]);
    for (const valor of ['10', '120']) {
      escribir(porId(el, 'expiracion'), valor);
      await estabilizar();
      clic(el, 'guardar-parametros');
      await estabilizar();
    }
    expect(guardadas).toHaveLength(2);
  });

  it('CA-3-17 los días para reembolso van de 0 a 365', async () => {
    const { el, estabilizar, comp } = await montar();
    const guardadas = emisiones(comp as ParametrosTab);
    for (const valor of ['-1', '366']) {
      escribir(porId(el, 'reembolso'), valor);
      await estabilizar();
      clic(el, 'guardar-parametros');
      await estabilizar();
      expect(porId(el, 'reembolso').getAttribute('aria-invalid'), valor).toBe('true');
    }
    expect(guardadas).toEqual([]);
  });

  it('CA-3-17 los correos para alertas son de 1 a 5 y válidos', async () => {
    const { el, estabilizar, comp } = await montar();
    const guardadas = emisiones(comp as ParametrosTab);
    escribir(el.querySelector('[data-testid="correo-alerta"]'), 'no-es-correo');
    await estabilizar();
    clic(el, 'guardar-parametros');
    await estabilizar();
    expect(el.querySelector('[data-testid="correo-alerta"]')!.getAttribute('aria-invalid')).toBe(
      'true',
    );
    expect(guardadas).toEqual([]);

    escribir(el.querySelector('[data-testid="correo-alerta"]'), 'ventas@fitstore.pe');
    for (let i = 0; i < 4; i++) {
      clic(el, 'agregar-correo');
      await estabilizar();
      const campos = el.querySelectorAll('[data-testid="correo-alerta"]');
      escribir(campos[campos.length - 1], `alerta${i}@fitstore.pe`);
      await estabilizar();
    }
    expect(el.querySelectorAll('[data-testid="correo-alerta"]').length).toBe(5);
    expect(porId<HTMLButtonElement>(el, 'agregar-correo').disabled).toBe(true);

    for (let i = 0; i < 4; i++) {
      (el.querySelector('[data-testid="quitar-correo"]') as HTMLButtonElement).click();
      await estabilizar();
    }
    expect(el.querySelectorAll('[data-testid="correo-alerta"]').length).toBe(1);
    expect(
      (el.querySelector('[data-testid="quitar-correo"]') as HTMLButtonElement | null)?.disabled ??
        true,
    ).toBe(true);
  });

  it('CA-3-17 los correos para alertas se avisan como internos: no se publican en la tienda', async () => {
    const { el } = await montar();
    expect(el.textContent).toContain('no se muestran en la tienda');
  });

  it('CA-3-38 el bloque Inventario trae el stock mínimo por defecto y el margen de reserva de catalog', async () => {
    const { el } = await montar();
    expect((porId(el, 'stock-minimo') as HTMLInputElement).value).toBe('5');
    expect((porId(el, 'margen-reserva') as HTMLInputElement).value).toBe('10');
  });

  it('CA-3-38 guardar envía el stock mínimo y el margen a catalog (PUT /admin/inventario/configuracion)', async () => {
    const { el, estabilizar, ctrl } = await montar();
    escribir(porId(el, 'stock-minimo'), '8');
    escribir(porId(el, 'margen-reserva'), '15');
    await estabilizar();
    clic(el, 'guardar-parametros');
    await estabilizar();

    const peticion = ctrl.expectOne(URL_INVENTARIO);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ stockMinimoPorDefecto: 8, margenReservaMinutos: 15 });
    peticion.flush(respuesta({ stockMinimoPorDefecto: 8, margenReservaMinutos: 15 }));
    await estabilizar();
    expect(porId(el, 'ok-inventario').textContent).toContain('Guardamos el inventario');
  });

  it('CA-3-38 el stock mínimo va de 0 a 999 y el margen de reserva de 0 a 30 minutos', async () => {
    const { el, estabilizar, comp, ctrl } = await montar();
    const guardadas = emisiones(comp as ParametrosTab);
    escribir(porId(el, 'stock-minimo'), '1000');
    escribir(porId(el, 'margen-reserva'), '31');
    await estabilizar();
    clic(el, 'guardar-parametros');
    await estabilizar();
    expect(porId(el, 'stock-minimo').getAttribute('aria-invalid')).toBe('true');
    expect(porId(el, 'margen-reserva').getAttribute('aria-invalid')).toBe('true');
    ctrl.expectNone(URL_INVENTARIO);
    expect(guardadas).toEqual([]);
  });

  it('CA-3-38 si catalog no responde al guardar, esa parte muestra el error y los parámetros de settings sí se guardan', async () => {
    const { el, estabilizar, comp, ctrl } = await montar();
    const guardadas = emisiones(comp as ParametrosTab);
    clic(el, 'guardar-parametros');
    await estabilizar();

    expect(guardadas).toHaveLength(1);
    ctrl
      .expectOne(URL_INVENTARIO)
      .flush(errorApi('SERVICIO_NO_DISPONIBLE', 'Servicio no disponible'), {
        status: 503,
        statusText: 'Service Unavailable',
      });
    await estabilizar();
    expect(porId(el, 'error-inventario').textContent).toContain('No pudimos guardar el inventario');
    expect(el.querySelector('[data-testid="ok-inventario"]')).toBeNull();
  });

  it('CA-3-38 si catalog está caído al abrir, el bloque Inventario avisa y los parámetros de settings se guardan igual', async () => {
    const { el, estabilizar, comp, ctrl } = await montar({ catalogCaido: true });
    expect(porId(el, 'error-inventario').textContent).toContain('No pudimos cargar el inventario');
    const guardadas = emisiones(comp as ParametrosTab);
    clic(el, 'guardar-parametros');
    await estabilizar();
    expect(guardadas).toHaveLength(1);
    ctrl.expectNone(URL_INVENTARIO);
  });

  it('CA-3-38 el bloque Inventario ofrece reintentar la lectura cuando catalog no respondió', async () => {
    const { el, estabilizar, ctrl } = await montar({ catalogCaido: true });
    clic(el, 'reintentar-inventario');
    await estabilizar();
    ctrl
      .expectOne(URL_INVENTARIO)
      .flush(respuesta({ stockMinimoPorDefecto: 3, margenReservaMinutos: 7 }));
    await estabilizar();
    expect((porId(el, 'stock-minimo') as HTMLInputElement).value).toBe('3');
    expect(el.querySelector('[data-testid="error-inventario"]')).toBeNull();
  });

  it('CA-3-17 el error 400 del servidor en un campo se muestra donde corresponde', async () => {
    const { el } = await montar({
      extra: { erroresServidor: { expiracionPedidoMinutos: 'Fuera de rango' } },
    });
    expect(porId(el, 'expiracion').getAttribute('aria-invalid')).toBe('true');
    expect(el.textContent).toContain('Fuera de rango');
  });
});
