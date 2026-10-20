import { FsCheckbox } from './checkbox';
import { montar } from '../../testing';

describe('Checkbox de ui/', () => {
  it('CA-0b-25 muestra su etiqueta y alterna el valor', async () => {
    const { el, comp, estabilizar } = await montar(FsCheckbox, { etiqueta: 'Acepto los términos' });
    const input = el.querySelector('input')!;
    expect(input.type).toBe('checkbox');
    expect(el.textContent).toContain('Acepto los términos');
    input.click();
    await estabilizar();
    expect(comp.marcado()).toBe(true);
    input.click();
    await estabilizar();
    expect(comp.marcado()).toBe(false);
  });

  it('CA-0b-25 deshabilitado no se puede marcar', async () => {
    const { el } = await montar(FsCheckbox, { etiqueta: 'Recibir ofertas', deshabilitado: true });
    expect(el.querySelector('input')!.disabled).toBe(true);
  });
});
