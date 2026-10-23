import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { PedidosPage } from './pedidos.page';
import { fila, http, ok, proveedores } from './pruebas-pedidos';

const RUTA = '/api/v1/admin/pedidos';

const pagina = (contenido: ReturnType<typeof fila>[], extra: Record<string, number> = {}) =>
  ok({
    contenido,
    pagina: 0,
    tamano: 20,
    totalElementos: contenido.length,
    totalPaginas: 1,
    contadores: { PAGADO: 2, ENTREGADO: 1, PENDIENTE_PAGO: 4, ...extra },
  });

async function abrir(url: string) {
  TestBed.configureTestingModule({
    providers: [provideZonelessChangeDetection(), ...proveedores([{ path: 'pedidos', component: PedidosPage }])],
  });
  const harness = await RouterTestingHarness.create();
  const componente = harness.navigateByUrl(url, PedidosPage);
  await componente;
  // El efecto que pide la lista corre en la primera detección de cambios
  harness.detectChanges();
  return { harness, componente: Promise.resolve() };
}

const consulta = () => http().expectOne((r) => r.url === RUTA);

describe('Pedidos del backoffice (P-4-05)', () => {
  afterEach(() => http().verify());

  it('CA-4-40 muestra código, fecha, cliente, modalidad, estado y total, y un contador por estado', async () => {
    const { harness, componente } = await abrir('/pedidos');
    consulta().flush(pagina([fila('FS-000121', { estado: 'PAGADO' }), fila('FS-000119', { estado: 'ENTREGADO', modalidad: 'RECOJO' })]));
    await componente;
    harness.detectChanges();
    await harness.fixture.whenStable();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('FS-000121');
    expect(texto).toContain('20/10/2026');
    expect(texto).toContain('Ana Quispe');
    expect(texto).toContain('Delivery local');
    expect(texto).toContain('Recojo');
    expect(texto).toContain('Pagado');
    expect(texto).toContain('S/ 129.80');
    // chips con su contador
    expect(texto).toMatch(/Pagado\s*2/);
    expect(texto).toMatch(/Pendiente de pago\s*4/);
    const enlaces = [...(harness.routeNativeElement?.querySelectorAll<HTMLAnchorElement>('a') ?? [])];
    expect(enlaces.find((a) => a.textContent?.includes('FS-000121'))?.getAttribute('href')).toBe('/pedidos/id-FS-000121');
  });

  it('CA-4-41 los filtros de la URL (estado, modalidad, fechas y código) viajan a la API', async () => {
    const { harness, componente } = await abrir(
      '/pedidos?estado=PAGADO&modalidad=AGENCIA&desde=2026-10-01&hasta=2026-10-20&q=FS-000121',
    );
    const peticion = consulta();
    expect(peticion.request.params.get('estado')).toBe('PAGADO');
    expect(peticion.request.params.get('modalidad')).toBe('AGENCIA');
    expect(peticion.request.params.get('desde')).toBe('2026-10-01');
    expect(peticion.request.params.get('hasta')).toBe('2026-10-20');
    expect(peticion.request.params.get('codigo')).toBe('FS-000121');
    expect(peticion.request.params.has('cliente')).toBe(false);
    peticion.flush(pagina([fila('FS-000121')]));
    await componente;
    await harness.fixture.whenStable();
  });

  it('CA-4-41 un texto que no es un código busca por nombre, correo o DNI del cliente', async () => {
    const { harness, componente } = await abrir('/pedidos?q=ana%40correo.pe');
    const peticion = consulta();
    expect(peticion.request.params.get('cliente')).toBe('ana@correo.pe');
    expect(peticion.request.params.has('codigo')).toBe(false);
    peticion.flush(pagina([fila('FS-000121')]));
    await componente;
    await harness.fixture.whenStable();
  });

  it('CA-4-42 desde la ficha de un cliente (clienteId) solo se ven sus pedidos, con su nombre en el filtro', async () => {
    const { harness, componente } = await abrir('/pedidos?clienteId=35a11d4f-054a-44a5-8ac4-cf0ed4020f8c');
    const peticion = consulta();
    expect(peticion.request.params.get('clienteId')).toBe('35a11d4f-054a-44a5-8ac4-cf0ed4020f8c');
    peticion.flush(pagina([fila('FS-000121', { clienteNombre: 'Lucía Mendoza' })]));
    await componente;
    harness.detectChanges();
    await harness.fixture.whenStable();

    expect(harness.routeNativeElement?.textContent).toContain('Pedidos de Lucía Mendoza');
  });

  it('CA-4-61 las columnas Pedido, Fecha, Estado y Total se ordenan; Cliente y Modalidad no, porque el servidor no las acepta', async () => {
    const { harness, componente } = await abrir('/pedidos');
    consulta().flush(pagina([fila('FS-000121'), fila('FS-000119')]));
    await componente;
    harness.detectChanges();
    await harness.fixture.whenStable();
    harness.detectChanges();

    const ordenables = [...(harness.routeNativeElement?.querySelectorAll<HTMLElement>('th') ?? [])]
      .filter((th) => th.querySelector('.mat-sort-header-container') && !th.classList.contains('mat-sort-header-disabled'))
      .map((th) => th.textContent?.trim());
    expect(ordenables).toEqual(['Pedido', 'Fecha', 'Estado', 'Total']);
  });

  it('CA-4-61 al ordenar pide al servidor el campo del contrato, descendente al segundo clic, con el código como desempate y desde la página 0', async () => {
    const { harness, componente } = await abrir('/pedidos');
    consulta().flush(pagina([fila('FS-000121'), fila('FS-000119')]));
    await componente;
    harness.detectChanges();
    await harness.fixture.whenStable();
    harness.detectChanges();

    const el = harness.routeNativeElement as HTMLElement;
    const cabecera = (titulo: string) =>
      [...el.querySelectorAll<HTMLElement>('th')]
        .find((th) => th.textContent?.trim() === titulo)
        ?.querySelector<HTMLElement>('.mat-sort-header-container');
    const recargar = async (respuesta: ReturnType<typeof pagina>) => {
      const peticion = consulta();
      peticion.flush(respuesta);
      harness.detectChanges();
      await harness.fixture.whenStable();
      harness.detectChanges();
      return peticion.request.params.getAll('sort');
    };

    cabecera('Total')?.click();
    harness.detectChanges();
    expect(await recargar(pagina([fila('FS-000119')]))).toEqual(['total,asc', 'codigo,asc']);

    cabecera('Total')?.click();
    harness.detectChanges();
    expect(await recargar(pagina([fila('FS-000121')]))).toEqual(['total,desc', 'codigo,asc']);

    cabecera('Pedido')?.click();
    harness.detectChanges();
    expect(await recargar(pagina([fila('FS-000121')]))).toEqual(['codigo,asc']);
  });

  it('CA-4-40 si la API falla ofrece reintentar', async () => {
    const { harness, componente } = await abrir('/pedidos');
    consulta().flush(ok(null), { status: 500, statusText: 'Error' });
    await componente;
    harness.detectChanges();
    await harness.fixture.whenStable();

    expect(harness.routeNativeElement?.textContent).toContain('No pudimos cargar los pedidos');
  });
});
