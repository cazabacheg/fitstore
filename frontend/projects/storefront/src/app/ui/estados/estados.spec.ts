import { EstadoCargando, EstadoError, EstadoVacio } from './estados';
import { montar } from '../../testing';

describe('Estados comunes de ui/', () => {
  it('CA-0b-25 cargando es un esqueleto accesible', async () => {
    const { el } = await montar(EstadoCargando, { filas: 3 });
    expect(el.querySelectorAll('.fs-esqueleto')).toHaveLength(3);
    expect(el.querySelector('[aria-busy="true"]')).not.toBeNull();
  });

  it('CA-0b-25 vacío dice qué falta y ofrece la acción', async () => {
    const { el, comp, estabilizar } = await montar(EstadoVacio, {
      titulo: 'Tu carrito está vacío',
      texto: 'Agrega prendas para empezar',
      accion: 'Ver productos',
    });
    const emitidas: number[] = [];
    comp.accionar.subscribe(() => emitidas.push(1));
    expect(el.textContent).toContain('Tu carrito está vacío');
    (el.querySelector('button') as HTMLButtonElement).click();
    await estabilizar();
    expect(emitidas).toHaveLength(1);
  });

  it('CA-0b-25 vacío sin acción no muestra botón', async () => {
    const { el } = await montar(EstadoVacio, { titulo: 'Sin resultados' });
    expect(el.querySelector('button')).toBeNull();
  });

  it('CA-0b-25 error anuncia el problema y ofrece "Reintentar"', async () => {
    const { el, comp, estabilizar } = await montar(EstadoError, { titulo: 'No pudimos cargar' });
    const emitidas: number[] = [];
    comp.reintentar.subscribe(() => emitidas.push(1));
    expect(el.querySelector('[role="alert"]')!.textContent).toContain('No pudimos cargar');
    const boton = el.querySelector('button') as HTMLButtonElement;
    expect(boton.textContent).toContain('Reintentar');
    boton.click();
    await estabilizar();
    expect(emitidas).toHaveLength(1);
  });
});
