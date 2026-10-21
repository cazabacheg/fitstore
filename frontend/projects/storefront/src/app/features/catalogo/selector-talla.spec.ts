import { montar } from '../../testing';
import { SelectorTalla } from './selector-talla';

const TALLAS = [
  { id: 1, codigo: 'S', orden: 1, activo: true },
  { id: 2, codigo: 'M', orden: 2, activo: true },
];

describe('SelectorTalla', () => {
  it('CA-2-09 las tallas sin stock se ven tachadas y no se pueden elegir', async () => {
    const { el, comp, estabilizar } = await montar(SelectorTalla, {
      tallas: TALLAS,
      agotadas: [1],
    });
    const [s, m] = Array.from(el.querySelectorAll('button'));
    expect(s.disabled).toBe(true);
    expect(s.classList).toContain('tachada');
    s.click();
    await estabilizar();
    expect(comp.valor()).toBeNull();
    m.click();
    await estabilizar();
    expect(comp.valor()).toBe(2);
    expect(m.getAttribute('aria-pressed')).toBe('true');
  });
});
