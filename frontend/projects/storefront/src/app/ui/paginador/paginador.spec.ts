import { FsPaginador } from './paginador';
import { montar } from '../../testing';

const textos = (el: HTMLElement) =>
  [...el.querySelectorAll('button, .fs-paginador__elipsis')].map((b) => b.textContent?.trim());

describe('Paginador de ui/', () => {
  it('CA-0b-25 con una sola página no se muestra', async () => {
    const { el } = await montar(FsPaginador, { pagina: 0, totalPaginas: 1 });
    expect(el.querySelector('nav')).toBeNull();
  });

  it('CA-0b-25 marca la página actual (la API cuenta desde 0)', async () => {
    const { el } = await montar(FsPaginador, { pagina: 1, totalPaginas: 3 });
    expect(el.querySelector('[aria-current="page"]')!.textContent?.trim()).toBe('2');
  });

  it('CA-0b-25 muestra como máximo 7 botones con elipsis', async () => {
    const { el } = await montar(FsPaginador, { pagina: 9, totalPaginas: 20 });
    const botones = el.querySelectorAll('button[data-pagina]');
    expect(botones.length).toBeLessThanOrEqual(7);
    expect(textos(el)).toContain('…');
    expect(el.querySelector('[aria-current="page"]')!.textContent?.trim()).toBe('10');
  });

  it('CA-0b-25 elegir una página emite su número desde 0', async () => {
    const { el, comp, estabilizar } = await montar(FsPaginador, { pagina: 0, totalPaginas: 5 });
    const emitidas: number[] = [];
    comp.cambio.subscribe((p: number) => emitidas.push(p));
    (el.querySelectorAll('button[data-pagina]')[2] as HTMLButtonElement).click();
    await estabilizar();
    expect(emitidas).toEqual([2]);
  });

  it('CA-0b-25 anterior y siguiente respetan los extremos', async () => {
    const { el } = await montar(FsPaginador, { pagina: 0, totalPaginas: 3 });
    expect((el.querySelector('[aria-label="Página anterior"]') as HTMLButtonElement).disabled).toBe(
      true,
    );
    expect(
      (el.querySelector('[aria-label="Página siguiente"]') as HTMLButtonElement).disabled,
    ).toBe(false);
  });
});
