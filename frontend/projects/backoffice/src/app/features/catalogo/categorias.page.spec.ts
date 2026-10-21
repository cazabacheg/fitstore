import { Component } from '@angular/core';
import { ArbolCategorias } from './arbol-categorias';
import { CategoriasPage } from './categorias.page';
import { CATEGORIAS, armar, asentar, atender, boton, montarComp, sobre } from './prueba-catalogo';

@Component({ selector: 'fs-falso', template: '' })
class Falso {}
const RUTAS = [
  { path: 'catalogo/categorias', component: CategoriasPage },
  { path: '**', component: Falso },
];

async function abrir() {
  const t = await armar(RUTAS, '/catalogo/categorias');
  atender(t.ctrl, { '/admin/categorias': CATEGORIAS });
  await asentar(t.fixture);
  return t;
}
const esperarDialogo = () => new Promise((r) => setTimeout(r, 400));
const dialogo = () => document.querySelector('mat-dialog-container') as HTMLElement | null;

describe('ArbolCategorias', () => {
  it('CA-2-18 arrastrar una categoría emite su nueva posición entre sus hermanas', async () => {
    const dos = [
      ...CATEGORIAS,
      { ...CATEGORIAS[0], id: 20, nombre: 'Tops', slug: 'tops', orden: 2, hijas: [] },
    ];
    const { comp } = await montarComp(ArbolCategorias, { categorias: dos });
    const emitidos: unknown[] = [];
    comp.ordenar.subscribe((o) => emitidos.push(o));
    comp.soltar({ previousIndex: 0, currentIndex: 1 }, dos);
    expect(emitidos).toEqual([{ id: 10, orden: 1 }]);
  });

  it('CA-2-17 muestra dos niveles: raíces e hijas', async () => {
    const { el } = await montarComp(ArbolCategorias, { categorias: CATEGORIAS });
    expect(el.textContent).toContain('Leggings');
    expect(el.querySelector('[data-categoria="11"]')!.textContent).toContain('Cortos');
  });
});

describe('CategoriasPage (P-2-05)', () => {
  it('CA-2-18 al reordenar guarda con PATCH /admin/categorias/{id}/orden', async () => {
    const t = await abrir();
    const arbol = t.fixture.debugElement.query((d) => d.name === 'fs-arbol-categorias')
      .componentInstance as ArbolCategorias;
    arbol.soltar({ previousIndex: 0, currentIndex: 0 }, CATEGORIAS); // sin cambio: no llama
    t.ctrl.expectNone('/api/v1/admin/categorias/10/orden');
    arbol.soltar({ previousIndex: 0, currentIndex: 1 }, [
      CATEGORIAS[0],
      { ...CATEGORIAS[0], id: 20 },
    ]);
    await asentar(t.fixture);
    const req = t.ctrl.expectOne('/api/v1/admin/categorias/10/orden');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ orden: 1 });
  });

  it('CA-2-17 crea una categoría hija eligiendo su padre', async () => {
    const t = await abrir();
    boton(t.el, 'Nueva categoría')!.click();
    await asentar(t.fixture);
    await esperarDialogo();
    const d = dialogo()!;
    const nombre = d.querySelector<HTMLInputElement>('input[name="nombre"]')!;
    nombre.value = 'Largos';
    nombre.dispatchEvent(new Event('input'));
    const padre = d.querySelector<HTMLSelectElement>('select[name="padre"]')!;
    // Solo las raíces pueden ser padre: una hija no puede tener hijas.
    expect(Array.from(padre.options).map((o) => o.textContent?.trim())).toEqual([
      'Ninguna (raíz)',
      'Leggings',
    ]);
    padre.value = '10';
    padre.dispatchEvent(new Event('change'));
    await asentar(t.fixture);
    boton(d, 'Guardar')!.click();
    await esperarDialogo();
    await asentar(t.fixture);
    const req = t.ctrl.expectOne('/api/v1/admin/categorias');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ nombre: 'Largos', padreId: 10 });
  });

  it('CA-2-19 si la categoría tiene hijas o productos avisa y sugiere desactivarla', async () => {
    const t = await abrir();
    t.el
      .querySelector<HTMLButtonElement>(
        '[data-categoria="10"] button[aria-label="Eliminar Leggings"]',
      )!
      .click();
    await asentar(t.fixture);
    await esperarDialogo();
    document.querySelector<HTMLElement>('mat-dialog-container button.fs-btn--peligro')!.click();
    await esperarDialogo();
    await asentar(t.fixture);
    t.ctrl.expectOne('/api/v1/admin/categorias/10').flush(
      {
        success: false,
        mensaje: 'La categoría tiene hijas o productos',
        codigo: 'CATEGORIA_EN_USO',
      },
      { status: 409, statusText: 'Conflict' },
    );
    await asentar(t.fixture);
    expect(t.el.querySelector('[role="alert"]')!.textContent).toContain('desactívala');
    expect(t.el.querySelector('[role="alert"]')!.textContent).toContain('Leggings');
  });

  it('CA-2-20 al elegir una categoría con guía la carga, se edita y se guarda', async () => {
    const t = await abrir();
    t.el
      .querySelector<HTMLButtonElement>('[data-categoria="10"] button[data-seleccionar]')!
      .click();
    await asentar(t.fixture);
    t.ctrl
      .expectOne('/api/v1/categorias/leggings/guia-tallas')
      .flush(sobre({ columnas: ['Talla'], filas: [['S']], nota: null }));
    await asentar(t.fixture);
    boton(t.el, 'Agregar fila')!.click();
    await asentar(t.fixture);
    boton(t.el, 'Guardar guía')!.click();
    await asentar(t.fixture);
    const req = t.ctrl.expectOne('/api/v1/admin/categorias/10/guia-tallas');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toMatchObject({ columnas: ['Talla'], filas: [['S'], ['']] });
  });
});
