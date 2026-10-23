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

  it('CA-0b-33 los avisos de éxito e información usan role="status" y desaparecen a los 3 s', () => {
    const { f, el, servicio } = montar();
    servicio.mostrar('exito', 'Guardado');
    f.detectChanges();
    expect(avisos(el)[0].getAttribute('role')).toBe('status');
    vi.advanceTimersByTime(2900);
    f.detectChanges();
    expect(avisos(el).length).toBe(1);
    vi.advanceTimersByTime(200);
    f.detectChanges();
    expect(avisos(el).length).toBe(0);
    expect(servicio.toasts()).toEqual([]);
  });

  it('CA-0b-33 los avisos de error se quedan hasta cerrarlos', () => {
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

  it('CA-0b-33 no muestra más de tres avisos a la vez (los más recientes)', () => {
    const { f, el, servicio } = montar();
    servicio.mostrar('info', 'Uno');
    servicio.mostrar('info', 'Dos');
    servicio.mostrar('info', 'Tres');
    servicio.mostrar('info', 'Cuatro');
    f.detectChanges();
    expect(avisos(el).map((a) => a.textContent)).toEqual([
      expect.stringContaining('Dos'),
      expect.stringContaining('Tres'),
      expect.stringContaining('Cuatro'),
    ]);
  });
});

describe('Toast (ui): estilo y posición', () => {
  let CSS = '';
  beforeAll(async () => {
    const modulo = 'node:fs'; // variable: el proyecto no trae @types/node
    const fs = (await import(/* @vite-ignore */ modulo)) as {
      readFileSync(ruta: string, codificacion: string): string;
    };
    const proceso = (globalThis as unknown as { process: { cwd(): string } }).process;
    CSS = fs
      .readFileSync(`${proceso.cwd()}/projects/backoffice/src/app/ui/toast/toast.css`, 'utf8')
      .replace(/\/\*[\s\S]*?\*\//g, '');
  });
  const regla = (selector: RegExp) =>
    [...CSS.matchAll(/([^{}]+)\{([^}]*)\}/g)]
      .filter((r) => selector.test(r[1]))
      .map((r) => r[2])
      .join(';');

  it('CA-0b-33 los avisos van en la esquina superior derecha, sobre el resto de la pantalla', () => {
    const contenedor = regla(/^\s*\.fs-toasts\s*$/);
    expect(contenedor).toMatch(/position:\s*fixed/);
    expect(contenedor).toMatch(/top:\s*var\(--espacio-\d\)/);
    expect(contenedor).toMatch(/right:\s*var\(--espacio-\d\)/);
    expect(contenedor).not.toMatch(/bottom:/);
    expect(contenedor).toMatch(/z-index:\s*\d{4,}/);
  });

  it('CA-0b-33 el aviso es grande (texto de 16 px, ancho mínimo) y el éxito y el error se distinguen por color de fondo', () => {
    expect(regla(/\.fs-aviso__texto/)).toMatch(/font-size:\s*16px/);
    expect(regla(/^\s*\.fs-aviso\s*$/)).toMatch(/min-width:\s*\d{3}px/);
    expect(regla(/\.fs-aviso--exito/)).toMatch(/background:\s*var\(--fs-color-exito-suave\)/);
    expect(regla(/\.fs-aviso--error/)).toMatch(/background:\s*var\(--fs-color-error-suave\)/);
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
