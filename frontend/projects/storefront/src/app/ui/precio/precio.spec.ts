import { FsPrecio } from './precio';
import { montar } from '../../testing';

describe('Precio de ui/', () => {
  it('CA-0b-25 muestra el monto en soles con dos decimales', async () => {
    const { el } = await montar(FsPrecio, { monto: 59.9 });
    expect(el.querySelector('.fs-precio__actual')!.textContent?.trim()).toBe('S/ 59.90');
    expect(el.querySelector('.fs-precio__antes')).toBeNull();
  });

  it('CA-0b-25 con descuento muestra el original tachado y la insignia', async () => {
    const { el } = await montar(FsPrecio, { monto: 53.91, antes: 59.9, descuento: '−10 %' });
    expect(el.querySelector('.fs-precio__antes')!.textContent?.trim()).toBe('S/ 59.90');
    expect(el.querySelector('.fs-insignia')!.textContent).toContain('−10 %');
  });

  it('CA-0b-27 redondea half-up como el backend', async () => {
    const { el } = await montar(FsPrecio, { monto: 332.456 });
    expect(el.querySelector('.fs-precio__actual')!.textContent?.trim()).toBe('S/ 332.46');
  });
});
