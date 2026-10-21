import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { paymentApi, provideFitstoreHttp } from 'shared';
import { DetallePagoPage } from './detalle-pago.page';
import { SESION_ADMIN, SESION_OPERADOR } from '../../testing/sesiones';
import { montar as montarBackoffice } from '../../testing/montar';

const ID = '7c1d4a52-0000-4000-8000-000000000001';

function detalle(cambios: Partial<paymentApi.PagoDetalle> = {}): paymentApi.PagoDetalle {
  return {
    id: ID,
    codigoPedido: 'FS-000124',
    clienteNombre: 'Lucía Ramos',
    monto: 373.36,
    proveedor: 'MERCADO_PAGO',
    medio: 'TARJETA',
    intentos: 2,
    estado: 'REEMBOLSADA_PARCIAL',
    creadoEn: '2026-10-19T15:30:00Z',
    tieneReembolsoFallido: false,
    pedidoId: 'b6a1f0de-0000-4000-8000-0000000000aa',
    cliente: { nombre: 'Lucía Ramos', dni: '45871236', email: 'lucia.ramos@correo.pe' },
    transacciones: [
      {
        id: 't1',
        proveedor: 'MERCADO_PAGO',
        medio: 'TARJETA',
        estado: 'RECHAZADA',
        monto: 373.36,
        idProveedor: '101',
        codigoRespuesta: 'cc_rejected_insufficient_amount',
        mensaje: 'Fondos insuficientes',
        creadoEn: '2026-10-19T15:31:00Z',
      },
      {
        id: 't2',
        proveedor: 'MERCADO_PAGO',
        medio: 'TARJETA',
        estado: 'APROBADA',
        monto: 373.36,
        idProveedor: '102',
        codigoRespuesta: 'accredited',
        mensaje: '¡Pago aprobado!',
        creadoEn: '2026-10-19T15:33:00Z',
      },
    ],
    reembolsos: [
      {
        id: 'r1',
        intencionId: ID,
        codigoPedido: 'FS-000124',
        clienteNombre: 'Lucía Ramos',
        monto: 59.9,
        motivo: 'POSTVENTA',
        estado: 'COMPLETADO',
        intentos: 1,
        ultimoError: null,
        proximoIntentoEn: null,
        resueltoPor: null,
        nota: null,
        creadoEn: '2026-10-20T10:00:00Z',
      },
    ],
    boleta: { numero: 'B001-00000046', emitidaEn: '2026-10-19T15:34:00Z' },
    montoReembolsado: 59.9,
    neto: 313.46,
    ...cambios,
  };
}
const sobre = <T>(data: T) => ({ success: true, mensaje: 'Operación exitosa', data });

async function abrir(id = ID) {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([{ path: 'pagos/:id', component: DetallePagoPage }]),
      provideFitstoreHttp({ carrito: false }),
      provideHttpClientTesting(),
    ],
  });
  const ctrl = TestBed.inject(HttpTestingController);
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(`/pagos/${id}`, DetallePagoPage);
  return { ctrl, harness, el: harness.routeNativeElement as HTMLElement };
}
async function estable(h: RouterTestingHarness) {
  h.fixture.detectChanges();
  await h.fixture.whenStable();
  h.fixture.detectChanges();
}
const texto = (el: HTMLElement) => (el.textContent ?? '').replace(/\s+/g, ' ');

