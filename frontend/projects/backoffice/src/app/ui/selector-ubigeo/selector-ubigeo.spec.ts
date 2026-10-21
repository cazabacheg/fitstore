import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { OverlayContainer } from '@angular/cdk/overlay';
import { Component, signal } from '@angular/core';
import { TestBed, type ComponentFixture } from '@angular/core/testing';
import { provideFitstoreHttp } from 'shared';
import { SelectorUbigeo, type HastaUbigeo } from './selector-ubigeo';

const DEPARTAMENTOS = [
  { codigo: '04', nombre: 'Arequipa' },
  { codigo: '15', nombre: 'Lima' },
];
const PROVINCIAS_LIMA = [
  { codigo: '1501', nombre: 'Lima' },
  { codigo: '1502', nombre: 'Barranca' },
];
const DISTRITOS_LIMA = [
  { codigo: '150101', nombre: 'Lima' },
  { codigo: '150122', nombre: 'Miraflores' },
];
const sobre = <T>(data: T) => ({ success: true, mensaje: 'Operación exitosa', data });

@Component({
  imports: [SelectorUbigeo],
  template: `<fs-selector-ubigeo
    [hasta]="hasta()"
    [(valor)]="valor"
    (valorChange)="emitidos.push($event)"
  />`,
})
class Anfitrion {
  hasta = signal<HastaUbigeo>('distrito');
  valor = signal<string | null>(null);
  emitidos: (string | null)[] = [];
}

function preparar() {
  localStorage.clear();
  TestBed.configureTestingModule({
    providers: [provideFitstoreHttp({ carrito: false }), provideHttpClientTesting()],
  });
  return TestBed.inject(HttpTestingController);
}
async function crear(
  ctrl: HttpTestingController,
  config: (a: Anfitrion) => void = () => undefined,
) {
  const f = TestBed.createComponent(Anfitrion);
  config(f.componentInstance);
  f.detectChanges();
  await f.whenStable();
  ctrl.expectOne('/api/v1/ubigeo/departamentos').flush(sobre(DEPARTAMENTOS));
  await estable(f);
  return f;
}
async function estable(f: ComponentFixture<unknown>) {
  f.detectChanges();
  await f.whenStable();
  f.detectChanges();
}
const selects = (f: ComponentFixture<unknown>) =>
  [...(f.nativeElement as HTMLElement).querySelectorAll('select')] as HTMLSelectElement[];
async function elegir(f: ComponentFixture<unknown>, nivel: number, codigo: string) {
  const s = selects(f)[nivel];
  s.value = codigo;
  s.dispatchEvent(new Event('change'));
  await estable(f);
}

/** El buscador espera 300 ms sin teclear antes de consultar. */
const esperarBuscador = () => new Promise((r) => setTimeout(r, 350));

/** Lo justo de `process` de Node para escuchar errores sin manejar (el proyecto no trae @types/node). */
interface NodeLike {
  on(evento: string, f: (e: unknown) => void): void;
  off(evento: string, f: (e: unknown) => void): void;
}

