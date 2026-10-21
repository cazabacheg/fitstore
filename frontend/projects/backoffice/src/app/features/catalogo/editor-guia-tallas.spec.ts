import { asentar, montarComp } from './prueba-catalogo';
import { EditorGuiaTallas } from './editor-guia-tallas';

const GUIA = {
  columnas: ['Talla', 'Cintura'],
  filas: [
    ['S', '60'],
    ['M', '64'],
  ],
  nota: null,
};

describe('EditorGuiaTallas', () => {
  it('CA-2-20 agrega y quita filas', async () => {
    const { fixture, el, comp } = await montarComp(EditorGuiaTallas, { guia: GUIA });
    (
      Array.from(el.querySelectorAll('button')).find((b) =>
        b.textContent?.includes('Agregar fila'),
      ) as HTMLElement
    ).click();
    await asentar(fixture);
    expect(comp.guia().filas.length).toBe(3);
    expect(comp.guia().filas[2]).toEqual(['', '']);
    el.querySelector<HTMLButtonElement>('button[aria-label="Quitar fila 1"]')!.click();
    await asentar(fixture);
    expect(comp.guia().filas).toEqual([
      ['M', '64'],
      ['', ''],
    ]);
  });

  it('CA-2-20 agrega y quita columnas en todas las filas', async () => {
    const { fixture, el, comp } = await montarComp(EditorGuiaTallas, { guia: GUIA });
    (
      Array.from(el.querySelectorAll('button')).find((b) =>
        b.textContent?.includes('Agregar columna'),
      ) as HTMLElement
    ).click();
    await asentar(fixture);
    expect(comp.guia().columnas.length).toBe(3);
    expect(comp.guia().filas.every((f) => f.length === 3)).toBe(true);
    el.querySelector<HTMLButtonElement>('button[aria-label="Quitar columna 2"]')!.click();
    await asentar(fixture);
    expect(comp.guia().columnas).toEqual(['Talla', '']);
    expect(comp.guia().filas[0]).toEqual(['S', '']);
  });

  it('CA-2-20 editar una celda actualiza la guía', async () => {
    const { fixture, el, comp } = await montarComp(EditorGuiaTallas, { guia: GUIA });
    const celda = el.querySelector<HTMLInputElement>(
      'input[aria-label="Fila 2, columna Cintura"]',
    )!;
    celda.value = '66';
    celda.dispatchEvent(new Event('input'));
    await asentar(fixture);
    expect(comp.guia().filas[1]).toEqual(['M', '66']);
  });
});
