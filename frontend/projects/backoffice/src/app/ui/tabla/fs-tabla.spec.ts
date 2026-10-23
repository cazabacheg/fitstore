import { OverlayContainer } from '@angular/cdk/overlay';
import { Component, signal } from '@angular/core';
import { TestBed, type ComponentFixture } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import type { ConsultaPagina, Pagina } from 'shared';
import { FsCelda } from './fs-celda';
import { FsTabla, type AccionFila, type ColumnaTabla } from './fs-tabla';

interface Pedido {
  codigo: string;
  cliente: string;
  total: number;
  estado: string;
}
const FILAS: Pedido[] = [
  { codigo: 'FS-000119', cliente: 'Luis Rojas', total: 129.8, estado: 'Pagado' },
  { codigo: 'FS-000121', cliente: 'Ana Quispe', total: 59.9, estado: 'Por vencer' },
];
const COLUMNAS: ColumnaTabla<Pedido>[] = [
  { clave: 'codigo', titulo: 'Pedido', valor: (f) => f.codigo, ordenable: true },
  { clave: 'cliente', titulo: 'Cliente', valor: (f) => f.cliente },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'total', titulo: 'Total', valor: (f) => `S/ ${f.total.toFixed(2)}`, ordenable: true },
];
const pagina = (contenido: Pedido[], extra: Partial<Pagina<Pedido>> = {}): Pagina<Pedido> => ({
  contenido,
  pagina: 0,
  tamano: 20,
  totalElementos: 64,
  totalPaginas: 4,
  ...extra,
});

@Component({
  imports: [FsTabla, FsCelda],
  template: `
    <fs-tabla
      [columnas]="columnas"
      [datos]="datos()"
      columnaId="codigo"
      [filaEnlace]="enlace()"
      [abribleEnDialogo]="dialogo()"
      [acciones]="acciones()"
      [cargando]="cargando()"
      [claseFila]="claseFila"
      (consulta)="consultas.push($event)"
      (alAbrir)="abiertos.push($event.codigo)"
    >
      <ng-template fsCelda="estado" let-fila>
        <span class="insignia">{{ fila.estado }}</span>
      </ng-template>
    </fs-tabla>
  `,
})
class Anfitrion {
  columnas = COLUMNAS;
  datos = signal<Pagina<Pedido> | null>(pagina(FILAS));
  cargando = signal(false);
  enlace = signal<((f: Pedido) => unknown) | null>((f) => ['/pedidos', f.codigo]);
  dialogo = signal(false);
  acciones = signal<((f: Pedido) => AccionFila<Pedido>[]) | null>(null);
  consultas: ConsultaPagina[] = [];
  abiertos: string[] = [];
  claseFila = (f: Pedido) => (f.estado === 'Por vencer' ? ('aviso' as const) : null);
}

@Component({ template: 'detalle' })
class Detalle {}

async function montar(): Promise<{
  f: ComponentFixture<Anfitrion>;
  el: HTMLElement;
  comp: Anfitrion;
}> {
  TestBed.configureTestingModule({
    providers: [provideRouter([{ path: 'pedidos/:codigo', component: Detalle }])],
  });
  const f = TestBed.createComponent(Anfitrion);
  f.detectChanges();
  await f.whenStable();
  f.detectChanges();
  return { f, el: f.nativeElement as HTMLElement, comp: f.componentInstance };
}
async function estable(f: ComponentFixture<unknown>) {
  f.detectChanges();
  await f.whenStable();
  f.detectChanges();
}
const filas = (el: HTMLElement) => [...el.querySelectorAll<HTMLElement>('tbody tr')];

