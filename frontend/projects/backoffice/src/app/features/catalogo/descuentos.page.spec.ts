import { Component } from '@angular/core';
import { DescuentosPage } from './descuentos.page';
import { armar, asentar, atender, boton } from './prueba-catalogo';

@Component({ selector: 'fs-falso', template: '' })
class Falso {}
const RUTAS = [
  { path: 'catalogo/descuentos', component: DescuentosPage },
  { path: '**', component: Falso },
];
const POLITICA = {
  activo: true,
  tramos: [
    { unidadesMinimas: 3, porcentaje: 5 },
    { unidadesMinimas: 6, porcentaje: 10 },
  ],
};

async function abrir() {
  const t = await armar(RUTAS, '/catalogo/descuentos');
  atender(t.ctrl, { '/admin/descuentos': POLITICA });
  await asentar(t.fixture);
  return t;
}

describe('DescuentosPage (P-2-06)', () => {
  it('CA-2-36 muestra la política global y la vista previa de la tienda', async () => {
    const t = await abrir();
    expect(t.el.querySelectorAll('fs-editor-tramos input[aria-label^="Unidades"]').length).toBe(2);
    expect(t.el.textContent).toContain('3 un. −5 % · 6 un. −10 %');
  });

  it('CA-2-36 guarda los tramos con PUT /admin/descuentos', async () => {
    const t = await abrir();
    boton(t.el, 'Agregar tramo')!.click();
    await asentar(t.fixture);
    boton(t.el, 'Guardar')!.click();
    await asentar(t.fixture);
    const req = t.ctrl.expectOne('/api/v1/admin/descuentos');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      activo: true,
      tramos: [
        { unidadesMinimas: 3, porcentaje: 5 },
        { unidadesMinimas: 6, porcentaje: 10 },
        { unidadesMinimas: 9, porcentaje: 15 },
      ],
    });
  });

  it('CA-2-36 con tramos inválidos no deja guardar', async () => {
    const t = await abrir();
    const porcentaje = t.el.querySelector<HTMLInputElement>(
      'input[aria-label="Porcentaje del tramo 2"]',
    )!;
    porcentaje.value = '70';
    porcentaje.dispatchEvent(new Event('input'));
    await asentar(t.fixture);
    expect(t.el.textContent).toContain('El porcentaje debe estar entre 0 y 50');
    expect((boton(t.el, 'Guardar') as HTMLButtonElement).disabled).toBe(true);
  });

  it('CA-2-37 apagar el interruptor global avisa que la tienda deja de mostrar las etiquetas', async () => {
    const t = await abrir();
    const interruptor = t.el.querySelector<HTMLInputElement>('input[name="activo"]')!;
    interruptor.click();
    await asentar(t.fixture);
    expect(t.el.textContent).toContain('la tienda dejará de mostrar las etiquetas de descuento');
    boton(t.el, 'Guardar')!.click();
    await asentar(t.fixture);
    expect(t.ctrl.expectOne('/api/v1/admin/descuentos').request.body.activo).toBe(false);
  });
});
