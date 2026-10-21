import { Component } from '@angular/core';
import type { catalogApi } from 'shared';
import { SESION_OPERADOR } from '../../testing/sesiones';
import { armar, asentar, atender, boton, pagina, sobre } from '../catalogo/prueba-catalogo';
import { InventarioPage } from './inventario.page';

@Component({ selector: 'fs-falso', template: '' })
class Falso {}
const RUTAS = [{ path: 'inventario', component: InventarioPage }, { path: 'inventario/:varianteId', component: Falso }];

const base = { productoId: 7, producto: 'Legging Pro', talla: 'S', color: 'Negro', fisico: 10, reservado: 2, minimo: 5, minimoPropio: false };
const STOCK: catalogApi.Stock[] = [
  { ...base, varianteId: 101, sku: 'LEG01-NEG-S', disponible: 8, estado: 'OK' },
  { ...base, varianteId: 102, sku: 'LEG01-AZU-S', disponible: 4, estado: 'BAJO' },
  { ...base, varianteId: 103, sku: 'LEG01-ROJ-S', fisico: 2, reservado: 2, disponible: 0, estado: 'AGOTADO' },
];
const esperar = () => new Promise((r) => setTimeout(r, 400));

async function abrir(url = '/inventario', sesion?: Parameters<typeof armar>[2]) {
  const t = await armar(RUTAS, url, sesion);
  atender(t.ctrl, { '/admin/inventario': pagina(STOCK) });
  await asentar(t.fixture);
  return t;
}
const escribir = async (raiz: ParentNode, fixture: Parameters<typeof asentar>[0], selector: string, valor: string) => {
  const campo = raiz.querySelector<HTMLInputElement>(selector)!;
  campo.value = valor;
  campo.dispatchEvent(new Event('input'));
  await asentar(fixture);
};

describe('InventarioPage (P-2-07)', () => {
  it('CA-2-44 "solo stock bajo" y "solo agotados" viajan como filtros de la API', async () => {
    const bajo = await armar(RUTAS, '/inventario?estado=BAJO');
    const r1 = bajo.ctrl.expectOne((r) => r.url === '/api/v1/admin/inventario');
    expect(r1.request.params.get('soloStockBajo')).toBe('true');
    expect(r1.request.params.get('soloAgotados')).toBeNull();
    r1.flush(sobre(pagina([STOCK[1]])));
  });

  it('CA-2-44 el filtro de agotados pide solo agotados', async () => {
    const t = await armar(RUTAS, '/inventario?estado=AGOTADO&q=LEG');
    const r = t.ctrl.expectOne((x) => x.url === '/api/v1/admin/inventario');
    expect(r.request.params.get('soloAgotados')).toBe('true');
    expect(r.request.params.get('q')).toBe('LEG');
    r.flush(sobre(pagina([STOCK[2]])));
  });

  it('CA-2-44 marca las filas por stock bajo (aviso) y agotado (error)', async () => {
    const { el } = await abrir();
    const filas = Array.from(el.querySelectorAll('tr.mat-mdc-row'));
    expect(filas[0].classList).not.toContain('fs-fila--aviso');
    expect(filas[1].classList).toContain('fs-fila--aviso');
    expect(filas[2].classList).toContain('fs-fila--error');
  });

  it('CA-2-41 el SKU es un enlace al kardex y la fila lleva "Kardex ›" y "Mínimo"', async () => {
    const { el } = await abrir();
    expect(el.querySelector<HTMLAnchorElement>('a.fs-codigo')!.getAttribute('href')).toBe('/inventario/101');
    const fila = el.querySelector('tr.mat-mdc-row')!;
    expect(Array.from(fila.querySelectorAll('button')).map((b) => b.textContent?.trim())).toEqual(['Kardex ›', 'Mínimo']);
  });

  it('CA-2-39 registra una entrada de varias líneas con su motivo', async () => {
    const t = await abrir();
    boton(t.el, 'Registrar entrada')!.click();
    await esperar();
    await asentar(t.fixture);
    const d = document.querySelector<HTMLElement>('mat-dialog-container')!;
    await escribir(d, t.fixture, 'input[aria-label="SKU de la línea 1"]', 'LEG01-NEG-S');
    await escribir(d, t.fixture, 'input[aria-label="Cantidad de la línea 1"]', '12');
    boton(d, 'Agregar línea')!.click();
    await asentar(t.fixture);
    await escribir(d, t.fixture, 'input[aria-label="SKU de la línea 2"]', 'LEG01-AZU-S');
    await escribir(d, t.fixture, 'input[aria-label="Cantidad de la línea 2"]', '6');
    await escribir(d, t.fixture, 'input[name="motivo"]', 'Compra a proveedor');
    boton(d, 'Registrar entrada')!.click();
    await esperar();
    await asentar(t.fixture);
    const req = t.ctrl.expectOne('/api/v1/admin/inventario/entradas');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      lineas: [{ varianteId: 101, cantidad: 12 }, { varianteId: 102, cantidad: 6 }],
      motivo: 'Compra a proveedor',
    });
  });

  it('CA-2-39 no deja registrar una entrada con un SKU que no existe', async () => {
    const t = await abrir();
    boton(t.el, 'Registrar entrada')!.click();
    await esperar();
    await asentar(t.fixture);
    const d = document.querySelector<HTMLElement>('mat-dialog-container')!;
    await escribir(d, t.fixture, 'input[aria-label="SKU de la línea 1"]', 'NO-EXISTE');
    await escribir(d, t.fixture, 'input[aria-label="Cantidad de la línea 1"]', '3');
    expect(d.textContent).toContain('No encontramos el SKU');
    expect((boton(d, 'Registrar entrada') as HTMLButtonElement).disabled).toBe(true);
    t.ctrl.match(() => true); // la búsqueda de SKU queda sin respuesta
  });

  it('CA-2-43 cambia el mínimo propio de un SKU', async () => {
    const t = await abrir();
    Array.from(t.el.querySelectorAll('tr.mat-mdc-row')[0].querySelectorAll('button')).find((b) => b.textContent?.includes('Mínimo'))!.click();
    await esperar();
    await asentar(t.fixture);
    const d = document.querySelector<HTMLElement>('mat-dialog-container')!;
    await escribir(d, t.fixture, 'input[name="minimo"]', '8');
    boton(d, 'Guardar')!.click();
    await esperar();
    await asentar(t.fixture);
    const req = t.ctrl.expectOne('/api/v1/admin/inventario/101/minimo');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ minimo: 8 });
  });

  it('CA-2-43 dejar el mínimo vacío vuelve al de la tienda', async () => {
    const t = await abrir();
    Array.from(t.el.querySelectorAll('tr.mat-mdc-row')[0].querySelectorAll('button')).find((b) => b.textContent?.includes('Mínimo'))!.click();
    await esperar();
    await asentar(t.fixture);
    const d = document.querySelector<HTMLElement>('mat-dialog-container')!;
    boton(d, 'Usar el de la tienda')!.click();
    await esperar();
    await asentar(t.fixture);
    expect(t.ctrl.expectOne('/api/v1/admin/inventario/101/minimo').request.body).toEqual({ minimo: null });
  });

  it('CA-2-39 el OPERADOR también registra entradas (la pantalla es de OPERADOR y ADMIN)', async () => {
    const t = await abrir('/inventario', SESION_OPERADOR);
    expect(boton(t.el, 'Registrar entrada')).toBeDefined();
  });
});
