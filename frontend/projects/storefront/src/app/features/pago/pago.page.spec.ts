import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed, type ComponentFixture } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { paymentApi, provideFitstoreHttp } from 'shared';
import { PagoPage } from './pago.page';
import { SDK_MERCADO_PAGO, type SdkMercadoPago } from './sdk-mercado-pago';

const CODIGO = 'FS-000124';
const URL_PAGINA = `/api/v1/pagos/${CODIGO}`;
const URL_CARGOS = `/api/v1/pagos/${CODIGO}/cargos`;

type Pagina = paymentApi.PaginaPago;

function pagina(cambios: Partial<Pagina> = {}): Pagina {
  return {
    codigoPedido: CODIGO,
    estado: 'PENDIENTE',
    monto: 419.8,
    items: [
      { producto: 'Polo Dry-Fit', talla: 'M', color: 'Negro', cantidad: 2, total: 119.8 },
      { producto: 'Legging Pro', talla: 'S', color: 'Azul', cantidad: 1, total: 290 },
    ],
    costoEnvio: 10,
    expiraEn: new Date(Date.now() + 25 * 60_000).toISOString(),
    medios: [
      {
        proveedor: 'MERCADO_PAGO',
        medio: 'TARJETA',
        clavePublica: 'TEST-pub',
        disponible: true,
        motivoNoDisponible: null,
      },
      {
        proveedor: 'SIMULADO',
        medio: 'TARJETA',
        clavePublica: null,
        disponible: true,
        motivoNoDisponible: null,
      },
    ],
    topeYape: 500,
    intentosRechazados: [],
    procesando: false,
    boletaDisponible: false,
    simuladoHabilitado: true,
    ...cambios,
  };
}

const sobre = <T>(data: T, mensaje = 'Operación exitosa') => ({ success: true, mensaje, data });
const errorApi = (codigo: string, mensaje: string) => ({ success: false, mensaje, codigo });

/** Mercado Pago simulado: el Brick guarda su configuración para que la prueba dispare el envío. */
function sdkFalso() {
  const brick = { config: null as null | Record<string, unknown>, desmontado: false };
  const sdk: SdkMercadoPago = {
    crear: (clavePublica: string) => {
      brick.config = { clavePublica };
      return {
        bricks: () => ({
          create: async (_tipo: string, _id: string, config: Record<string, unknown>) => {
            brick.config = { ...brick.config, ...config };
            return { unmount: () => (brick.desmontado = true) };
          },
        }),
      };
    },
  };
  return { sdk, brick };
}

async function estable(f: ComponentFixture<unknown>) {
  f.detectChanges();
  await f.whenStable();
  f.detectChanges();
}

function preparar(sdk: SdkMercadoPago = sdkFalso().sdk) {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideFitstoreHttp({ carrito: false }),
      provideHttpClientTesting(),
      provideRouter([]),
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { paramMap: convertToParamMap({ codigo: CODIGO }) } },
      },
      { provide: SDK_MERCADO_PAGO, useValue: () => Promise.resolve(sdk) },
    ],
  });
  return TestBed.inject(HttpTestingController);
}

async function abrir(
  ctrl: HttpTestingController,
  respuesta: Pagina = pagina(),
): Promise<ComponentFixture<PagoPage>> {
  const f = TestBed.createComponent(PagoPage);
  await estable(f);
  ctrl.expectOne(URL_PAGINA).flush(sobre(respuesta));
  await estable(f);
  return f;
}

const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? '';
const el = (f: ComponentFixture<unknown>) => f.nativeElement as HTMLElement;
function boton(f: ComponentFixture<unknown>, parte: string): HTMLButtonElement {
  const b = [...el(f).querySelectorAll('button')].find((x) => x.textContent?.includes(parte));
  if (!b) throw new Error(`No hay un botón con "${parte}"`);
  return b as HTMLButtonElement;
}
function elegirMedio(f: ComponentFixture<unknown>, nombre: string) {
  const r = [...el(f).querySelectorAll<HTMLElement>('[role="radio"]')].find((x) =>
    x.textContent?.includes(nombre),
  );
  if (!r) throw new Error(`No hay un medio "${nombre}"`);
  r.click();
}

