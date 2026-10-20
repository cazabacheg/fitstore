import { FsBadge } from './badge';
import { montar } from '../../testing';

describe('Badge de ui/', () => {
  it('CA-0b-25 es neutra por defecto', async () => {
    const { el } = await montar(FsBadge);
    expect(el.querySelector('.fs-insignia')!.className.trim()).toBe('fs-insignia');
  });

  it.each(['exito', 'aviso', 'error', 'info', 'marca', 'acento'])(
    'CA-0b-25 la variante %s usa su clase semántica',
    async (v) => {
      const { el } = await montar(FsBadge, { variante: v });
      expect(el.querySelector('.fs-insignia')!.classList).toContain(`fs-insignia--${v}`);
    },
  );
});
