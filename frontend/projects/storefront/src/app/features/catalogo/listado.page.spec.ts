import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideFitstoreHttp } from 'shared';
import { provideZonelessChangeDetection } from '@angular/core';
import { ListadoPage } from './listado.page';
import { ARBOL, COLORES, TALLAS, TARJETAS, atender, pagina } from './prueba-catalogo';

async function abrir(url: string) {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([
        { path: 'productos', component: ListadoPage },
        { path: 'categoria/:slug', component: ListadoPage },
      ]),
      provideFitstoreHttp({ carrito: false }),
      provideHttpClientTesting(),
    ],
  });
  const ctrl = TestBed.inject(HttpTestingController);
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  return { ctrl, harness, el: harness.routeNativeElement as HTMLElement };
}

const MAESTROS = { '/categorias': ARBOL, '/tallas': TALLAS, '/colores': COLORES };

async function cargar(url: string, productos: unknown = pagina(TARJETAS)) {
  const t = await abrir(url);
  atender(t.ctrl, { ...MAESTROS, '/productos': productos });
  await t.harness.fixture.whenStable();
  t.harness.detectChanges();
  return t;
}

describe('ListadoPage', () => {
  it('CA-2-03 los filtros de la URL viajan a la API', async () => {
    const t = await abrir('/productos?q=polo&talla=M&color=1&min=40&max=60&disponible=true&orden=precio,asc&pagina=2');
    const reqs = t.ctrl.match((r) => r.url === '/productos');
    expect(reqs.length).toBe(1);
    const p = reqs[0].request.params;
    expect(p.get('q')).toBe('polo');
    expect(p.getAll('talla')).toEqual(['M']);
    expect(p.getAll('color')).toEqual(['1']);
    expect(p.get('precioMin')).toBe('40');
    expect(p.get('precioMax')).toBe('60');
    expect(p.get('disponible')).toBe('true');
    expect(p.get('sort')).toBe('precio,asc');
    expect(p.get('page')).toBe('1');
    expect(p.get('size')).toBe('24');
  });

  it('CA-2-03 al marcar un filtro la URL cambia y se mantiene al recargar', async () => {
    const t = await cargar('/productos');
    const casilla = t.el.querySelector<HTMLInputElement>('input[data-filtro="talla-M"]')!;
    casilla.click();
    await t.harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toContain('talla=M');
    const reqs = t.ctrl.match((r) => r.url === '/productos');
    expect(reqs[0].request.params.getAll('talla')).toEqual(['M']);
  });

  it('CA-2-03 los chips de filtros activos quitan un filtro de la URL', async () => {
    const t = await cargar('/productos?talla=M&color=1');
    const chips = t.el.querySelectorAll('.chip-filtro');
    expect(chips.length).toBe(2);
    expect(t.el.textContent).toContain('Talla M');
    expect(t.el.textContent).toContain('Negro');
    t.el.querySelector<HTMLButtonElement>('.chip-filtro button')!.click();
    await t.harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).not.toContain('talla=M');
  });

  it('CA-2-04 el orden cambia la URL y pide 24 por página', async () => {
    const t = await cargar('/productos');
    const sel = t.el.querySelector<HTMLSelectElement>('select[data-orden]')!;
    sel.value = 'precio,desc';
    sel.dispatchEvent(new Event('change'));
    await t.harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toContain('orden=precio');
    const reqs = t.ctrl.match((r) => r.url === '/productos');
    expect(reqs[0].request.params.get('sort')).toBe('precio,desc');
    expect(reqs[0].request.params.get('size')).toBe('24');
  });

  it('CA-2-04 el paginador cambia la página en la URL', async () => {
    const t = await cargar('/productos', pagina(TARJETAS, { totalPaginas: 3 }));
    t.el.querySelector<HTMLButtonElement>('button[data-pagina="1"]')!.click();
    await t.harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toContain('pagina=2');
    const reqs = t.ctrl.match((r) => r.url === '/productos');
    expect(reqs[0].request.params.get('page')).toBe('1');
  });

  it('CA-2-01 /categoria/:slug pide los productos de esa categoría', async () => {
    const t = await abrir('/categoria/leggings');
    const reqs = t.ctrl.match((r) => r.url === '/productos');
    expect(reqs[0].request.params.get('categoria')).toBe('leggings');
  });

  it('CA-2-05 la tarjeta muestra nombre, "Desde", muestras, etiqueta y "Agotado"', async () => {
    const t = await cargar('/productos');
    const tarjetas = t.el.querySelectorAll('fs-tarjeta-producto');
    expect(tarjetas.length).toBe(2);
    expect(tarjetas[0].textContent).toContain('Legging Pro');
    expect(tarjetas[0].textContent).toContain('S/ 59.90');
    expect(tarjetas[0].textContent).toContain('−10 %');
    expect(tarjetas[0].querySelectorAll('.fs-muestra').length).toBe(2);
    expect(tarjetas[1].textContent).toContain('Agotado');
  });

  it('CA-2-06 sin resultados muestra el mensaje y "Limpiar filtros" quita los filtros', async () => {
    const t = await cargar('/productos?talla=M&q=xyz', pagina([]));
    expect(t.el.textContent).toContain('No encontramos productos con estos filtros');
    const boton = Array.from(t.el.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Limpiar filtros'),
    )!;
    boton.click();
    await t.harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/productos');
  });

  it('CA-2-07 muestra el estado cargando y el error con reintentar', async () => {
    const t = await abrir('/productos');
    expect(t.el.querySelector('.fs-esqueleto')).not.toBeNull();
    atender(t.ctrl, MAESTROS, { '/productos': 500 });
    await t.harness.fixture.whenStable();
    t.harness.detectChanges();
    expect(t.el.textContent).toContain('No pudimos cargar');
    Array.from(t.el.querySelectorAll('button')).find((b) => b.textContent?.includes('Reintentar'))!.click();
    await t.harness.fixture.whenStable();
    expect(t.ctrl.match((r) => r.url === '/productos').length).toBe(1);
  });
});