describe('Página de pago (P-5-01)', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'setInterval', 'clearInterval', 'Date'] });
  });
  afterEach(() => {
    vi.useRealTimers();
    TestBed.inject(HttpTestingController).verify();
  });

  it('CA-5-03 muestra el código, el resumen de productos, el total, la cuenta regresiva y los medios', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl);

    expect(texto(f)).toContain('FS-000124');
    expect(texto(f)).toContain('Polo Dry-Fit');
    expect(texto(f)).toContain('Legging Pro');
    expect(texto(f)).toContain('S/ 419.80');
    expect(el(f).querySelector('[role="timer"]')?.textContent).toMatch(/\d{2}:\d{2}/);
    const medios = [...el(f).querySelectorAll('[role="radio"]')].map((m) => m.textContent?.trim());
    expect(medios.some((m) => m?.includes('Mercado Pago'))).toBe(true);
    expect(medios.some((m) => m?.includes('Pago de prueba'))).toBe(true);
  });

  it('CA-5-03 la cuenta regresiva avanza cada segundo', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl, pagina({ expiraEn: new Date(Date.now() + 90_000).toISOString() }));
    expect(el(f).querySelector('[role="timer"]')?.textContent).toContain('01:30');

    await vi.advanceTimersByTimeAsync(5_000);
    await estable(f);

    expect(el(f).querySelector('[role="timer"]')?.textContent).toContain('01:25');
  });

  it('CA-5-04 mientras no existe la intención muestra "Preparando tu pago…" y reintenta cada segundo', async () => {
    const ctrl = preparar();
    const f = TestBed.createComponent(PagoPage);
    await estable(f);
    ctrl.expectOne(URL_PAGINA).flush(errorApi('PREPARANDO_PAGO', 'Preparando tu pago…'), {
      status: 404,
      statusText: 'Not Found',
    });
    await estable(f);
    expect(texto(f)).toContain('Preparando tu pago…');

    await vi.advanceTimersByTimeAsync(1_000);
    ctrl.expectOne(URL_PAGINA).flush(errorApi('PREPARANDO_PAGO', 'Preparando tu pago…'), {
      status: 404,
      statusText: 'Not Found',
    });
    await vi.advanceTimersByTimeAsync(1_000);
    ctrl.expectOne(URL_PAGINA).flush(sobre(pagina()));
    await estable(f);

    expect(texto(f)).not.toContain('Preparando tu pago…');
    expect(texto(f)).toContain('Polo Dry-Fit');
  });

  it('CA-5-04 tras 10 reintentos sin intención muestra un error con "Reintentar"', async () => {
    const ctrl = preparar();
    const f = TestBed.createComponent(PagoPage);
    await estable(f);
    for (let n = 0; n <= 10; n++) {
      ctrl.expectOne(URL_PAGINA).flush(errorApi('PREPARANDO_PAGO', 'Preparando tu pago…'), {
        status: 404,
        statusText: 'Not Found',
      });
      await vi.advanceTimersByTimeAsync(1_000);
    }
    await estable(f);

    ctrl.expectNone(URL_PAGINA);
    expect(el(f).querySelector('[role="alert"]')).not.toBeNull();
    expect(texto(f)).toContain('No pudimos preparar tu pago');
    expect(boton(f, 'Reintentar')).toBeTruthy();
  });

  it('CA-5-05 con un total mayor a S/ 500 Yape aparece deshabilitado con el aviso del tope', async () => {
    const ctrl = preparar();
    const f = await abrir(
      ctrl,
      pagina({
        monto: 650,
        medios: [
          ...pagina().medios,
          {
            proveedor: 'CULQI',
            medio: 'YAPE',
            clavePublica: 'pk',
            disponible: false,
            motivoNoDisponible: 'Yape permite hasta S/ 500 por día',
          },
        ],
      }),
    );

    const yape = [...el(f).querySelectorAll<HTMLElement>('[role="radio"]')].find((x) =>
      x.textContent?.includes('Yape'),
    )!;
    expect(yape.getAttribute('aria-disabled')).toBe('true');
    expect(texto(f)).toContain('Yape permite hasta S/ 500 por día');
  });

  it('CA-5-23 un medio con el circuito abierto aparece como "No disponible por el momento" y no se puede elegir', async () => {
    const ctrl = preparar();
    const f = await abrir(
      ctrl,
      pagina({
        medios: [
          {
            proveedor: 'MERCADO_PAGO',
            medio: 'TARJETA',
            clavePublica: 'TEST-pub',
            disponible: false,
            motivoNoDisponible: 'No disponible por el momento; elige otro medio de pago',
          },
          pagina().medios[1],
        ],
      }),
    );

    const mp = [...el(f).querySelectorAll<HTMLElement>('[role="radio"]')].find((x) =>
      x.textContent?.includes('Mercado Pago'),
    )!;
    expect(mp.getAttribute('aria-disabled')).toBe('true');
    expect(texto(f)).toContain('No disponible por el momento');
    mp.click();
    await estable(f);
    expect(el(f).querySelector('fs-brick-mercado-pago')).toBeNull();
  });

  it('CA-5-06 un pedido ya pagado muestra "Ya pagaste este pedido" y el enlace al pedido, sin formulario', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl, pagina({ estado: 'PAGADA' }));

    expect(texto(f)).toContain('Ya pagaste este pedido');
    expect(el(f).querySelector('a[href="/cuenta/pedidos/FS-000124"]')).not.toBeNull();
    expect(el(f).querySelector('[role="radio"]')).toBeNull();
  });

  it('CA-5-06 un pedido vencido muestra "El plazo para pagar venció" y el enlace al pedido', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl, pagina({ expiraEn: new Date(Date.now() - 60_000).toISOString() }));

    expect(texto(f)).toContain('El plazo para pagar venció');
    expect(el(f).querySelector('a[href="/cuenta/pedidos/FS-000124"]')).not.toBeNull();
    expect(el(f).querySelector('[role="radio"]')).toBeNull();
  });

  it('CA-5-06 un pedido cancelado muestra su estado y el enlace al pedido', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl, pagina({ estado: 'ANULADA' }));

    expect(texto(f)).toContain('Este pedido fue cancelado');
    expect(el(f).querySelector('a[href="/cuenta/pedidos/FS-000124"]')).not.toBeNull();
  });

  it('CA-5-08 cuando la cuenta regresiva llega a cero el formulario se bloquea y dice que el plazo venció', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl, pagina({ expiraEn: new Date(Date.now() + 3_000).toISOString() }));
    elegirMedio(f, 'Pago de prueba');
    await estable(f);
    expect(el(f).querySelector('[role="radio"]')).not.toBeNull();

    await vi.advanceTimersByTimeAsync(4_000);
    await estable(f);

    expect(texto(f)).toContain('El plazo para pagar venció');
    expect(el(f).querySelector('[role="radio"]')).toBeNull();
    expect(el(f).querySelector('input')).toBeNull();
  });
});

