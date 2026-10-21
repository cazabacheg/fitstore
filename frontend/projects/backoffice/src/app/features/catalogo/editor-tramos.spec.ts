import { asentar, montarComp } from './prueba-catalogo';
import { EditorTramos } from './editor-tramos';

describe('EditorTramos', () => {
  it('CA-2-36 agrega y quita tramos (hasta 10)', async () => {
    const { fixture, el, comp } = await montarComp(EditorTramos, {
      tramos: [{ unidadesMinimas: 3, porcentaje: 5 }],
    });
    const agregar = Array.from(el.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Agregar tramo'),
    )!;
    agregar.click();
    await asentar(fixture);
    expect(comp.tramos().length).toBe(2);
    el.querySelector<HTMLButtonElement>('button[aria-label="Quitar tramo 1"]')!.click();
    await asentar(fixture);
    expect(comp.tramos().length).toBe(1);
  });

  it('CA-2-36 con 10 tramos ya no deja agregar', async () => {
    const tramos = Array.from({ length: 10 }, (_, i) => ({
      unidadesMinimas: i + 2,
      porcentaje: i + 1,
    }));
    const { el } = await montarComp(EditorTramos, { tramos });
    const agregar = Array.from(el.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Agregar tramo'),
    )!;
    expect(agregar.disabled).toBe(true);
  });

  it('CA-2-36 explica por qué no son válidos: unidades no crecientes, porcentaje fuera de 0-50', async () => {
    const { el, comp } = await montarComp(EditorTramos, {
      tramos: [
        { unidadesMinimas: 6, porcentaje: 10 },
        { unidadesMinimas: 3, porcentaje: 60 },
      ],
    });
    const texto = el.textContent!;
    expect(texto).toContain('Las unidades deben ir de menor a mayor');
    expect(texto).toContain('El porcentaje debe estar entre 0 y 50');
    expect(comp.valido()).toBe(false);
  });

  it('CA-2-36 tramos correctos no muestran errores y se editan escribiendo', async () => {
    const { fixture, el, comp } = await montarComp(EditorTramos, {
      tramos: [{ unidadesMinimas: 3, porcentaje: 5 }],
    });
    const unidades = el.querySelector<HTMLInputElement>(
      'input[aria-label="Unidades del tramo 1"]',
    )!;
    unidades.value = '4';
    unidades.dispatchEvent(new Event('input'));
    await asentar(fixture);
    expect(comp.tramos()[0].unidadesMinimas).toBe(4);
    expect(comp.valido()).toBe(true);
    expect(el.querySelector('[role="alert"]')).toBeNull();
  });

  it('CA-2-37 la vista previa dice cómo lo ve la tienda', async () => {
    const { el } = await montarComp(EditorTramos, {
      tramos: [
        { unidadesMinimas: 3, porcentaje: 5 },
        { unidadesMinimas: 6, porcentaje: 10 },
      ],
    });
    expect(el.textContent).toContain('3 un. −5 % · 6 un. −10 %');
  });
});
