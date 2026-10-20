import { Component, signal } from '@angular/core';
import { TestBed, type ComponentFixture } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { BarraFiltros, type ChipFiltro, type SelectFiltro } from './barra-filtros';

const CHIPS: ChipFiltro[] = [
  { valor: 'PENDIENTE_PAGO', etiqueta: 'Pendiente de pago' },
  { valor: 'PAGADO', etiqueta: 'Pagado' },
];
const MODALIDADES: SelectFiltro = {
  clave: 'modalidad',
  etiqueta: 'Modalidad',
  opciones: [
    { valor: 'RECOJO', etiqueta: 'Recojo' },
    { valor: 'DELIVERY', etiqueta: 'Delivery local' },
  ],
};

@Component({
  imports: [BarraFiltros],
  template: `<fs-barra-filtros
    placeholder="Buscar por código o cliente"
    [selects]="selects()"
    [chips]="chips"
    [contadores]="contadores"
    (cambio)="cambios.push($event)"
  />`,
})
class Anfitrion {
  selects = signal<SelectFiltro[]>([MODALIDADES]);
  chips = CHIPS;
  contadores = { PENDIENTE_PAGO: 4, PAGADO: 7 };
  cambios: Record<string, string>[] = [];
}

async function montar(url = '/pedidos') {
  vi.useFakeTimers();
  TestBed.configureTestingModule({
    providers: [provideRouter([{ path: 'pedidos', component: Anfitrion }])],
  });
  const router = TestBed.inject(Router);
  await router.navigateByUrl(url);
  const f = TestBed.createComponent(Anfitrion);
  f.detectChanges();
  return { f, el: f.nativeElement as HTMLElement, router };
}
const chip = (el: HTMLElement, texto: string) =>
  [...el.querySelectorAll<HTMLElement>('.fs-chip')].find((c) =>
    c.textContent?.includes(texto),
  ) as HTMLElement;
async function ajustar(f: ComponentFixture<unknown>) {
  await vi.advanceTimersByTimeAsync(0);
  f.detectChanges();
}

describe('BarraFiltros (ui)', () => {
  afterEach(() => vi.useRealTimers());

  it('CA-0b-25 muestra los chips con los contadores del contrato y "Todos" con el total', async () => {
    const { el } = await montar();
    expect(
      [...el.querySelectorAll('.fs-chip')].map((c) => c.textContent?.replace(/\s+/g, ' ').trim()),
    ).toEqual(['Todos 11', 'Pendiente de pago 4', 'Pagado 7']);
    expect(chip(el, 'Todos').getAttribute('aria-pressed')).toBe('true');
  });

  it('CA-0b-25 elegir un chip lo marca con aria-pressed, lo escribe en la URL y emite el filtro', async () => {
    const { f, el, router } = await montar();
    chip(el, 'Pagado').click();
    await ajustar(f);
    expect(chip(el, 'Pagado').getAttribute('aria-pressed')).toBe('true');
    expect(chip(el, 'Todos').getAttribute('aria-pressed')).toBe('false');
    expect(router.url).toBe('/pedidos?estado=PAGADO');
    expect(f.componentInstance.cambios.at(-1)).toEqual({ estado: 'PAGADO' });
  });

  it('CA-0b-25 el buscador espera 400 ms antes de consultar', async () => {
    const { f, el, router } = await montar();
    const input = el.querySelector('input[type="search"]') as HTMLInputElement;
    input.value = 'FS-0001';
    input.dispatchEvent(new Event('input'));
    await vi.advanceTimersByTimeAsync(399);
    expect(f.componentInstance.cambios).toEqual([]);
    await vi.advanceTimersByTimeAsync(2);
    f.detectChanges();
    expect(f.componentInstance.cambios.at(-1)).toEqual({ q: 'FS-0001' });
    expect(router.url).toBe('/pedidos?q=FS-0001');
  });

  it('CA-0b-25 un select de filtro emite su valor y "Limpiar filtros" aparece solo si hay alguno activo', async () => {
    const { f, el, router } = await montar();
    const textoLimpiar = () =>
      [...el.querySelectorAll('button')].find((b) => b.textContent?.includes('Limpiar filtros'));
    expect(textoLimpiar()).toBeUndefined();
    const select = el.querySelector('select') as HTMLSelectElement;
    select.value = 'RECOJO';
    select.dispatchEvent(new Event('change'));
    await ajustar(f);
    expect(f.componentInstance.cambios.at(-1)).toEqual({ modalidad: 'RECOJO' });
    expect(textoLimpiar()).toBeDefined();
    textoLimpiar()?.click();
    await ajustar(f);
    expect(router.url).toBe('/pedidos');
    expect(f.componentInstance.cambios.at(-1)).toEqual({});
    expect(textoLimpiar()).toBeUndefined();
  });

  it('CA-0b-25 lee los filtros de la URL para que el enlace se pueda compartir', async () => {
    const { el } = await montar('/pedidos?estado=PAGADO&modalidad=DELIVERY&q=ana');
    expect(chip(el, 'Pagado').getAttribute('aria-pressed')).toBe('true');
    expect((el.querySelector('select') as HTMLSelectElement).value).toBe('DELIVERY');
    expect((el.querySelector('input[type="search"]') as HTMLInputElement).value).toBe('ana');
  });

  it('CA-0b-25 con más de 4 controles, los demás van en "Más filtros"', async () => {
    const { f, el } = await montar();
    f.componentInstance.selects.set(
      ['a', 'b', 'c', 'd'].map((k) => ({
        clave: k,
        etiqueta: k.toUpperCase(),
        opciones: [{ valor: '1', etiqueta: 'Uno' }],
      })),
    );
    f.detectChanges();
    expect(el.querySelectorAll('select').length).toBe(3); // buscador + 3 selects = 4 visibles
    const mas = [...el.querySelectorAll('button')].find((b) =>
      b.textContent?.includes('Más filtros'),
    ) as HTMLElement;
    mas.click();
    f.detectChanges();
    expect(el.querySelectorAll('select').length).toBe(4);
  });
});
