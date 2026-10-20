import { FsSelect } from './select';
import { montar } from '../../testing';

const OPCIONES = [
  { valor: 'S', etiqueta: 'Talla S' },
  { valor: 'M', etiqueta: 'Talla M' },
];

describe('Select de ui/', () => {
  it('CA-0b-25 lista las opciones con su placeholder y etiqueta', async () => {
    const { el } = await montar(FsSelect, {
      etiqueta: 'Talla',
      opciones: OPCIONES,
      placeholder: 'Elige una talla',
    });
    const select = el.querySelector('select')!;
    expect(el.querySelector('label')!.htmlFor).toBe(select.id);
    expect([...select.options].map((o) => o.textContent?.trim())).toEqual([
      'Elige una talla',
      'Talla S',
      'Talla M',
    ]);
  });

  it('CA-0b-25 elegir una opción actualiza el valor', async () => {
    const { el, comp, estabilizar } = await montar(FsSelect, {
      etiqueta: 'Talla',
      opciones: OPCIONES,
    });
    const select = el.querySelector('select')!;
    select.value = 'M';
    select.dispatchEvent(new Event('change'));
    await estabilizar();
    expect(comp.valor()).toBe('M');
  });

  it('CA-0b-25 con error lo marca y lo enlaza', async () => {
    const { el } = await montar(FsSelect, {
      etiqueta: 'Talla',
      opciones: OPCIONES,
      error: 'Elige una talla',
    });
    const select = el.querySelector('select')!;
    expect(select.getAttribute('aria-invalid')).toBe('true');
    expect(select.getAttribute('aria-describedby')).toBe(el.querySelector('.fs-campo__error')!.id);
  });
});