describe('Pagar con Mercado Pago, el simulado y el resultado (P-5-01 y P-5-02)', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'setInterval', 'clearInterval', 'Date'] });
  });
  afterEach(() => {
    vi.useRealTimers();
    TestBed.inject(HttpTestingController).verify();
  });

  it('CA-5-15 elegir Mercado Pago muestra el Brick y al enviarlo cobra con el token y una Idempotency-Key', async () => {
    const { sdk, brick } = sdkFalso();
    const ctrl = preparar(sdk);
    const f = await abrir(ctrl);

    elegirMedio(f, 'Mercado Pago');
    await estable(f);
    await vi.advanceTimersByTimeAsync(0);
    expect(el(f).querySelector('fs-brick-mercado-pago')).not.toBeNull();
    expect(brick.config?.['clavePublica']).toBe('TEST-pub');

    const onSubmit = (brick.config as { callbacks: { onSubmit: (d: unknown) => Promise<void> } }).callbacks
      .onSubmit;
    const enviado = onSubmit({
      token: 'tok_brick_123',
      payment_method_id: 'visa',
      installments: 1,
      issuer_id: '310',
      payer: { email: 'rosa@correo.pe' },
    });
    await vi.advanceTimersByTimeAsync(0);
    const req = ctrl.expectOne(URL_CARGOS);
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
    expect(req.request.body).toEqual({
      proveedor: 'MERCADO_PAGO',
      medio: 'TARJETA',
      token: 'tok_brick_123',
      email: 'rosa@correo.pe',
      datosAdicionales: { payment_method_id: 'visa', installments: 1, issuer_id: '310' },
    });
    req.flush(
      sobre(
        {
          estado: 'APROBADA',
          codigoPedido: CODIGO,
          monto: 419.8,
          proveedor: 'MERCADO_PAGO',
          medio: 'TARJETA',
          descripcionMedio: 'Visa ****1111',
        },
        '¡Pago aprobado!',
      ),
    );
    await enviado;
    await estable(f);

    expect(texto(f)).toContain('¡Pago aprobado!');
    expect(texto(f)).toContain('FS-000124');
    expect(texto(f)).toContain('Te enviamos la confirmación a tu correo');
    expect(el(f).querySelector('a[href="/cuenta/pedidos/FS-000124"]')).not.toBeNull();
    // la boleta se consulta hasta que esté lista
    ctrl.match(URL_PAGINA).forEach((r) => r.flush(sobre(pagina({ estado: 'PAGADA' }))));
  });

  it('CA-5-24 "Pago de prueba" muestra el aviso de que no es un cobro real', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl);

    elegirMedio(f, 'Pago de prueba');
    await estable(f);

    expect(texto(f)).toContain('no es un cobro real');
  });

  it('CA-5-25 el pago de prueba genera el token sim_<últimos 4> con la tarjeta ingresada', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl);
    elegirMedio(f, 'Pago de prueba');
    await estable(f);

    const campo = el(f).querySelector<HTMLInputElement>('input[name="tarjeta-simulada"]')!;
    campo.value = '4111 1111 1111 1111';
    campo.dispatchEvent(new Event('input'));
    await estable(f);
    boton(f, 'Pagar').click();
    await vi.advanceTimersByTimeAsync(0);

    const req = ctrl.expectOne(URL_CARGOS);
    expect(req.request.body).toMatchObject({ proveedor: 'SIMULADO', medio: 'TARJETA', token: 'sim_1111' });
    req.flush(
      sobre({ estado: 'APROBADA', codigoPedido: CODIGO, monto: 419.8, proveedor: 'SIMULADO', medio: 'TARJETA' }),
    );
    await estable(f);
    expect(texto(f)).toContain('¡Pago aprobado!');
    ctrl.match(URL_PAGINA).forEach((r) => r.flush(sobre(pagina({ estado: 'PAGADA' }))));
  });

  it('CA-5-25 sin una tarjeta de prueba válida no se puede pagar', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl);
    elegirMedio(f, 'Pago de prueba');
    await estable(f);

    expect(boton(f, 'Pagar').disabled).toBe(true);
    ctrl.expectNone(URL_CARGOS);
  });

  async function pagarConSimulado(f: ComponentFixture<PagoPage>, tarjeta: string) {
    elegirMedio(f, 'Pago de prueba');
    await estable(f);
    const campo = el(f).querySelector<HTMLInputElement>('input[name="tarjeta-simulada"]')!;
    campo.value = tarjeta;
    campo.dispatchEvent(new Event('input'));
    await estable(f);
    boton(f, 'Pagar').click();
    await vi.advanceTimersByTimeAsync(0);
  }

  it('CA-5-11 un rechazo muestra el motivo, deja reintentar y el reintento usa otra Idempotency-Key', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl);
    await pagarConSimulado(f, '4000 0000 0000 0002');
    const primera = ctrl.expectOne(URL_CARGOS);
    const clavePrimera = primera.request.headers.get('Idempotency-Key');
    primera.flush(errorApi('PAGO_RECHAZADO', 'Fondos insuficientes'), { status: 402, statusText: 'Payment Required' });
    await estable(f);

    expect(texto(f)).toContain('No pudimos cobrar tu pago');
    expect(texto(f)).toContain('Fondos insuficientes');

    boton(f, 'Intentar de nuevo').click();
    await estable(f);
    await pagarConSimulado(f, '4111 1111 1111 1111');
    const segunda = ctrl.expectOne(URL_CARGOS);
    expect(segunda.request.headers.get('Idempotency-Key')).not.toBe(clavePrimera);
    segunda.flush(
      sobre({ estado: 'APROBADA', codigoPedido: CODIGO, monto: 419.8, proveedor: 'SIMULADO', medio: 'TARJETA' }),
    );
    await estable(f);
    expect(texto(f)).toContain('¡Pago aprobado!');
    ctrl.match(URL_PAGINA).forEach((r) => r.flush(sobre(pagina({ estado: 'PAGADA' }))));
  });

  it('CA-5-17 si se corta la red el reintento reutiliza la misma Idempotency-Key', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl);
    await pagarConSimulado(f, '4111 1111 1111 1111');
    const primera = ctrl.expectOne(URL_CARGOS);
    const clave = primera.request.headers.get('Idempotency-Key');
    primera.error(new ProgressEvent('error'), { status: 0 });
    await estable(f);

    boton(f, 'Pagar').click();
    await vi.advanceTimersByTimeAsync(0);

    const segunda = ctrl.expectOne(URL_CARGOS);
    expect(segunda.request.headers.get('Idempotency-Key')).toBe(clave);
    segunda.flush(
      sobre({ estado: 'APROBADA', codigoPedido: CODIGO, monto: 419.8, proveedor: 'SIMULADO', medio: 'TARJETA' }),
    );
    await estable(f);
    ctrl.match(URL_PAGINA).forEach((r) => r.flush(sobre(pagina({ estado: 'PAGADA' }))));
  });

  it('CA-5-18 un 202 muestra "Estamos confirmando tu pago" y consulta el estado cada 3 s hasta tener el resultado', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl);
    await pagarConSimulado(f, '4000 0000 0000 0119');
    ctrl
      .expectOne(URL_CARGOS)
      .flush(
        sobre(
          { estado: 'PROCESANDO', codigoPedido: CODIGO, monto: 419.8, proveedor: 'SIMULADO', medio: 'TARJETA' },
          'Estamos confirmando tu pago',
        ),
        { status: 202, statusText: 'Accepted' },
      );
    await estable(f);
    expect(texto(f)).toContain('Estamos confirmando tu pago');
    expect(texto(f)).toContain('Puede tardar hasta 2 minutos');

    await vi.advanceTimersByTimeAsync(3_000);
    ctrl.expectOne(URL_PAGINA).flush(sobre(pagina({ procesando: true })));
    await estable(f);
    expect(texto(f)).toContain('Estamos confirmando tu pago');

    await vi.advanceTimersByTimeAsync(3_000);
    ctrl.expectOne(URL_PAGINA).flush(sobre(pagina({ estado: 'PAGADA' })));
    await estable(f);

    expect(texto(f)).toContain('¡Pago aprobado!');
    await vi.advanceTimersByTimeAsync(3_000);
    ctrl.match(URL_PAGINA).forEach((r) => r.flush(sobre(pagina({ estado: 'PAGADA' }))));
  });

  it('CA-5-18 si tras 2 minutos sigue sin resultado avisa que llegará por correo y enlaza al pedido', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl);
    await pagarConSimulado(f, '4000 0000 0000 0119');
    ctrl
      .expectOne(URL_CARGOS)
      .flush(
        sobre({ estado: 'PROCESANDO', codigoPedido: CODIGO, monto: 419.8, proveedor: 'SIMULADO', medio: 'TARJETA' }),
        { status: 202, statusText: 'Accepted' },
      );
    await estable(f);

    for (let n = 0; n < 40; n++) {
      await vi.advanceTimersByTimeAsync(3_000);
      ctrl.match(URL_PAGINA).forEach((r) => r.flush(sobre(pagina({ procesando: true }))));
    }
    await estable(f);

    expect(texto(f)).toContain('Te avisaremos por correo cuando se confirme');
    expect(el(f).querySelector('a[href="/cuenta/pedidos/FS-000124"]')).not.toBeNull();
    await vi.advanceTimersByTimeAsync(30_000);
    ctrl.expectNone(URL_PAGINA);
  });

  it('CA-5-18 si la página se abre con un cobro en curso retoma la consulta del resultado', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl, pagina({ procesando: true }));

    expect(texto(f)).toContain('Estamos confirmando tu pago');
    await vi.advanceTimersByTimeAsync(3_000);
    ctrl.expectOne(URL_PAGINA).flush(sobre(pagina({ estado: 'PAGADA' })));
    await estable(f);
    expect(texto(f)).toContain('¡Pago aprobado!');
    await vi.advanceTimersByTimeAsync(3_000);
    ctrl.match(URL_PAGINA).forEach((r) => r.flush(sobre(pagina({ estado: 'PAGADA' }))));
  });

  it('CA-5-29 el botón "Descargar boleta" aparece cuando la boleta está lista y descarga el PDF', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl, pagina({ estado: 'PAGADA' }));
    expect(texto(f)).toContain('Ya pagaste este pedido');
    expect(() => boton(f, 'Descargar boleta')).toThrow();

    // la consulta periódica de la boleta la encuentra
    await vi.advanceTimersByTimeAsync(3_000);
    ctrl.expectOne(URL_PAGINA).flush(sobre(pagina({ estado: 'PAGADA', boletaDisponible: true })));
    await estable(f);

    const descargar = boton(f, 'Descargar boleta');
    URL.createObjectURL = vi.fn(() => 'blob:boleta');
    URL.revokeObjectURL = vi.fn();
    descargar.click();
    await vi.advanceTimersByTimeAsync(0);
    const req = ctrl.expectOne(`/api/v1/pagos/${CODIGO}/boleta`);
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['%PDF'], { type: 'application/pdf' }));
    await estable(f);

    expect(URL.createObjectURL).toHaveBeenCalled();
  });
});

