import { Component } from '@angular/core';
import { MaestrosPage } from './maestros.page';
import { COLORES, TALLAS, armar, asentar, atender, boton, sobre } from './prueba-catalogo';
import { ListaTallas } from './lista-tallas';

@Component({ selector: 'fs-falso', template: '' })
class Falso {}
const RUTAS = [
  { path: 'catalogo/maestros', component: MaestrosPage },
  { path: '**', component: Falso },
];
const esperar = () => new Promise((r) => setTimeout(r, 400));

async function abrir() {
  const t = await armar(RUTAS, '/catalogo/maestros');
  atender(t.ctrl, { '/admin/tallas': TALLAS, '/admin/colores': COLORES });
  await asentar(t.fixture);
  return t;
}
const escribir = async (t: Awaited<ReturnType<typeof abrir>>, selector: string, valor: string) => {
  const campo = t.el.querySelector<HTMLInputElement>(selector)!;
  campo.value = valor;
  campo.dispatchEvent(new Event('input'));
  await asentar(t.fixture);
};

describe('ListaTallas y ListaColores (P-2-06)', () => {
  it('CA-2-33 crea una talla con el siguiente orden', async () => {
    const t = await abrir();
    await escribir(t, 'input[name="nuevaTalla"]', 'XXL');
    boton(t.el, 'Agregar talla')!.click();
    await asentar(t.fixture);
    const req = t.ctrl.expectOne('/api/v1/admin/tallas');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ codigo: 'XXL', orden: 6 });
  });

  it('CA-2-33 no deja repetir una talla sin importar mayúsculas', async () => {
    const t = await abrir();
    await escribir(t, 'input[name="nuevaTalla"]', 'xl');
    expect(t.el.textContent).toContain('Ya existe la talla XL');
    expect((boton(t.el, 'Agregar talla') as HTMLButtonElement).disabled).toBe(true);
  });

  it('CA-2-33 el color exige un hex válido #RRGGBB', async () => {
    const t = await abrir();
    await escribir(t, 'input[name="nuevoColor"]', 'Verde');
    await escribir(t, 'input[name="hexColor"]', 'verde');
    expect(t.el.textContent).toContain('Usa el formato #RRGGBB');
    expect((boton(t.el, 'Agregar color') as HTMLButtonElement).disabled).toBe(true);
    await escribir(t, 'input[name="hexColor"]', '#16a34a');
    const agregar = boton(t.el, 'Agregar color') as HTMLButtonElement;
    expect(agregar.disabled).toBe(false);
    agregar.click();
    await asentar(t.fixture);
    expect(t.ctrl.expectOne('/api/v1/admin/colores').request.body).toEqual({
      nombre: 'Verde',
      hex: '#16a34a',
    });
  });

  it('CA-2-33 no deja repetir un color por nombre', async () => {
    const t = await abrir();
    await escribir(t, 'input[name="nuevoColor"]', 'negro');
    expect(t.el.textContent).toContain('Ya existe el color Negro');
  });

  it('CA-2-34 eliminar una talla en uso avisa que la usa alguna variante', async () => {
    const t = await abrir();
    t.el.querySelector<HTMLButtonElement>('button[aria-label="Eliminar talla XL"]')!.click();
    await esperar();
    document.querySelector<HTMLElement>('mat-dialog-container button.fs-btn--peligro')!.click();
    await esperar();
    await asentar(t.fixture);
    t.ctrl
      .expectOne('/api/v1/admin/tallas/5')
      .flush(
        { success: false, mensaje: 'En uso', codigo: 'EN_USO' },
        { status: 409, statusText: 'Conflict' },
      );
    await asentar(t.fixture);
    expect(t.el.querySelector('[role="alert"]')!.textContent).toContain('la usa alguna variante');
  });

  it('CA-2-34 una talla sin uso se elimina y desaparece de la lista', async () => {
    const t = await abrir();
    t.el.querySelector<HTMLButtonElement>('button[aria-label="Eliminar color Rojo"]')!.click();
    await esperar();
    document.querySelector<HTMLElement>('mat-dialog-container button.fs-btn--peligro')!.click();
    await esperar();
    await asentar(t.fixture);
    t.ctrl.expectOne('/api/v1/admin/colores/3').flush(sobre(null));
    await asentar(t.fixture);
    expect(t.el.textContent).not.toContain('Rojo');
  });

  it('CA-2-35 reordenar las tallas guarda el nuevo orden de las que cambiaron', async () => {
    const t = await abrir();
    const lista = t.fixture.debugElement.query((d) => d.name === 'fs-lista-tallas')
      .componentInstance as ListaTallas;
    lista.soltar({ previousIndex: 0, currentIndex: 1 }); // XS pasa después de S
    await asentar(t.fixture);
    const a = t.ctrl.expectOne('/api/v1/admin/tallas/1');
    const b = t.ctrl.expectOne('/api/v1/admin/tallas/2');
    expect(a.request.method).toBe('PUT');
    expect(a.request.body).toEqual({ codigo: 'XS', orden: 2, activo: true });
    expect(b.request.body).toEqual({ codigo: 'S', orden: 1, activo: true });
    t.ctrl.verify();
  });
});
