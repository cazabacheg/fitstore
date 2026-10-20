import { TestBed } from '@angular/core/testing';
import { ToastService } from 'shared';
import { FsToast } from './toast';
import { montar } from '../../testing';

describe('Toast de ui/', () => {
  afterEach(() => vi.useRealTimers());

  it('CA-0b-13 muestra el mensaje que llega por ToastService', async () => {
    const { el, estabilizar } = await montar(FsToast);
    TestBed.inject(ToastService).mostrar('error', 'Los precios cambiaron, revisa tu pedido');
    await estabilizar();
    const aviso = el.querySelector('.fs-aviso')!;
    expect(aviso.textContent).toContain('Los precios cambiaron, revisa tu pedido');
    expect(aviso.classList).toContain('fs-aviso--error');
    expect(aviso.getAttribute('role')).toBe('alert');
  });

  it('CA-0b-13 los de éxito e información son role=status y se van a los 5 s', async () => {
    vi.useFakeTimers();
    const { el, estabilizar } = await montar(FsToast);
    TestBed.inject(ToastService).mostrar('exito', 'Guardado');
    await estabilizar();
    expect(el.querySelector('.fs-aviso')!.getAttribute('role')).toBe('status');
    vi.advanceTimersByTime(5000);
    await estabilizar();
    expect(el.querySelector('.fs-aviso')).toBeNull();
  });

  it('CA-0b-13 los de error se quedan hasta cerrarlos', async () => {
    vi.useFakeTimers();
    const { el, estabilizar } = await montar(FsToast);
    TestBed.inject(ToastService).mostrar('error', 'Sin conexión');
    await estabilizar();
    vi.advanceTimersByTime(60000);
    await estabilizar();
    expect(el.querySelector('.fs-aviso')).not.toBeNull();
    (el.querySelector('.fs-aviso button') as HTMLButtonElement).click();
    await estabilizar();
    expect(el.querySelector('.fs-aviso')).toBeNull();
  });

  it('CA-0b-25 no muestra más de dos avisos a la vez', async () => {
    const { el, estabilizar } = await montar(FsToast);
    const servicio = TestBed.inject(ToastService);
    servicio.mostrar('info', 'Uno');
    servicio.mostrar('info', 'Dos');
    servicio.mostrar('info', 'Tres');
    await estabilizar();
    const textos = [...el.querySelectorAll('.fs-aviso')].map((a) => a.textContent);
    expect(textos).toHaveLength(2);
    expect(textos.join()).toContain('Tres');
  });
});