describe('Detalle de pago (P-5-04)', () => {
  it('CA-5-34 muestra cada intento con proveedor, medio, estado, código y mensaje de la pasarela, y la fecha', async () => {
    const { ctrl, harness, el } = await abrir();
    ctrl.expectOne(`/api/v1/admin/pagos/${ID}`).flush(sobre(detalle()));
    await estable(harness);

    const filas = [...el.querySelectorAll('table.fs-tabla-intentos tbody tr')];
    expect(filas).toHaveLength(2);
    expect(texto(filas[0] as HTMLElement)).toContain('Mercado Pago');
    expect(texto(filas[0] as HTMLElement)).toContain('Tarjeta');
    expect(texto(filas[0] as HTMLElement)).toContain('Pago rechazado');
    expect(texto(filas[0] as HTMLElement)).toContain('cc_rejected_insufficient_amount');
    expect(texto(filas[0] as HTMLElement)).toContain('Fondos insuficientes');
    expect(texto(filas[0] as HTMLElement)).toContain('19/10/2026 10:31');
    expect(texto(filas[1] as HTMLElement)).toContain('Aprobado');
    // los intentos no tienen detalle: las filas no son enlaces ni botones
    expect(el.querySelectorAll('table.fs-tabla-intentos tbody a, table.fs-tabla-intentos tbody button')).toHaveLength(0);
  });

  it('CA-5-34 muestra los reembolsos con motivo, monto y estado, y lo cobrado, lo reembolsado y lo neto', async () => {
    const { ctrl, harness, el } = await abrir();
    ctrl.expectOne(`/api/v1/admin/pagos/${ID}`).flush(sobre(detalle()));
    await estable(harness);

    const t = texto(el);
    expect(t).toContain('Postventa');
    expect(t).toContain('S/ 59.90');
    expect(t).toContain('Reembolsado');
    expect(t).toContain('Cobrado');
    expect(t).toContain('S/ 373.36');
    expect(t).toContain('Neto');
    expect(t).toContain('S/ 313.46');
  });

  it('CA-5-34 el encabezado enlaza al pedido y muestra al cliente, sin datos de tarjeta', async () => {
    const { ctrl, harness, el } = await abrir();
    ctrl.expectOne(`/api/v1/admin/pagos/${ID}`).flush(sobre(detalle()));
    await estable(harness);

    const enlace = el.querySelector<HTMLAnchorElement>('a[href="/pedidos/b6a1f0de-0000-4000-8000-0000000000aa"]')!;
    expect(enlace.textContent).toContain('FS-000124');
    expect(texto(el)).toContain('Lucía Ramos');
    expect(texto(el)).toContain('lucia.ramos@correo.pe');
    expect(texto(el)).not.toMatch(/\b\d{4} ?\d{4} ?\d{4} ?\d{4}\b/);
  });

  it('CA-5-34 la tarjeta de la boleta muestra el número y la descarga', async () => {
    const { ctrl, harness, el } = await abrir();
    ctrl.expectOne(`/api/v1/admin/pagos/${ID}`).flush(sobre(detalle()));
    await estable(harness);
    expect(texto(el)).toContain('B001-00000046');

    URL.createObjectURL = vi.fn(() => 'blob:boleta');
    URL.revokeObjectURL = vi.fn();
    [...el.querySelectorAll('button')].find((b) => b.textContent?.includes('Descargar boleta'))!.click();
    await estable(harness);
    const r = ctrl.expectOne(`/api/v1/admin/pagos/${ID}/boleta`);
    expect(r.request.responseType).toBe('blob');
    r.flush(new Blob(['%PDF'], { type: 'application/pdf' }));
    await estable(harness);

    expect(URL.createObjectURL).toHaveBeenCalled();
  });

  it('CA-5-34 sin boleta emitida lo dice y no ofrece la descarga', async () => {
    const { ctrl, harness, el } = await abrir();
    ctrl.expectOne(`/api/v1/admin/pagos/${ID}`).flush(sobre(detalle({ boleta: null })));
    await estable(harness);

    expect(texto(el)).toContain('La boleta todavía no se emitió');
    expect([...el.querySelectorAll('button')].some((b) => b.textContent?.includes('Descargar boleta'))).toBe(false);
  });

  it('CA-5-34 un pago sin reembolsos ni intentos lo dice', async () => {
    const { ctrl, harness, el } = await abrir();
    ctrl.expectOne(`/api/v1/admin/pagos/${ID}`).flush(
      sobre(detalle({ reembolsos: [], transacciones: [], intentos: 0, estado: 'PENDIENTE', proveedor: null, medio: null, montoReembolsado: 0, neto: 0, boleta: null })),
    );
    await estable(harness);

    expect(texto(el)).toContain('Sin reembolsos');
    expect(texto(el)).toContain('Todavía no hay intentos de cobro');
  });

  it('CA-5-34 un pago inexistente muestra el error con "Reintentar"', async () => {
    const { ctrl, harness, el } = await abrir();
    ctrl.expectOne(`/api/v1/admin/pagos/${ID}`).flush(
      { success: false, mensaje: 'No encontramos ese pago', codigo: 'NO_ENCONTRADO' },
      { status: 404, statusText: 'Not Found' },
    );
    await estable(harness);

    expect(el.querySelector('[role="alert"]')).not.toBeNull();
  });
});

describe('Acceso al backoffice de pagos', () => {
  beforeEach(() => localStorage.clear());

  it('CA-5-35 un OPERADOR no ve el menú Pagos ni entra a /pagos', async () => {
    const h = await montarBackoffice(SESION_OPERADOR, { url: '/pagos' });
    const el = h.routeNativeElement as HTMLElement;
    const menu = el.querySelector('nav[aria-label="Menú del backoffice"]') as HTMLElement | null;
    const items = [...(menu?.querySelectorAll('[data-clave] > a, [data-clave] > button') ?? [])].map((n) => n.textContent ?? '');

    expect(items.some((t) => t.includes('Pagos'))).toBe(false);
    expect(TestBed.inject(HttpTestingController).match(() => true).some((r) => r.request.url.startsWith('/api/v1/admin/pagos'))).toBe(false);
    expect(h.fixture.nativeElement.textContent).toMatch(/permiso|403/i);
  });

  it('CA-5-35 un ADMIN sí entra a /pagos y ve la lista', async () => {
    const h = await montarBackoffice(SESION_ADMIN, { url: '/pagos' });
    const ctrl = TestBed.inject(HttpTestingController);
    ctrl.expectOne((r) => r.url === '/api/v1/admin/pagos').flush({
      success: true,
      mensaje: 'Operación exitosa',
      data: { contenido: [], pagina: 0, tamano: 20, totalElementos: 0, totalPaginas: 0 },
    });
    h.fixture.detectChanges();
    await h.fixture.whenStable();

    expect(h.fixture.nativeElement.textContent).toContain('Pagos');
  });
});
