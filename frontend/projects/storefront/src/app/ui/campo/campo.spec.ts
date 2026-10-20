import { FsCampo } from './campo';
import { montar } from '../../testing';

describe('Campo de ui/', () => {
  it('CA-0b-25 muestra la etiqueta asociada al control y la ayuda', async () => {
    const { el } = await montar(FsCampo, { etiqueta: 'Correo', ayuda: 'Usa tu correo' });
    const input = el.querySelector('input')!;
    const etiqueta = el.querySelector('label')!;
    expect(etiqueta.textContent).toContain('Correo');
    expect(etiqueta.htmlFor).toBe(input.id);
    expect(el.textContent).toContain('Usa tu correo');
    expect(input.getAttribute('aria-invalid')).toBeNull();
  });

  it('CA-0b-25 con error marca el campo, enlaza el mensaje y lo anuncia', async () => {
    const { el } = await montar(FsCampo, { etiqueta: 'Correo', error: 'Correo inválido' });
    const input = el.querySelector('input')!;
    expect(el.querySelector('.fs-campo')!.classList).toContain('fs-campo--error');
    expect(input.getAttribute('aria-invalid')).toBe('true');
    const mensaje = el.querySelector('.fs-campo__error')!;
    expect(mensaje.textContent).toContain('Correo inválido');
    expect(input.getAttribute('aria-describedby')).toContain(mensaje.id);
  });

  it('CA-0b-25 lo escrito actualiza el valor', async () => {
    const { el, comp, estabilizar } = await montar(FsCampo, { etiqueta: 'Nombre' });
    const input = el.querySelector('input')!;
    input.value = 'Rosa';
    input.dispatchEvent(new Event('input'));
    await estabilizar();
    expect(comp.valor()).toBe('Rosa');
  });

  it('CA-0b-25 deshabilitado no permite editar', async () => {
    const { el } = await montar(FsCampo, { etiqueta: 'Nombre', deshabilitado: true });
    expect(el.querySelector('input')!.disabled).toBe(true);
  });
});