describe('Pago en un modal de procesamiento (P-5-01 y P-5-02)', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'setInterval', 'clearInterval', 'Date'] });
  });
  afterEach(() => {
    vi.useRealTimers();
    TestBed.inject(HttpTestingController).verify();
  });

  const dialogo = (f: ComponentFixture<unknown>) => el(f).querySelector<HTMLElement>('[role="dialog"]');

  async function pagar(f: ComponentFixture<PagoPage>, tarjeta: string) {
    elegirMedio(f, 'Pago de prueba');
    await estable(f);
    const campo = el(f).querySelector<HTMLInputElement>('input[name="tarjeta-simulada"]')!;
    campo.value = tarjeta;
    campo.dispatchEvent(new Event('input'));
    await estable(f);
    boton(f, 'Pagar').click();
    await vi.advanceTimersByTimeAsync(0);
    await estable(f);
  }

  const APROBADA = sobre({ estado: 'APROBADA', codigoPedido: CODIGO, monto: 419.8, proveedor: 'SIMULADO', medio: 'TARJETA' });

  it('CA-5-42 al pagar abre un modal de procesamiento con spinner y la pantalla de pago sigue detrás', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl);

    await pagar(f, '4111 1111 1111 1111');

    const d = dialogo(f);
    expect(d).not.toBeNull();
    expect(d?.textContent).toContain('Procesando tu pago');
    expect(d?.querySelector('.fs-spinner')).not.toBeNull();
    expect(el(f).querySelector('[role="radio"]')).not.toBeNull();
    expect(texto(f)).toContain('Paga tu pedido');
    ctrl.expectOne(URL_CARGOS).flush(APROBADA);
    await estable(f);
    ctrl.match(URL_PAGINA).forEach((r) => r.flush(sobre(pagina({ estado: 'PAGADA' }))));
  });

  it('CA-5-42 al aprobarse el mismo modal pasa a "¡Pago aprobado!" con sus botones, sin reemplazar la pantalla de pago', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl);
    await pagar(f, '4111 1111 1111 1111');

    ctrl.expectOne(URL_CARGOS).flush(APROBADA);
    await estable(f);

    const d = dialogo(f);
    expect(d?.textContent).toContain('¡Pago aprobado!');
    expect(d?.textContent).not.toContain('Procesando tu pago');
    expect(d?.querySelector('a[href="/cuenta/pedidos/FS-000124"]')).not.toBeNull();
    expect(texto(f)).not.toContain('Ya pagaste este pedido');
    expect(el(f).querySelector('[role="radio"]')).not.toBeNull();
    ctrl.match(URL_PAGINA).forEach((r) => r.flush(sobre(pagina({ estado: 'PAGADA' }))));
  });

  it('CA-5-42 al rechazarse el modal muestra el motivo y "Intentar de nuevo" lo cierra para reintentar sin salir de la pantalla', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl);
    await pagar(f, '4000 0000 0000 0002');

    ctrl.expectOne(URL_CARGOS).flush(errorApi('PAGO_RECHAZADO', 'Fondos insuficientes'), { status: 402, statusText: 'Payment Required' });
    await estable(f);

    expect(dialogo(f)?.textContent).toContain('No pudimos cobrar tu pago');
    expect(dialogo(f)?.textContent).toContain('Fondos insuficientes');
    expect(el(f).querySelector('[role="radio"]')).not.toBeNull();

    boton(f, 'Intentar de nuevo').click();
    await estable(f);

    expect(dialogo(f)).toBeNull();
    expect(texto(f)).toContain('Paga tu pedido');
  });

  it('CA-5-42 un pago en confirmación (202) mantiene el modal con "Estamos confirmando tu pago"', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl);
    await pagar(f, '4000 0000 0000 0119');

    ctrl
      .expectOne(URL_CARGOS)
      .flush(sobre({ estado: 'PROCESANDO', codigoPedido: CODIGO, monto: 419.8, proveedor: 'SIMULADO', medio: 'TARJETA' }), {
        status: 202,
        statusText: 'Accepted',
      });
    await estable(f);

    expect(dialogo(f)?.textContent).toContain('Estamos confirmando tu pago');
    await vi.advanceTimersByTimeAsync(3_000);
    ctrl.expectOne(URL_PAGINA).flush(sobre(pagina({ estado: 'PAGADA' })));
    await estable(f);
    expect(dialogo(f)?.textContent).toContain('¡Pago aprobado!');
    await vi.advanceTimersByTimeAsync(3_000);
    ctrl.match(URL_PAGINA).forEach((r) => r.flush(sobre(pagina({ estado: 'PAGADA' }))));
  });

  it('CA-5-43 el modal es accesible: role dialog, modal, estado en aria-live y el foco queda dentro', async () => {
    const ctrl = preparar();
    const f = await abrir(ctrl);
    await pagar(f, '4000 0000 0000 0002');

    const d = dialogo(f)!;
    expect(d.getAttribute('aria-modal')).toBe('true');
    expect(d.getAttribute('aria-label') ?? d.getAttribute('aria-labelledby')).toBeTruthy();
    expect(d.querySelector('[aria-live="polite"]')).not.toBeNull();
    expect(d.contains(document.activeElement)).toBe(true);

    ctrl.expectOne(URL_CARGOS).flush(errorApi('PAGO_RECHAZADO', 'Fondos insuficientes'), { status: 402, statusText: 'Payment Required' });
    await estable(f);
    await vi.advanceTimersByTimeAsync(0);
    await estable(f);

    expect(d.contains(document.activeElement)).toBe(true);
    expect(document.activeElement?.textContent).toContain('Intentar de nuevo');
  });
});
