import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Component, provideZonelessChangeDetection } from '@angular/core';
import { TestBed, type ComponentFixture } from '@angular/core/testing';
import { Router, RouterOutlet, provideRouter } from '@angular/router';
import { CarritoContador, ToastService, provideFitstoreHttp } from 'shared';
import { DetallePage } from './detalle.page';
import { ARBOL, DETALLE, sobre } from './prueba-catalogo';

@Component({ imports: [RouterOutlet], template: '<router-outlet />' })
class Anfitrion {}

async function asentar(fixture: ComponentFixture<unknown>) {
  fixture.detectChanges();
  await new Promise((r) => setTimeout(r));
  fixture.detectChanges();
}

async function abrir(producto: unknown = DETALLE, estado = 200, arbol: unknown = ARBOL) {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([{ path: 'producto/:slug', component: DetallePage }]),
      provideFitstoreHttp({ carrito: true }),
      provideHttpClientTesting(),
    ],
  });
  const ctrl = TestBed.inject(HttpTestingController);
  const fixture = TestBed.createComponent(Anfitrion);
  await TestBed.inject(Router).navigateByUrl('/producto/legging-pro');
  await asentar(fixture);
  const el = fixture.nativeElement as HTMLElement;
  for (const req of ctrl.match(() => true)) {
    if (req.request.url.endsWith('/categorias')) {
      if (arbol === null)
        req.flush({ success: false, mensaje: 'x' }, { status: 500, statusText: 'x' });
      else req.flush(sobre(arbol));
    } else if (estado === 200) req.flush(sobre(producto));
    else req.flush({ success: false, mensaje: 'No existe' }, { status: estado, statusText: 'x' });
  }
  await asentar(fixture);
  return { ctrl, fixture, el };
}

const boton = (el: HTMLElement, texto: string) =>
  Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes(texto))!;
const talla = (el: HTMLElement, codigo: string) =>
  Array.from(el.querySelectorAll<HTMLButtonElement>('fs-selector-talla button')).find((b) =>
    b.textContent?.trim().startsWith(codigo),
  )!;
const imagenPrincipal = (el: HTMLElement) =>
  el.querySelector<HTMLImageElement>('.fs-galeria__principal img')?.getAttribute('src');

/** Negro: S agotada, M últimas 4, L disponible. Azul: S disponible, M últimas 2, L agotada. */
const CON_AZUL = {
  ...DETALLE,
  variantes: [
    ...DETALLE.variantes.filter((v) => v.colorId !== 2),
    { id: 201, sku: 'A-S', tallaId: 1, colorId: 2, precio: 59.9, disponibilidad: 'DISPONIBLE' },
    {
      id: 202,
      sku: 'A-M',
      tallaId: 2,
      colorId: 2,
      precio: 59.9,
      disponibilidad: 'ULTIMAS',
      ultimasUnidades: 2,
    },
    { id: 203, sku: 'A-L', tallaId: 3, colorId: 2, precio: 59.9, disponibilidad: 'AGOTADO' },
  ],
};

