import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Component, provideZonelessChangeDetection } from '@angular/core';
import { TestBed, type ComponentFixture } from '@angular/core/testing';
import { Router, RouterOutlet, provideRouter } from '@angular/router';
import { CarritoContador, ToastService, provideFitstoreHttp } from 'shared';
import { DetallePage } from './detalle.page';
import { DETALLE, atender, sobre } from './prueba-catalogo';

@Component({ imports: [RouterOutlet], template: '<router-outlet />' })
class Anfitrion {}

async function asentar(fixture: ComponentFixture<unknown>) {
  fixture.detectChanges();
  await new Promise((r) => setTimeout(r));
  fixture.detectChanges();
}

async function abrir(producto: unknown = DETALLE, estado = 200) {
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
    if (estado === 200) req.flush(sobre(producto));
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

describe('DetallePage', () => {
  it('CA-2-08 preselecciona el primer color con stock y la galería muestra solo sus fotos', async () => {
    const { el } = await abrir();
    const colores = Array.from(el.querySelectorAll<HTMLButtonElement>('fs-selector-color button'));
    expect(colores.find((c) => c.getAttribute('aria-pressed') === 'true')?.getAttribute('title')).toBe('Negro');
    expect(imagenPrincipal(el)).toBe('/f/neg-1.jpg');
    const miniaturas = Array.from(el.querySelectorAll('.fs-galeria__miniatura img')).map((i) => i.getAttribute('src'));
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
      variantes: DETALLE.variantes.map((v) => ({ ...v, disponibilidad: 'AGOTADO', ultimasUnidades: null })),
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
    expect(Array.from(el.querySelectorAll('button')).some((b) => b.textContent?.includes('Guía de tallas'))).toBe(false);
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
    req.flush(sobre({ lineas: [], avisosTramo: [], subtotal: 0, descuentoTotal: 0, total: 0, cantidadTotal: 5, preciosPorConfirmar: false, puedeIrAlCheckout: true }));
    await asentar(fixture);
    expect(TestBed.inject(CarritoContador).cantidad()).toBe(5);
    expect(TestBed.inject(ToastService).toasts().some((t) => t.tipo === 'exito')).toBe(true);
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
});