describe('SelectorUbigeo', () => {
  it('CA-0b-26 si /ubigeo falla, el selector queda vacío y no deja un error sin manejar', async () => {
    const ctrl = preparar();
    const f = TestBed.createComponent(Anfitrion);
    f.detectChanges();
    await f.whenStable();
    const errores: unknown[] = [];
    const capturar = (e: unknown) => errores.push(e);
    const proc = (globalThis as unknown as { process: NodeLike }).process;
    proc.on('uncaughtException', capturar);
    ctrl
      .expectOne('/api/v1/ubigeo/departamentos')
      .flush({ success: false, mensaje: 'Error', codigo: 'ERROR' }, { status: 500, statusText: 'Error' });
    await estable(f);
    await new Promise((r) => setTimeout(r, 0));
    proc.off('uncaughtException', capturar);
    expect(errores).toEqual([]);
    expect(selects(f)[0].options.length).toBe(1);
  });

  it('CA-0b-26 el buscador espera a que se deje de escribir: una sola consulta con el texto final', async () => {
    const ctrl = preparar();
    const f = await crear(ctrl);
    const buscador = (f.nativeElement as HTMLElement).querySelector(
      'input[type="search"], input[role="combobox"]',
    ) as HTMLInputElement;
    for (const texto of ['mi', 'mir', 'miraf']) {
      buscador.value = texto;
      buscador.dispatchEvent(new Event('input'));
    }
    await estable(f);
    ctrl.expectNone((r) => r.url === '/api/v1/ubigeo/distritos');
    await esperarBuscador();
    await estable(f);
    const pedidos = ctrl.match((r) => r.url === '/api/v1/ubigeo/distritos');
    expect(pedidos.map((p) => p.request.params.get('q'))).toEqual(['miraf']);
  });

  it('CA-0b-26 elegir Lima › Lima › Miraflores emite el código 150122', async () => {
    const ctrl = preparar();
    const f = await crear(ctrl);
    await elegir(f, 0, '15');
    ctrl.expectOne('/api/v1/ubigeo/departamentos/15/provincias').flush(sobre(PROVINCIAS_LIMA));
    await estable(f);
    await elegir(f, 1, '1501');
    ctrl.expectOne('/api/v1/ubigeo/provincias/1501/distritos').flush(sobre(DISTRITOS_LIMA));
    await estable(f);
    expect(f.componentInstance.emitidos).toEqual([]); // incompleto: aún no hay código
    await elegir(f, 2, '150122');
    expect(f.componentInstance.emitidos).toEqual(['150122']);
    expect(f.componentInstance.valor()).toBe('150122');
  });

  it('CA-0b-26 las listas vienen de /ubigeo y quedan en la caché del navegador (no se vuelven a pedir)', async () => {
    const ctrl = preparar();
    const f = await crear(ctrl);
    expect([...selects(f)[0].options].map((o) => o.textContent?.trim())).toEqual([
      'Selecciona',
      'Arequipa',
      'Lima',
    ]);
    const segundo = TestBed.createComponent(Anfitrion);
    segundo.detectChanges();
    await estable(segundo);
    ctrl.expectNone('/api/v1/ubigeo/departamentos');
    expect(selects(segundo)[0].options.length).toBe(3);
  });

  it('CA-0b-26 los niveles de abajo se habilitan al elegir el de arriba', async () => {
    const ctrl = preparar();
    const f = await crear(ctrl);
    expect(selects(f).map((s) => s.disabled)).toEqual([false, true, true]);
    await elegir(f, 0, '15');
    ctrl.expectOne('/api/v1/ubigeo/departamentos/15/provincias').flush(sobre(PROVINCIAS_LIMA));
    await estable(f);
    expect(selects(f).map((s) => s.disabled)).toEqual([false, false, true]);
  });

  it('CA-0b-26 con hasta = "departamento" se detiene en el primer select y emite 04', async () => {
    const ctrl = preparar();
    const f = await crear(ctrl, (a) => a.hasta.set('departamento'));
    expect(selects(f).length).toBe(1);
    await elegir(f, 0, '04');
    expect(f.componentInstance.emitidos).toEqual(['04']);
  });

  it('CA-0b-26 con hasta = "provincia" muestra dos selects y emite 0401', async () => {
    const ctrl = preparar();
    const f = await crear(ctrl, (a) => a.hasta.set('provincia'));
    expect(selects(f).length).toBe(2);
    await elegir(f, 0, '04');
    ctrl
      .expectOne('/api/v1/ubigeo/departamentos/04/provincias')
      .flush(sobre([{ codigo: '0401', nombre: 'Arequipa' }]));
    await estable(f);
    expect(f.componentInstance.emitidos).toEqual([]);
    await elegir(f, 1, '0401');
    expect(f.componentInstance.emitidos).toEqual(['0401']);
  });

  it('CA-0b-26 elegir otro departamento vacía provincia y distrito y deja el valor en nulo', async () => {
    const ctrl = preparar();
    const f = await crear(ctrl);
    await elegir(f, 0, '15');
    ctrl.expectOne('/api/v1/ubigeo/departamentos/15/provincias').flush(sobre(PROVINCIAS_LIMA));
    await estable(f);
    await elegir(f, 1, '1501');
    ctrl.expectOne('/api/v1/ubigeo/provincias/1501/distritos').flush(sobre(DISTRITOS_LIMA));
    await estable(f);
    await elegir(f, 2, '150122');
    await elegir(f, 0, '04');
    ctrl
      .expectOne('/api/v1/ubigeo/departamentos/04/provincias')
      .flush(sobre([{ codigo: '0401', nombre: 'Arequipa' }]));
    await estable(f);
    expect(selects(f)[1].value).toBe('');
    expect(selects(f)[2].value).toBe('');
    expect(f.componentInstance.valor()).toBeNull();
    expect(f.componentInstance.emitidos.at(-1)).toBeNull();
  });

  it('CA-0b-26 con un código inicial (150122) llena los tres selects', async () => {
    const ctrl = preparar();
    const f = TestBed.createComponent(Anfitrion);
    f.componentInstance.valor.set('150122');
    f.detectChanges();
    await f.whenStable();
    ctrl.expectOne('/api/v1/ubigeo/departamentos').flush(sobre(DEPARTAMENTOS));
    ctrl.expectOne('/api/v1/ubigeo/departamentos/15/provincias').flush(sobre(PROVINCIAS_LIMA));
    ctrl.expectOne('/api/v1/ubigeo/provincias/1501/distritos').flush(sobre(DISTRITOS_LIMA));
    await estable(f);
    expect(selects(f).map((s) => s.value)).toEqual(['15', '1501', '150122']);
    expect(f.componentInstance.emitidos).toEqual([]);
  });

  it('CA-0b-26 se puede buscar el distrito por nombre y llena los tres selects', async () => {
    const ctrl = preparar();
    const f = await crear(ctrl);
    const buscador = (f.nativeElement as HTMLElement).querySelector(
      'input[type="search"], input[role="combobox"]',
    ) as HTMLInputElement;
    buscador.focus();
    buscador.dispatchEvent(new Event('focusin'));
    buscador.value = 'miraf';
    buscador.dispatchEvent(new Event('input'));
    await esperarBuscador();
    await estable(f);
    const pedido = ctrl.expectOne((r) => r.url === '/api/v1/ubigeo/distritos');
    expect(pedido.request.params.get('q')).toBe('miraf');
    pedido.flush(
      sobre([
        { codigo: '150122', nombre: 'Miraflores', provincia: 'Lima', departamento: 'Lima' },
        { codigo: '040129', nombre: 'Miraflores', provincia: 'Arequipa', departamento: 'Arequipa' },
      ]),
    );
    await estable(f);
    const opciones = [
      ...TestBed.inject(OverlayContainer)
        .getContainerElement()
        .querySelectorAll<HTMLElement>('mat-option'),
    ];
    expect(opciones.map((o) => o.textContent?.trim())).toEqual([
      'Miraflores · Lima, Lima',
      'Miraflores · Arequipa, Arequipa',
    ]);
    opciones[0].click();
    await estable(f);
    ctrl.expectOne('/api/v1/ubigeo/departamentos/15/provincias').flush(sobre(PROVINCIAS_LIMA));
    ctrl.expectOne('/api/v1/ubigeo/provincias/1501/distritos').flush(sobre(DISTRITOS_LIMA));
    await estable(f);
    expect(selects(f).map((s) => s.value)).toEqual(['15', '1501', '150122']);
    expect(f.componentInstance.emitidos).toEqual(['150122']);
  });

  it('CA-0b-26 el buscador por nombre solo aparece cuando se llega hasta el distrito', async () => {
    const ctrl = preparar();
    const f = await crear(ctrl, (a) => a.hasta.set('provincia'));
    expect((f.nativeElement as HTMLElement).querySelector('input[role="combobox"]')).toBeNull();
  });
});
