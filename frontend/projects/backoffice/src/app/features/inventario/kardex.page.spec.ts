import { Component } from '@angular/core';
import type { catalogApi } from 'shared';
import { armar, asentar, atender, boton, pagina } from '../catalogo/prueba-catalogo';
import { KardexPage } from './kardex.page';

@Component({ selector: 'fs-falso', template: '' })
class Falso {}
const RUTAS = [{ path: 'inventario/:varianteId', component: KardexPage }, { path: '**', component: Falso }];

const STOCK: catalogApi.Stock = {
  varianteId: 101, sku: 'LEG01-NEG-S', productoId: 7, producto: 'Legging Pro', talla: 'S', color: 'Negro',
  fisico: 10, reservado: 2, disponible: 8, minimo: 5, minimoPropio: false, estado: 'OK',
};
const MOVIMIENTOS: catalogApi.Movimiento[] = [
  { id: 3, tipo: 'VENTA', cantidad: -2, saldoFisico: 10, motivo: null, referencia: 'FS-000124', usuarioNombre: null, creadoEn: '2026-10-20T15:30:00Z' },
  { id: 2, tipo: 'AJUSTE', cantidad: -1, saldoFisico: 12, motivo: 'Prenda dañada', referencia: null, usuarioNombre: 'Omar Operador', creadoEn: '2026-10-19T10:00:00Z' },
  { id: 1, tipo: 'ENTRADA', cantidad: 13, saldoFisico: 13, motivo: 'Compra a proveedor', referencia: null, usuarioNombre: 'Carla Admin', creadoEn: '2026-10-18T09:00:00Z' },
];
const esperar = () => new Promise((r) => setTimeout(r, 400));

async function abrir() {
  const t = await armar(RUTAS, '/inventario/101');
  atender(t.ctrl, { '/admin/inventario/101/movimientos': { stock: STOCK, movimientos: pagina(MOVIMIENTOS) } });
  await asentar(t.fixture);
  return t;
}
const escribir = async (raiz: ParentNode, fixture: Parameters<typeof asentar>[0], selector: string, valor: string) => {
  const campo = raiz.querySelector<HTMLInputElement>(selector)!;
  campo.value = valor;
  campo.dispatchEvent(new Event('input'));
  await asentar(fixture);
};

describe('KardexPage (P-2-08)', () => {
  it('CA-2-41 muestra los movimientos paginados con tipo, cantidad, saldo, motivo y usuario o "sistema"', async () => {
    const { el } = await abrir();
    const filas = Array.from(el.querySelectorAll('tr.mat-mdc-row')).map((f) => f.textContent!);
    expect(filas.length).toBe(3);
    expect(filas[0]).toContain('Venta');
    expect(filas[0]).toContain('−2');
    expect(filas[0]).toContain('sistema');
    expect(filas[1]).toContain('Prenda dañada');
    expect(filas[1]).toContain('Omar Operador');
    expect(filas[2]).toContain('+13');
    expect(el.textContent).toContain('LEG01-NEG-S');
  });

  it('CA-2-41 las filas no son clicables y el código de pedido enlaza a /pedidos?codigo=', async () => {
    const { el } = await abrir();
    expect(el.querySelector('tr.fs-fila--clicable')).toBeNull();
    const enlace = el.querySelector<HTMLAnchorElement>('a[href^="/pedidos"]')!;
    expect(enlace.textContent).toContain('FS-000124');
    expect(enlace.getAttribute('href')).toBe('/pedidos?codigo=FS-000124');
  });

  it('CA-2-40 un ajuste negativo sin motivo no se puede registrar', async () => {
    const t = await abrir();
    boton(t.el, 'Registrar ajuste')!.click();
    await esperar();
    await asentar(t.fixture);
    const d = document.querySelector<HTMLElement>('mat-dialog-container')!;
    await escribir(d, t.fixture, 'input[name="cantidad"]', '-2');
    expect((boton(d, 'Registrar ajuste') as HTMLButtonElement).disabled).toBe(true);
    expect(d.textContent).toContain('Escribe el motivo');
  });

  it('CA-2-40 un ajuste que dejaría el físico por debajo de lo reservado se bloquea con su motivo', async () => {
    const t = await abrir();
    boton(t.el, 'Registrar ajuste')!.click();
    await esperar();
    await asentar(t.fixture);
    const d = document.querySelector<HTMLElement>('mat-dialog-container')!;
    await escribir(d, t.fixture, 'input[name="cantidad"]', '-9'); // físico 10, reservado 2
    await escribir(d, t.fixture, 'input[name="motivo"]', 'Conteo');
    expect(d.textContent).toContain('dejaría el físico por debajo de lo reservado');
    expect((boton(d, 'Registrar ajuste') as HTMLButtonElement).disabled).toBe(true);
  });

  it('CA-2-40 registra un ajuste válido con su motivo y recarga el kardex', async () => {
    const t = await abrir();
    boton(t.el, 'Registrar ajuste')!.click();
    await esperar();
    await asentar(t.fixture);
    const d = document.querySelector<HTMLElement>('mat-dialog-container')!;
    await escribir(d, t.fixture, 'input[name="cantidad"]', '-2');
    await escribir(d, t.fixture, 'input[name="motivo"]', 'Prenda dañada');
    boton(d, 'Registrar ajuste')!.click();
    await esperar();
    await asentar(t.fixture);
    const req = t.ctrl.expectOne('/api/v1/admin/inventario/ajustes');
    expect(req.request.body).toEqual({ varianteId: 101, cantidad: -2, motivo: 'Prenda dañada' });
  });
});
