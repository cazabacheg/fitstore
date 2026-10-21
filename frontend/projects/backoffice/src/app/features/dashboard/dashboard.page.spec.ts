import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed, type ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideFitstoreHttp, provideSesionConfig } from 'shared';
import { DashboardPage } from './dashboard.page';

const ok = <T>(data: T) => ({ success: true, mensaje: 'Operación exitosa', data });
const pagina = <T>(contenido: T[], total: number, extra: object = {}) =>
  ok({ contenido, pagina: 0, tamano: 1, totalElementos: total, totalPaginas: 1, ...extra });

const CONTADORES_PEDIDOS = {
  PENDIENTE_PAGO: 4,
  PAGADO: 7,
  EN_PREPARACION: 5,
  LISTO_DESPACHO: 3,
  ENVIADO: 9,
  LISTO_RECOJO: 2,
  ENTREGADO: 40,
};
const STOCK = [
  { varianteId: 1, sku: 'LEG01-NEG-XL', disponible: 0, estado: 'AGOTADO' },
  { varianteId: 2, sku: 'TOP02-ARE-S', disponible: 2, estado: 'BAJO' },
];

let fixture: ComponentFixture<DashboardPage>;
const http = () => TestBed.inject(HttpTestingController);
const texto = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

function abrir() {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([]),
      provideFitstoreHttp({ carrito: false }),
      provideHttpClientTesting(),
      provideSesionConfig({ tipo: 'STAFF', rutaLogin: '/ingresar', rutaSinPermiso: '/403' }),
    ],
  });
  fixture = TestBed.createComponent(DashboardPage);
  fixture.detectChanges();
}

/** Responde las cinco consultas de una carga; `entregas` en `null` simula el servicio caído. */
function responder(entregas: object | null = { PENDIENTE_ASIGNACION: 3, EN_RUTA: 4, ENTREGADA: 7, FALLIDA: 1 }) {
  http().expectOne((r) => r.url === '/api/v1/admin/pedidos').flush(pagina([], 0, { contadores: CONTADORES_PEDIDOS }));
  http().expectOne((r) => r.url === '/api/v1/admin/inventario').flush(pagina(STOCK, 2));
  const e = http().expectOne((r) => r.url === '/api/v1/admin/entregas');
  if (entregas) e.flush(pagina([], 15, { contadores: entregas }));
  else e.flush(ok(null), { status: 503, statusText: 'Servicio no disponible' });
  const reclamos = http().match((r) => r.url === '/api/v1/admin/reclamos');
  expect(reclamos.length).toBe(2);
  for (const r of reclamos) r.flush(pagina([], r.request.params.get('porVencer') === 'true' ? 2 : 0));
  fixture.detectChanges();
}

const enlaces = () => [...(fixture.nativeElement as HTMLElement).querySelectorAll<HTMLAnchorElement>('a')];
const href = (contiene: string) => enlaces().find((a) => a.textContent?.includes(contiene))?.getAttribute('href');
const boton = (contiene: string) =>
  [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].find((b) => b.textContent?.includes(contiene));

describe('Dashboard del backoffice (P-8-01)', () => {
  afterEach(() => http().verify());

  it('CA-8-01 muestra pedidos por estado, stock bajo, entregas de hoy y reclamos por vencer y vencidos', () => {
    abrir();
    responder();
    expect(texto()).toMatch(/Pendiente de pago\s*4/);
    expect(texto()).toMatch(/Pagado\s*7/);
    expect(texto()).toMatch(/Enviado\s*9/);
    expect(texto()).toContain('LEG01-NEG-XL');
    expect(texto()).toContain('Agotado');
    expect(texto()).toContain('TOP02-ARE-S');
    expect(texto()).toMatch(/Programadas\s*15/);
    expect(texto()).toMatch(/Entregadas\s*7/);
    expect(texto()).toMatch(/Fallidas\s*1/);
    expect(texto()).toMatch(/Por vencer\s*2/);
    expect(texto()).toMatch(/Vencidos\s*0/);
  });

  it('CA-8-01 pide cada contador con size=1 y el stock bajo con size=5', () => {
    abrir();
    const pedidos = http().expectOne((r) => r.url === '/api/v1/admin/pedidos');
    expect(pedidos.request.params.get('size')).toBe('1');
    const stock = http().expectOne((r) => r.url === '/api/v1/admin/inventario');
    expect(stock.request.params.get('soloStockBajo')).toBe('true');
    expect(stock.request.params.get('size')).toBe('5');
    const entregas = http().expectOne((r) => r.url === '/api/v1/admin/entregas');
    expect(entregas.request.params.get('fecha')).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    const reclamos = http().match((r) => r.url === '/api/v1/admin/reclamos');
    reclamos.forEach((r) => expect(r.request.params.get('size')).toBe('1'));
    pedidos.flush(pagina([], 0, { contadores: {} }));
    stock.flush(pagina([], 0));
    entregas.flush(pagina([], 0, { contadores: {} }));
    reclamos.forEach((r) => r.flush(pagina([], 0)));
  });

  it('CA-8-02 el OPERADOR ve lo mismo: no hay ventas ni top de productos', () => {
    abrir();
    responder();
    expect(texto()).not.toMatch(/Ventas|Más vendidos/);
    expect(texto()).toContain('Pedidos por estado');
  });

  it('CA-8-03 cada tarjeta lleva a su pantalla con el filtro aplicado', () => {
    abrir();
    responder();
    expect(href('Pagado')).toBe('/pedidos?estado=PAGADO');
    expect(href('Enviado')).toBe('/pedidos?estado=ENVIADO');
    expect(href('Ver inventario')).toBe('/inventario?estado=BAJO');
    expect(href('Entregas de hoy')).toBe('/entregas');
    expect(href('Por vencer')).toBe('/reclamos?plazo=porVencer');
    expect(href('Vencidos')).toBe('/reclamos?plazo=vencidos');
  });

  it('CA-8-04 si delivery no responde, solo su tarjeta dice "No disponible" y el resto se ve normal', () => {
    abrir();
    responder(null);
    expect(texto().match(/No disponible/g)?.length).toBe(1);
    expect(texto()).toMatch(/Pagado\s*7/);
    expect(texto()).toContain('LEG01-NEG-XL');
    expect(texto()).toMatch(/Por vencer\s*2/);
    // Reintentar vuelve a pedir solo esa tarjeta
    boton('Reintentar')?.click();
    http()
      .expectOne((r) => r.url === '/api/v1/admin/entregas')
      .flush(pagina([], 3, { contadores: { EN_RUTA: 3 } }));
    fixture.detectChanges();
    expect(texto()).not.toContain('No disponible');
    expect(texto()).toMatch(/Programadas\s*3/);
  });

  it('CA-8-05 carga al abrir y con "Actualizar", sin actualización automática, y muestra la hora de la última carga', () => {
    abrir();
    responder();
    expect(texto()).toMatch(/Actualizado a las \d{2}:\d{2}/);
    expect(boton('Actualizar')).toBeTruthy();
    boton('Actualizar')?.click();
    responder();
  });
});
