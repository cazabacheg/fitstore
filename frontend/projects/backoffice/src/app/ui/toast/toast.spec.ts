import { TestBed } from '@angular/core/testing';
import { EstadoServicio, ToastService } from 'shared';
import { BannerServicioNoDisponible } from './banner-servicio-no-disponible';
import { Toast } from './toast';

function montar() {
  const f = TestBed.createComponent(Toast);
  f.detectChanges();
  return { f, el: f.nativeElement as HTMLElement, servicio: TestBed.inject(ToastService) };
}
const avisos = (el: HTMLElement) => [...el.querySelectorAll<HTMLElement>('.fs-aviso')];

describe('Toast (ui)', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('CA-0b-13 muestra el mensaje de error de ToastService con role="alert"', () => {
    const { f, el, servicio } = montar();
    servicio.mostrar('error', 'No hay stock suficiente');
    f.detectChanges();
    const aviso = avisos(el)[0];
    expect(aviso.textContent).toContain('No hay stock suficiente');
    expect(aviso.getAttribute('role')).toBe('alert');
    expect(aviso.classList.contains('fs-aviso--error')).toBe(true);
  });

  it('CA-0b-25 los avisos de éxito e información usan role="status" y desaparecen a los 5 s', () => {
    const { f, el, servicio } = montar();
    servicio.mostrar('exito', 'Guardado');
    f.detectChanges();
    expect(avisos(el)[0].getAttribute('role')).toBe('status');
    vi.advanceTimersByTime(4900);
    f.detectChanges();
    expect(avisos(el).length).toBe(1);
    vi.advanceTimersByTime(200);
    f.detectChanges();
    expect(avisos(el).length).toBe(0);
    expect(servicio.toasts()).toEqual([]);
  });

  it('CA-0b-25 los avisos de error se quedan hasta cerrarlos', () => {
    const { f, el, servicio } = montar();
    servicio.mostrar('error', 'Sin conexión');
    f.detectChanges();
    vi.advanceTimersByTime(60_000);
    f.detectChanges();
    expect(avisos(el).length).toBe(1);
    (el.querySelector('button[aria-label="Cerrar aviso"]') as HTMLElement).click();
    f.detectChanges();
    expect(avisos(el).length).toBe(0);
  });

  it('CA-0b-25 no muestra más de dos avisos a la vez (los más recientes)', () => {
    const { f, el, servicio } = montar();
    servicio.mostrar('info', 'Uno');
    servicio.mostrar('info', 'Dos');
    servicio.mostrar('info', 'Tres');
    f.detectChanges();
    expect(avisos(el).map((a) => a.textContent)).toEqual([
      expect.stringContaining('Dos'),
      expect.stringContaining('Tres'),
    ]);
  });
});

describe('BannerServicioNoDisponible (ui)', () => {
  it('CA-0b-13 aparece con un 503 (EstadoServicio) y se oculta cuando el servicio vuelve', () => {
    const f = TestBed.createComponent(BannerServicioNoDisponible);
    const estado = TestBed.inject(EstadoServicio);
    f.detectChanges();
    const el = f.nativeElement as HTMLElement;
    expect(el.textContent).not.toContain('Servicio no disponible');
    estado.noDisponible.set(true);
    f.detectChanges();
    expect(el.textContent).toContain('Servicio no disponible');
    expect(el.querySelector('[role="alert"]')).not.toBeNull();
    estado.noDisponible.set(false);
    f.detectChanges();
    expect(el.textContent).not.toContain('Servicio no disponible');
  });
});
