import { COLORES, TALLAS, montarComp, asentar } from './prueba-catalogo';
import { GeneradorMatriz } from './generador-matriz';

const marcar = async (
  fixture: Parameters<typeof asentar>[0],
  el: HTMLElement,
  selector: string,
) => {
  el.querySelectorAll<HTMLInputElement>(selector).forEach((c) => c.click());
  await asentar(fixture);
};

describe('GeneradorMatriz', () => {
  it('CA-2-22 con 5 tallas y 3 colores propone crear 15 variantes y emite los ids elegidos', async () => {
    const { fixture, el, comp } = await montarComp(GeneradorMatriz, {
      tallas: TALLAS,
      colores: COLORES,
      existentes: [],
    });
    const emitidos: unknown[] = [];
    comp.generar.subscribe((m) => emitidos.push(m));
    await marcar(fixture, el, 'input[data-talla]');
    await marcar(fixture, el, 'input[data-color]');
    const boton = Array.from(el.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Generar 15 variantes'),
    )!;
    expect(boton).toBeDefined();
    boton.click();
    expect(emitidos).toEqual([{ tallaIds: [1, 2, 3, 4, 5], colorIds: [1, 2, 3] }]);
  });

  it('CA-2-22 omite en la cuenta las combinaciones que ya existían', async () => {
    const existentes = [
      { tallaId: 1, colorId: 1 },
      { tallaId: 2, colorId: 1 },
    ];
    const { fixture, el } = await montarComp(GeneradorMatriz, {
      tallas: TALLAS,
      colores: COLORES,
      existentes,
    });
    await marcar(fixture, el, 'input[data-talla]');
    await marcar(fixture, el, 'input[data-color]');
    expect(el.textContent).toContain('Generar 13 variantes');
    expect(el.textContent).toContain('2 ya existen');
  });

  it('CA-2-22 sin tallas o sin colores marcados el botón está deshabilitado', async () => {
    const { el } = await montarComp(GeneradorMatriz, {
      tallas: TALLAS,
      colores: COLORES,
      existentes: [],
    });
    expect(
      el.querySelector<HTMLButtonElement>('button[type="button"].fs-btn--primario')!.disabled,
    ).toBe(true);
  });
});
