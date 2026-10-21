import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { paymentApi, provideFitstoreHttp } from 'shared';
import { DetallePagoPage } from './detalle-pago.page';
import { PagosPage } from './pagos.page';

const URL_API = '/api/v1/admin/pagos';

function fila(cambios: Partial<paymentApi.PagoResumen> = {}): paymentApi.PagoResumen {
  return {
    id: '7c1d4a52-0000-4000-8000-000000000001',
    codigoPedido: 'FS-000124',
    clienteNombre: 'Lucía Ramos',
    monto: 373.36,
    proveedor: 'MERCADO_PAGO',
    medio: 'TARJETA',
    intentos: 2,
    estado: 'PAGADA',
    creadoEn: '2026-10-19T15:30:00Z',
    tieneReembolsoFallido: false,
    ...cambios,
  };
}

function pagina(filas: paymentApi.PagoResumen[], extra: Partial<paymentApi.PaginaPagos> = {}) {
  return {
    success: true,
    mensaje: 'Operación exitosa',
    data: { contenido: filas, pagina: 0, tamano: 20, totalElementos: filas.length, totalPaginas: 1, ...extra },
  };
}

async function abrir(url = '/pagos') {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([
        { path: 'pagos', component: PagosPage },
        { path: 'pagos/:id', component: DetallePagoPage },
      ]),
      provideFitstoreHttp({ carrito: false }),
      provideHttpClientTesting(),
    ],
  });
  const ctrl = TestBed.inject(HttpTestingController);
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url, PagosPage);
  return { ctrl, harness, el: harness.routeNativeElement as HTMLElement };
}
async function estable(h: RouterTestingHarness) {
  h.fixture.detectChanges();
  await h.fixture.whenStable();
  h.fixture.detectChanges();
}
/** Refresca la vista sin esperar la estabilidad: con una petición HTTP en curso `whenStable` no termina. */
async function refrescar(h: RouterTestingHarness) {
  h.fixture.detectChanges();
  await new Promise((resolver) => setTimeout(resolver, 0));
  h.fixture.detectChanges();
}
const peticion = (ctrl: HttpTestingController) => ctrl.expectOne((r) => r.url === URL_API);

describe('Pagos (P-5-03)', () => {
  it('CA-5-33 lista las intenciones con código, cliente, monto, proveedor y medio, estado y fecha', async () => {
    const { ctrl, harness, el } = await abrir();
    peticion(ctrl).flush(pagina([fila(), fila({ id: 'otro', codigoPedido: 'FS-000125', estado: 'PENDIENTE', proveedor: null, medio: null, intentos: 0 })]));
    await estable(harness);

    const filas = [...el.querySelectorAll('tr[mat-row]')];
    expect(filas).toHaveLength(2);
    const primera = filas[0].textContent ?? '';
    expect(primera).toContain('FS-000124');
    expect(primera).toContain('Lucía Ramos');
    expect(primera).toContain('S/ 373.36');
    expect(primera).toContain('Mercado Pago');
    expect(primera).toContain('Tarjeta');
    expect(primera).toContain('Pagado');
    expect(primera).toContain('19/10/2026');
    expect(filas[1].textContent).toContain('Pendiente de pago');
    expect(filas[1].textContent).toContain('—');
  });

  it('CA-5-33 el código del pedido abre el detalle del pago y la fila tiene "Ver ›"', async () => {
    const { ctrl, harness, el } = await abrir();
    peticion(ctrl).flush(pagina([fila()]));
    await estable(harness);

    const enlace = el.querySelector<HTMLAnchorElement>('a.fs-codigo')!;
    expect(enlace.getAttribute('href')).toBe('/pagos/7c1d4a52-0000-4000-8000-000000000001');
    expect(el.textContent).toContain('Ver ›');
  });

  it('CA-5-33 los filtros de la URL (estado, proveedor, fechas y código) viajan a la API', async () => {
    const { ctrl } = await abrir(
      '/pagos?estado=PAGADA&proveedor=MERCADO_PAGO&desde=2026-10-01&hasta=2026-10-31&q=FS-0001',
    );

    const r = peticion(ctrl);
    expect(r.request.params.get('estado')).toBe('PAGADA');
    expect(r.request.params.get('proveedor')).toBe('MERCADO_PAGO');
    expect(r.request.params.get('desde')).toBe('2026-10-01');
    expect(r.request.params.get('hasta')).toBe('2026-10-31');
    expect(r.request.params.get('codigo')).toBe('FS-0001');
    expect(r.request.params.get('page')).toBe('0');
    r.flush(pagina([]));
  });

  it('CA-5-33 elegir un estado en la barra actualiza la URL y vuelve a consultar', async () => {
    const { ctrl, harness, el } = await abrir();
    peticion(ctrl).flush(pagina([fila()]));
    await estable(harness);

    const select = el.querySelector<HTMLSelectElement>('select[aria-label="Estado"]')!;
    select.value = 'ANULADA';
    select.dispatchEvent(new Event('change'));
    await refrescar(harness);

    const r = peticion(ctrl);
    expect(r.request.params.get('estado')).toBe('ANULADA');
    expect(TestBed.inject(Router).url).toContain('estado=ANULADA');
    r.flush(pagina([]));
  });

  it('CA-5-33 las fechas "Desde" y "Hasta" se filtran desde la barra', async () => {
    const { ctrl, harness, el } = await abrir();
    peticion(ctrl).flush(pagina([fila()]));
    await estable(harness);

    const desde = el.querySelector<HTMLInputElement>('input[aria-label="Desde"]')!;
    desde.value = '2026-10-10';
    desde.dispatchEvent(new Event('change'));
    await refrescar(harness);
    const r = peticion(ctrl);
    expect(r.request.params.get('desde')).toBe('2026-10-10');
    r.flush(pagina([]));
    await estable(harness);

    expect(TestBed.inject(Router).url).toContain('desde=2026-10-10');
  });

  it('CA-5-33 cambiar de página pide la página siguiente', async () => {
    const { ctrl, harness, el } = await abrir();
    peticion(ctrl).flush(pagina([fila()], { totalElementos: 45, totalPaginas: 3 }));
    await estable(harness);

    el.querySelector<HTMLButtonElement>('button.mat-mdc-paginator-navigation-next')!.click();
    await refrescar(harness);

    const r = peticion(ctrl);
    expect(r.request.params.get('page')).toBe('1');
    r.flush(pagina([fila({ codigoPedido: 'FS-000200' })], { pagina: 1, totalElementos: 45, totalPaginas: 3 }));
  });

  it('CA-5-33 si la API falla muestra el error con "Reintentar"', async () => {
    const { ctrl, harness, el } = await abrir();
    peticion(ctrl).flush({ success: false, mensaje: 'Error', codigo: 'ERROR_INTERNO' }, { status: 500, statusText: 'Error' });
    await estable(harness);

    expect(el.querySelector('[role="alert"]')).not.toBeNull();
    el.querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
    await refrescar(harness);
    peticion(ctrl).flush(pagina([fila()]));
    await estable(harness);
    expect(el.querySelectorAll('tr[mat-row]')).toHaveLength(1);
  });

  it('CA-5-33 sin pagos muestra el estado vacío', async () => {
    const { ctrl, harness, el } = await abrir();
    peticion(ctrl).flush(pagina([]));
    await estable(harness);

    expect(el.textContent).toContain('Sin resultados');
  });
});
