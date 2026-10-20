import { DialogoConfirmacion } from './dialogo-confirmacion';
import { FsDialogo } from './dialogo';
import { montar } from '../../testing';

describe('Diálogo de ui/', () => {
  it('CA-0b-25 cerrado no pinta nada', async () => {
    const { el } = await montar(FsDialogo, { titulo: 'Nueva dirección', abierto: false });
    expect(el.querySelector('[role="dialog"]')).toBeNull();
  });

  it('CA-0b-25 abierto es modal, tiene título accesible y Esc lo cierra', async () => {
    const { el, comp, estabilizar } = await montar(FsDialogo, {
      titulo: 'Nueva dirección',
      abierto: true,
    });
    const d = el.querySelector('[role="dialog"]')!;
    expect(d.getAttribute('aria-modal')).toBe('true');
    expect(el.querySelector('#' + d.getAttribute('aria-labelledby'))!.textContent).toContain(
      'Nueva dirección',
    );
    const cierres: number[] = [];
    comp.cerrado.subscribe(() => cierres.push(1));
    d.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    await estabilizar();
    expect(cierres).toEqual([1]);
  });

  it('CA-0b-25 devuelve el foco al elemento que lo abrió', async () => {
    const abridor = document.createElement('button');
    document.body.append(abridor);
    abridor.focus();
    const { fixture, estabilizar } = await montar(FsDialogo, { titulo: 'X', abierto: false });
    fixture.componentRef.setInput('abierto', true);
    await estabilizar();
    fixture.componentRef.setInput('abierto', false);
    await estabilizar();
    expect(document.activeElement).toBe(abridor);
    abridor.remove();
  });
});

describe('Diálogo de confirmación', () => {
  const entradas = {
    titulo: '¿Cancelar el pedido FS-000124?',
    texto: 'Esta acción no se puede deshacer.',
    textoConfirmar: 'Cancelar pedido',
    peligro: true,
    abierto: true,
  };

  it('CA-0b-25 muestra "Volver" a la izquierda y la acción con su verbo a la derecha', async () => {
    const { el } = await montar(DialogoConfirmacion, entradas);
    const botones = [...el.querySelectorAll('.fs-dialogo__acciones button')];
    expect(botones.map((b) => b.textContent?.trim())).toEqual(['Volver', 'Cancelar pedido']);
    expect(botones[1].classList).toContain('fs-btn--peligro');
  });

  it('CA-0b-25 confirmar y volver emiten su evento', async () => {
    const { el, comp, estabilizar } = await montar(DialogoConfirmacion, entradas);
    const eventos: string[] = [];
    comp.confirmado.subscribe(() => eventos.push('confirmado'));
    comp.cancelado.subscribe(() => eventos.push('cancelado'));
    const botones = el.querySelectorAll<HTMLButtonElement>('.fs-dialogo__acciones button');
    botones[1].click();
    botones[0].click();
    await estabilizar();
    expect(eventos).toEqual(['confirmado', 'cancelado']);
  });
});
