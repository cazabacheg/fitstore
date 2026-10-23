import { montar } from '../../testing';
import { TramosDescuento } from './tramos-descuento';

const TRAMOS = [
  { unidadesMinimas: 3, porcentaje: 5 },
  { unidadesMinimas: 6, porcentaje: 10 },
  { unidadesMinimas: 12, porcentaje: 15 },
];

const botones = (el: HTMLElement) => Array.from(el.querySelectorAll<HTMLButtonElement>('.tramo'));
const relleno = (b: HTMLElement) => b.style.getPropertyValue('--relleno');

describe('TramosDescuento', () => {
  it('CA-2-67 la barra se llena de color a medida que la cantidad avanza hacia los tramos', async () => {
    const { el, fixture, estabilizar } = await montar(TramosDescuento, {
      tramos: TRAMOS,
      cantidad: 1,
      maximo: 99,
    });
    expect(botones(el).map(relleno)).toEqual(['0%', '0%', '0%']);
    fixture.componentRef.setInput('cantidad', 2);
    await estabilizar();
    expect(botones(el).map(relleno)).toEqual(['50%', '0%', '0%']);
    fixture.componentRef.setInput('cantidad', 3);
    await estabilizar();
    expect(botones(el).map(relleno)).toEqual(['100%', '0%', '0%']);
    expect(botones(el)[0].classList).toContain('alcanzado');
    expect(botones(el)[1].classList).not.toContain('alcanzado');
    fixture.componentRef.setInput('cantidad', 12);
    await estabilizar();
    expect(botones(el).map(relleno)).toEqual(['100%', '100%', '100%']);
  });

  it('CA-2-68 cada tramo es un botón y al hacer clic fija esa cantidad', async () => {
    const { el, comp, estabilizar } = await montar(TramosDescuento, {
      tramos: TRAMOS,
      cantidad: 1,
      maximo: 99,
    });
    expect(botones(el)[2].textContent).toContain('12 un. −15 %');
    botones(el)[2].click();
    await estabilizar();
    expect(comp.cantidad()).toBe(12);
    botones(el)[0].click();
    await estabilizar();
    expect(comp.cantidad()).toBe(3);
  });

  it('CA-2-69 los tramos que el stock no alcanza se ven grises y no se pueden elegir', async () => {
    const { el, comp, estabilizar } = await montar(TramosDescuento, {
      tramos: TRAMOS,
      cantidad: 1,
      maximo: 4,
    });
    const [a, b, c] = botones(el);
    expect(a.disabled).toBe(false);
    expect(a.classList).not.toContain('sin-stock');
    for (const x of [b, c]) {
      expect(x.disabled).toBe(true);
      expect(x.classList).toContain('sin-stock');
    }
    c.click();
    await estabilizar();
    expect(comp.cantidad()).toBe(1);
  });

  it('CA-2-11 sin tramos no se pinta nada', async () => {
    const { el } = await montar(TramosDescuento, { tramos: [] });
    expect(el.querySelector('section')).toBeNull();
  });
});