describe('DetallePage', () => {
  it('CA-2-08 preselecciona el primer color con stock y la galería muestra solo sus fotos', async () => {
    const { el } = await abrir();
    const colores = Array.from(el.querySelectorAll<HTMLButtonElement>('fs-selector-color button'));
    expect(
      colores.find((c) => c.getAttribute('aria-pressed') === 'true')?.getAttribute('title'),
    ).toBe('Negro');
    expect(imagenPrincipal(el)).toBe('/f/neg-1.jpg');
    const miniaturas = Array.from(el.querySelectorAll('.fs-galeria__miniatura img')).map((i) =>
      i.getAttribute('src'),
    );
    expect(miniaturas).toEqual(['/f/neg-1.jpg', '/f/neg-2.jpg']);
  });

  it('CA-2-08 al elegir otro color la galería cambia a sus fotos', async () => {
    const { el, fixture } = await abrir();
    el.querySelector<HTMLButtonElement>('fs-selector-color button[title="Azul"]')!.click();
    await asentar(fixture);
    expect(imagenPrincipal(el)).toBe('/f/azu-1.jpg');
    expect(el.querySelectorAll('.fs-galeria__miniatura').length).toBe(1);
  });

  it('CA-2-09 colores sin stock en ninguna talla se ven tachados y no se eligen', async () => {
    const { el, fixture } = await abrir();
    const blanco = el.querySelector<HTMLButtonElement>('fs-selector-color button[title="Blanco"]')!;
    expect(blanco.disabled).toBe(true);
    expect(blanco.classList).toContain('tachado');
    blanco.click();
    await asentar(fixture);
    expect(imagenPrincipal(el)).toBe('/f/neg-1.jpg');
  });

  it('CA-2-09 muestra la disponibilidad y el precio de la variante elegida', async () => {
    const { el, fixture } = await abrir();
    expect(talla(el, 'S').disabled).toBe(true);
    talla(el, 'M').click();
    await asentar(fixture);
    expect(el.textContent).toContain('¡Últimas 4 unidades!');
    expect(el.textContent).toContain('S/ 64.90');
    talla(el, 'L').click();
    await asentar(fixture);
    expect(el.textContent).toContain('Disponible');
    expect(el.textContent).toContain('S/ 59.90');
  });

  it('CA-2-09 si ningún color tiene stock el producto se muestra "Agotado"', async () => {
    const agotado = {
      ...DETALLE,
      colores: DETALLE.colores.map((c) => ({ ...c, conStock: false })),
      variantes: DETALLE.variantes.map((v) => ({
        ...v,
        disponibilidad: 'AGOTADO',
        ultimasUnidades: null,
      })),
    };
    const { el } = await abrir(agotado);
    expect(el.textContent).toContain('Agotado');
    expect(boton(el, 'Agregar al carrito').disabled).toBe(true);
  });

  it('CA-2-10 la guía de tallas se abre desde el enlace y muestra la tabla de la categoría', async () => {
    const { el, fixture, ctrl } = await abrir();
    boton(el, 'Guía de tallas').click();
    await asentar(fixture);
    const req = ctrl.expectOne('/api/v1/categorias/cortos/guia-tallas');
    req.flush(sobre({ columnas: ['Talla', 'Cintura'], filas: [['S', '60 cm']], nota: null }));
    await asentar(fixture);
    expect(el.querySelector('table')!.textContent).toContain('60 cm');
  });

  it('CA-2-10 sin guía de tallas el enlace no aparece', async () => {
    const { el } = await abrir({ ...DETALLE, tieneGuiaTallas: false });
    expect(
      Array.from(el.querySelectorAll('button')).some((b) =>
        b.textContent?.includes('Guía de tallas'),
      ),
    ).toBe(false);
  });

  it('CA-2-11 muestra los tramos de descuento y que se combinan tallas y colores', async () => {
    const { el } = await abrir();
    const texto = el.textContent!;
    expect(texto).toContain('3 un. −5 %');
    expect(texto).toContain('6 un. −10 %');
    expect(texto).toContain('12 un. −15 %');
    expect(texto).toContain('Se combinan tallas y colores');
  });

  it('CA-2-12 agrega al carrito, avisa y actualiza el contador de la cabecera', async () => {
    const { el, fixture, ctrl } = await abrir();
    talla(el, 'M').click();
    await asentar(fixture);
    el.querySelector<HTMLButtonElement>('button[aria-label="Más unidades"]')!.click();
    await asentar(fixture);
    boton(el, 'Agregar al carrito').click();
    await asentar(fixture);
    const req = ctrl.expectOne('/api/v1/carrito/items');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ varianteId: 102, cantidad: 2 });
    req.flush(
      sobre({
        lineas: [],
        avisosTramo: [],
        subtotal: 0,
        descuentoTotal: 0,
        total: 0,
        cantidadTotal: 5,
        preciosPorConfirmar: false,
        puedeIrAlCheckout: true,
      }),
    );
    await asentar(fixture);
    expect(TestBed.inject(CarritoContador).cantidad()).toBe(5);
    expect(
      TestBed.inject(ToastService)
        .toasts()
        .some((t) => t.tipo === 'exito'),
    ).toBe(true);
  });

  it('CA-2-12 la cantidad no puede superar el disponible de la variante', async () => {
    const { el, fixture } = await abrir();
    talla(el, 'M').click(); // 4 unidades
    await asentar(fixture);
    const mas = el.querySelector<HTMLButtonElement>('button[aria-label="Más unidades"]')!;
    for (let i = 0; i < 10; i++) {
      mas.click();
      await asentar(fixture);
    }
    expect(el.querySelector<HTMLInputElement>('fs-selector-cantidad input')!.value).toBe('4');
    expect(mas.disabled).toBe(true);
  });

  it('CA-2-12 el botón está deshabilitado hasta elegir una talla', async () => {
    const { el } = await abrir();
    expect(boton(el, 'Agregar al carrito').disabled).toBe(true);
  });

  it('CA-2-07 un producto inexistente o no activo muestra el 404', async () => {
    const { el, ctrl } = await abrir(null, 404);
    expect(el.textContent).toContain('No encontramos este producto');
    ctrl.verify();
  });

  it('CA-2-66 al cambiar de color la talla se conserva si ahí está disponible y el aviso se actualiza', async () => {
    const { el, fixture } = await abrir(CON_AZUL);
    talla(el, 'M').click();
    await asentar(fixture);
    expect(el.textContent).toContain('¡Últimas 4 unidades!');
    el.querySelector<HTMLButtonElement>('fs-selector-color button[title="Azul"]')!.click();
    await asentar(fixture);
    expect(talla(el, 'M').getAttribute('aria-pressed')).toBe('true');
    expect(el.textContent).toContain('¡Últimas 2 unidades!');
    expect(boton(el, 'Agregar al carrito').disabled).toBe(false);
  });

  it('CA-2-66 al cambiar de color la talla se deselecciona solo si en el nuevo color está agotada', async () => {
    const { el, fixture } = await abrir(CON_AZUL);
    talla(el, 'L').click();
    await asentar(fixture);
    el.querySelector<HTMLButtonElement>('fs-selector-color button[title="Azul"]')!.click();
    await asentar(fixture);
    expect(
      Array.from(el.querySelectorAll('fs-selector-talla button')).some(
        (b) => b.getAttribute('aria-pressed') === 'true',
      ),
    ).toBe(false);
    expect(el.textContent).toContain('Elige una talla');
    expect(boton(el, 'Agregar al carrito').disabled).toBe(true);
  });

  const tramo = (el: HTMLElement, texto: string) =>
    Array.from(el.querySelectorAll<HTMLButtonElement>('fs-tramos-descuento .tramo')).find((b) =>
      b.textContent?.includes(texto),
    )!;
  const cantidadActual = (el: HTMLElement) =>
    el.querySelector<HTMLInputElement>('fs-selector-cantidad input')!.value;

  it('CA-2-68 hacer clic en un tramo carga esa cantidad', async () => {
    const { el, fixture } = await abrir();
    talla(el, 'L').click();
    await asentar(fixture);
    tramo(el, '12 un.').click();
    await asentar(fixture);
    expect(cantidadActual(el)).toBe('12');
    expect(tramo(el, '12 un.').classList).toContain('alcanzado');
  });

  it('CA-2-69 con la talla elegida, los tramos que su stock no alcanza se ven grises', async () => {
    const { el, fixture } = await abrir();
    talla(el, 'M').click(); // 4 unidades
    await asentar(fixture);
    expect(tramo(el, '3 un.').disabled).toBe(false);
    expect(tramo(el, '6 un.').disabled).toBe(true);
    expect(tramo(el, '12 un.').classList).toContain('sin-stock');
    tramo(el, '12 un.').click();
    await asentar(fixture);
    expect(cantidadActual(el)).toBe('1');
  });

  it('CA-2-69 sin talla elegida, los tramos usan el mayor stock conocido del color', async () => {
    const pocas = {
      ...DETALLE,
      variantes: DETALLE.variantes.map((v) =>
        v.id === 103 ? { ...v, disponibilidad: 'ULTIMAS', ultimasUnidades: 5 } : v,
      ),
    };
    const { el } = await abrir(pocas);
    expect(tramo(el, '3 un.').disabled).toBe(false);
    expect(tramo(el, '6 un.').disabled).toBe(true);
  });

  it('CA-2-69 sin talla elegida y con un color de stock amplio, ningún tramo se ve gris', async () => {
    const { el } = await abrir();
    expect(tramo(el, '12 un.').disabled).toBe(false);
  });

  it('CA-2-70 el aviso "últimas unidades" mantiene su umbral y los tramos grises dicen cuáles no se alcanzan', async () => {
    const { el, fixture } = await abrir();
    talla(el, 'L').click();
    await asentar(fixture);
    expect(el.textContent).toContain('Disponible');
    expect(el.textContent).not.toContain('Últimas');
    talla(el, 'M').click();
    await asentar(fixture);
    expect(el.textContent).toContain('¡Últimas 4 unidades!');
    expect(tramo(el, '3 un.').disabled).toBe(false);
    expect(tramo(el, '6 un.').disabled).toBe(true);
  });

  const migas = (el: HTMLElement) =>
    Array.from(el.querySelectorAll('nav.migas > *:not([aria-hidden])')).map((n) => ({
      texto: n.textContent?.trim(),
      href: n.getAttribute('href'),
    }));

  it('CA-2-71 las migas muestran Productos / categoría padre / categoría / nombre con enlaces', async () => {
    const { el } = await abrir();
    expect(migas(el)).toEqual([
      { texto: 'Productos', href: '/productos' },
      { texto: 'Leggings', href: '/categoria/leggings' },
      { texto: 'Cortos', href: '/categoria/cortos' },
      { texto: 'Legging Pro', href: null },
    ]);
  });

  it('CA-2-71 en una categoría raíz las migas llevan solo esa categoría', async () => {
    const { el } = await abrir({
      ...DETALLE,
      categorias: [{ id: 10, nombre: 'Leggings', slug: 'leggings' }],
    });
    expect(migas(el).map((m) => m.texto)).toEqual(['Productos', 'Leggings', 'Legging Pro']);
  });

  it('CA-2-71 si el producto está en la padre y en una hija se usa la hija', async () => {
    const { el } = await abrir({
      ...DETALLE,
      categorias: [
        { id: 10, nombre: 'Leggings', slug: 'leggings' },
        { id: 11, nombre: 'Cortos', slug: 'cortos' },
      ],
    });
    expect(migas(el).map((m) => m.texto)).toEqual([
      'Productos',
      'Leggings',
      'Cortos',
      'Legging Pro',
    ]);
  });

  it('CA-2-71 si el árbol de categorías falla, las migas siguen con la categoría del producto', async () => {
    const { el } = await abrir(DETALLE, 200, null);
    expect(migas(el).map((m) => m.texto)).toEqual(['Productos', 'Cortos', 'Legging Pro']);
  });

  async function abrirGuia() {
    const t = await abrir();
    boton(t.el, 'Guía de tallas').click();
    await asentar(t.fixture);
    t.ctrl
      .expectOne('/api/v1/categorias/cortos/guia-tallas')
      .flush(sobre({ columnas: ['Talla', 'Cintura'], filas: [['S', '60 cm']], nota: null }));
    await asentar(t.fixture);
    return t;
  }

  it('CA-2-72 la tabla de la guía tiene bordes y los valores van centrados bajo sus columnas', async () => {
    const { el } = await abrirGuia();
    const celda = el.querySelector('table td')!;
    const cabecera = el.querySelector('table th')!;
    expect(getComputedStyle(celda).textAlign).toBe('center');
    expect(getComputedStyle(cabecera).textAlign).toBe('center');
    expect(getComputedStyle(celda).borderTopWidth).toBe('1px');
    expect(getComputedStyle(cabecera).borderTopWidth).toBe('1px');
  });

  it('CA-2-72 la guía se cierra con clic en el fondo y no con clic dentro', async () => {
    const { el, fixture } = await abrirGuia();
    el.querySelector<HTMLElement>('.fs-dialogo')!.click();
    await asentar(fixture);
    expect(el.querySelector('[role="dialog"]')).not.toBeNull();
    el.querySelector<HTMLElement>('.fs-dialogo-fondo')!.click();
    await asentar(fixture);
    expect(el.querySelector('[role="dialog"]')).toBeNull();
  });

  it('CA-2-72 la guía se cierra con Escape y con el botón Cerrar', async () => {
    const t = await abrirGuia();
    document.body.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    await asentar(t.fixture);
    expect(t.el.querySelector('[role="dialog"]')).toBeNull();
    boton(t.el, 'Guía de tallas').click();
    await asentar(t.fixture);
    boton(t.el.querySelector<HTMLElement>('[role="dialog"]')!, 'Cerrar').click();
    await asentar(t.fixture);
    expect(t.el.querySelector('[role="dialog"]')).toBeNull();
  });
});
