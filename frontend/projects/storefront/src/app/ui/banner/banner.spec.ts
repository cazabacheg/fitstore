import { EstadoServicio } from 'shared';
import { BannerServicioNoDisponible } from './banner';
import { montar } from '../../testing';

describe('Banner "Servicio no disponible"', () => {
  it('CA-0b-13 no se muestra mientras el servicio responde', async () => {
    const { el } = await montar(BannerServicioNoDisponible);
    expect(el.querySelector('[role="alert"]')).toBeNull();
  });

  it('CA-0b-13 aparece cuando EstadoServicio marca 503 y desaparece al recuperarse', async () => {
    const { el, estabilizar } = await montar(BannerServicioNoDisponible);
    const estado = (await import('@angular/core/testing')).TestBed.inject(EstadoServicio);
    estado.noDisponible.set(true);
    await estabilizar();
    expect(el.querySelector('[role="alert"]')!.textContent).toContain('Servicio no disponible');
    estado.noDisponible.set(false);
    await estabilizar();
    expect(el.querySelector('[role="alert"]')).toBeNull();
  });
});