describe('FsTabla: paginación y orden', () => {
  it('CA-0b-28 al cambiar de página emite page, size y sort en el formato de la API', async () => {
    const { f, el, comp } = await montar();
    (el.querySelector('button.mat-mdc-paginator-navigation-next') as HTMLElement).click();
    await estable(f);
    expect(comp.consultas.at(-1)).toEqual({ page: 1, size: 20, sort: [] });
  });

  it('CA-0b-28 al cambiar el tamaño de página emite page 0 y el nuevo size', async () => {
    const { f, el, comp } = await montar();
    (el.querySelector('.mat-mdc-select-trigger') as HTMLElement).click();
    await estable(f);
    const opciones = [
      ...TestBed.inject(OverlayContainer)
        .getContainerElement()
        .querySelectorAll<HTMLElement>('mat-option'),
    ];
    opciones.find((o) => o.textContent?.trim() === '50')?.click();
    await estable(f);
    expect(comp.consultas.at(-1)).toEqual({ page: 0, size: 50, sort: [] });
  });

  it('CA-0b-28 al ordenar emite sort como campo,asc y luego campo,desc, y vuelve a la página 0', async () => {
    const { f, el, comp } = await montar();
    (el.querySelector('button.mat-mdc-paginator-navigation-next') as HTMLElement).click();
    await estable(f);
    comp.datos.set(pagina(FILAS, { pagina: 1 }));
    await estable(f);
    const cabecera = () => el.querySelector('th .mat-sort-header-container') as HTMLElement;
    cabecera().click();
    await estable(f);
    expect(comp.consultas.at(-1)).toEqual({ page: 0, size: 20, sort: ['codigo,asc'] });
    cabecera().click();
    await estable(f);
    expect(comp.consultas.at(-1)).toEqual({ page: 0, size: 20, sort: ['codigo,desc'] });
  });

  it('CA-0b-32 el orden elegido sobrevive a la recarga de la tabla: sigue marcado y el siguiente clic pasa a descendente', async () => {
    const { f, el, comp } = await montar();
    const cabecera = () => el.querySelector('th .mat-sort-header-container') as HTMLElement;
    cabecera().click();
    await estable(f);
    expect(comp.consultas.at(-1)?.sort).toEqual(['codigo,asc']);
    // La pantalla recarga: la tabla se reemplaza por el esqueleto y vuelve con los datos nuevos
    comp.cargando.set(true);
    await estable(f);
    comp.cargando.set(false);
    await estable(f);
    expect(el.querySelector('th')?.getAttribute('aria-sort')).toBe('ascending');
    cabecera().click();
    await estable(f);
    expect(comp.consultas.at(-1)?.sort).toEqual(['codigo,desc']);
  });

  it('CA-0b-32 un tercer clic quita el orden y la tabla vuelve al orden por defecto del servidor', async () => {
    const { f, el, comp } = await montar();
    const cabecera = () => el.querySelector('th .mat-sort-header-container') as HTMLElement;
    for (let i = 0; i < 3; i++) {
      cabecera().click();
      await estable(f);
    }
    expect(comp.consultas.at(-1)?.sort).toEqual([]);
  });

  it('CA-0b-28 solo las columnas ordenables tienen cabecera de orden', async () => {
    const { el } = await montar();
    const ordenables = [...el.querySelectorAll('th')].filter(
      (th) =>
        th.querySelector('.mat-sort-header-container') &&
        !th.classList.contains('mat-sort-header-disabled'),
    );
    expect(ordenables.map((th) => th.textContent?.trim())).toEqual(['Pedido', 'Total']);
  });

  it('CA-0b-28 muestra el rango "1–20 de 64" del paginador en español', async () => {
    const { el } = await montar();
    expect(el.querySelector('.mat-mdc-paginator-range-label')?.textContent?.trim()).toBe(
      '1 – 20 de 64',
    );
  });
});

describe('FsTabla: convención de filas R11', () => {
  it('CA-0b-28b el identificador de la fila es un enlace real', async () => {
    const { el } = await montar();
    const enlace = filas(el)[0].querySelector('td a') as HTMLAnchorElement;
    expect(enlace.textContent?.trim()).toBe('FS-000119');
    expect(enlace.getAttribute('href')).toBe('/pedidos/FS-000119');
  });

  it('CA-0b-28b un clic en cualquier parte de la fila abre el detalle', async () => {
    const { f, el } = await montar();
    (filas(el)[1].querySelectorAll('td')[1] as HTMLElement).click();
    await estable(f);
    expect(TestBed.inject(Router).url).toBe('/pedidos/FS-000121');
  });

  it('CA-0b-28b el clic en el enlace navega una sola vez (la fila no lo repite)', async () => {
    const { f, el } = await montar();
    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigateByUrl');
    const navegar2 = vi.spyOn(router, 'navigate');
    (filas(el)[0].querySelector('td a') as HTMLElement).click();
    await estable(f);
    expect(router.url).toBe('/pedidos/FS-000119');
    expect(navegar.mock.calls.length + navegar2.mock.calls.length).toBe(1);
  });

  it('CA-0b-28b Ctrl+clic en el enlace no navega en la misma pestaña ni abre dos veces', async () => {
    const { f, el } = await montar();
    const router = TestBed.inject(Router);
    const abrir = vi.spyOn(window, 'open').mockReturnValue(null);
    const enlace = filas(el)[0].querySelector('td a') as HTMLElement;
    enlace.dispatchEvent(
      new MouseEvent('click', { bubbles: true, cancelable: true, ctrlKey: true }),
    );
    await estable(f);
    expect(router.url).toBe('/');
    expect(abrir).not.toHaveBeenCalled(); // la pestaña nueva la abre el navegador con el href real
    abrir.mockRestore();
  });

  it('CA-0b-28b la fila responde al ratón: es clicable y tiene la clase de cambio de color', async () => {
    const { el } = await montar();
    expect(filas(el)[0].classList.contains('fs-fila--clicable')).toBe(true);
  });

  it('CA-0b-28b la última columna muestra "Ver ›" cuando la fila solo tiene detalle', async () => {
    const { f, el } = await montar();
    const ver = filas(el)[0].querySelector('td:last-child a') as HTMLAnchorElement;
    expect(ver.textContent?.trim()).toBe('Ver ›');
    expect(ver.getAttribute('href')).toBe('/pedidos/FS-000119');
    ver.click();
    await estable(f);
    expect(TestBed.inject(Router).url).toBe('/pedidos/FS-000119');
  });

  it('CA-0b-28b con 1 o 2 acciones muestra botones y un clic en ellos no abre el detalle', async () => {
    const { f, el, comp } = await montar();
    const cancelar = vi.fn();
    const asignar = vi.fn();
    comp.acciones.set(() => [
      { etiqueta: 'Asignar', alEjecutar: asignar },
      { etiqueta: 'Cancelar', alEjecutar: cancelar },
    ]);
    await estable(f);
    const botones = [...filas(el)[0].querySelectorAll<HTMLElement>('td:last-child button')];
    expect(botones.map((b) => b.textContent?.trim())).toEqual(['Asignar', 'Cancelar']);
    expect(filas(el)[0].querySelector('td:last-child a')).toBeNull();
    botones[1].click();
    await estable(f);
    expect(cancelar).toHaveBeenCalledWith(FILAS[0]);
    expect(asignar).not.toHaveBeenCalled();
    expect(TestBed.inject(Router).url).toBe('/');
  });

  it('CA-0b-28b con 3 o más acciones usa el menú "⋮" y elegir una no abre el detalle', async () => {
    const { f, el, comp } = await montar();
    const reembolsar = vi.fn();
    comp.acciones.set(() => [
      { etiqueta: 'Asignar', alEjecutar: vi.fn() },
      { etiqueta: 'Editar', alEjecutar: vi.fn() },
      { etiqueta: 'Reembolsar', alEjecutar: reembolsar },
    ]);
    await estable(f);
    const disparador = filas(el)[0].querySelector('td:last-child button') as HTMLElement;
    expect(disparador.getAttribute('aria-label')).toBe('Más acciones');
    expect(filas(el)[0].querySelectorAll('td:last-child button').length).toBe(1);
    disparador.click();
    await estable(f);
    const panel = TestBed.inject(OverlayContainer).getContainerElement();
    const items = [...panel.querySelectorAll<HTMLElement>('[role="menuitem"]')];
    expect(items.map((i) => i.textContent?.trim())).toEqual(['Asignar', 'Editar', 'Reembolsar']);
    items[2].click();
    await estable(f);
    expect(reembolsar).toHaveBeenCalledWith(FILAS[0]);
    expect(TestBed.inject(Router).url).toBe('/');
  });

  it('CA-0b-28b en una tabla sin detalle las filas no son clicables ni tienen enlace', async () => {
    const { f, el, comp } = await montar();
    comp.enlace.set(null);
    await estable(f);
    expect(el.querySelector('tbody a')).toBeNull();
    expect(filas(el)[0].classList.contains('fs-fila--clicable')).toBe(false);
    expect(el.querySelector('table')?.classList.contains('fs-tabla--plana')).toBe(true);
    (filas(el)[0].querySelectorAll('td')[1] as HTMLElement).click();
    await estable(f);
    expect(TestBed.inject(Router).url).toBe('/');
    expect(comp.abiertos).toEqual([]);
  });

  it('CA-0b-28b sin página de detalle, el identificador abre el diálogo de edición (alAbrir)', async () => {
    const { f, el, comp } = await montar();
    comp.enlace.set(null);
    comp.dialogo.set(true);
    await estable(f);
    const id = filas(el)[0].querySelector('td a, td button.fs-enlace') as HTMLElement;
    expect(id.textContent?.trim()).toBe('FS-000119');
    id.click();
    await estable(f);
    (filas(el)[1].querySelectorAll('td')[1] as HTMLElement).click();
    await estable(f);
    expect(comp.abiertos).toEqual(['FS-000119', 'FS-000121']);
  });
});

describe('FsTabla: celdas, variantes y estados', () => {
  it('CA-0b-28 usa la plantilla de celda del consumidor y marca las filas por vencer', async () => {
    const { el } = await montar();
    expect(filas(el)[0].querySelector('.insignia')?.textContent).toBe('Pagado');
    expect(filas(el)[1].classList.contains('fs-fila--aviso')).toBe(true);
    expect(filas(el)[0].classList.contains('fs-fila--aviso')).toBe(false);
  });

  it('CA-0b-28 mientras carga muestra el esqueleto y sin filas muestra el estado vacío', async () => {
    const { f, el, comp } = await montar();
    comp.cargando.set(true);
    await estable(f);
    expect(el.querySelector('[aria-label="Cargando"]')).not.toBeNull();
    expect(el.querySelector('table')).toBeNull();
    comp.cargando.set(false);
    comp.datos.set(pagina([], { totalElementos: 0, totalPaginas: 0 }));
    await estable(f);
    expect(el.textContent).toContain('Sin resultados');
  });
});
