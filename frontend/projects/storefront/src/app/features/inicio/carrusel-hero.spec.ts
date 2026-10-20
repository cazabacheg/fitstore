import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { montar } from '../../testing';
import { CarruselHero } from './carrusel-hero';
import { configuracionPublica } from './datos-prueba';

const IMAGENES = configuracionPublica().contenido.imagenesLanding;
const ENTRADAS = {
  imagenes: IMAGENES,
  titulo: 'Muévete con estilo',
  subtitulo: 'Hecho en Gamarra',
  ctaTexto: 'Ver novedades',
  ctaEnlace: '/productos',
};

function simularMovimientoReducido(reducido: boolean): void {
  window.matchMedia = ((consulta: string) => ({
    matches: reducido && consulta.includes('prefers-reduced-motion'),
    media: consulta,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    addListener: () => undefined,
    removeListener: () => undefined,
    dispatchEvent: () => false,
    onchange: null,
  })) as typeof window.matchMedia;
}

const imagenActiva = (el: HTMLElement) =>
  el.querySelector('[data-testid="diapositiva"][aria-hidden="false"]')?.getAttribute('data-indice');

describe('CarruselHero (P-3-01)', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    simularMovimientoReducido(false);
  });
  afterEach(() => vi.useRealTimers());

  it('CA-3-01 muestra el título, el subtítulo y el botón del hero con su enlace', async () => {
    const { el } = await montar(CarruselHero, ENTRADAS, [provideRouter([])]);
    expect(el.querySelector('h1')!.textContent).toContain('Muévete con estilo');
    expect(el.textContent).toContain('Hecho en Gamarra');
    const boton = el.querySelector('a[data-testid="hero-cta"]') as HTMLAnchorElement;
    expect(boton.textContent).toContain('Ver novedades');
    expect(boton.getAttribute('href')).toBe('/productos');
  });

  it('CA-3-01 dibuja una diapositiva por imagen, en orden, con su texto alternativo', async () => {
    const { el } = await montar(CarruselHero, ENTRADAS, [provideRouter([])]);
    const imgs = [...el.querySelectorAll('img[data-testid="imagen-hero"]')] as HTMLImageElement[];
    expect(imgs.map((i) => i.getAttribute('alt'))).toEqual(['Foto uno', 'Foto dos', 'Foto tres']);
    expect(imgs[0].getAttribute('src')).toBe('/media/tienda/landing/1.jpg');
    expect(el.querySelectorAll('[data-testid="punto"]').length).toBe(3);
  });

  it('CA-3-02 avanza solo cada 6 segundos y vuelve a la primera después de la última', async () => {
    const { el, estabilizar } = await montar(CarruselHero, ENTRADAS, [provideRouter([])]);
    expect(imagenActiva(el)).toBe('0');
    vi.advanceTimersByTime(5900);
    await estabilizar();
    expect(imagenActiva(el)).toBe('0');
    vi.advanceTimersByTime(200);
    await estabilizar();
    expect(imagenActiva(el)).toBe('1');
    vi.advanceTimersByTime(6000);
    await estabilizar();
    expect(imagenActiva(el)).toBe('2');
    vi.advanceTimersByTime(6000);
    await estabilizar();
    expect(imagenActiva(el)).toBe('0');
  });

  it('CA-3-02 se pausa al pasar el mouse y sigue al salir', async () => {
    const { el, estabilizar } = await montar(CarruselHero, ENTRADAS, [provideRouter([])]);
    const hero = el.querySelector('section') as HTMLElement;
    hero.dispatchEvent(new MouseEvent('mouseenter'));
    vi.advanceTimersByTime(20000);
    await estabilizar();
    expect(imagenActiva(el)).toBe('0');
    hero.dispatchEvent(new MouseEvent('mouseleave'));
    vi.advanceTimersByTime(6100);
    await estabilizar();
    expect(imagenActiva(el)).toBe('1');
  });

  it('CA-3-02 se pausa al enfocar un control del carrusel y sigue al salir del foco', async () => {
    const { el, estabilizar } = await montar(CarruselHero, ENTRADAS, [provideRouter([])]);
    const hero = el.querySelector('section') as HTMLElement;
    hero.dispatchEvent(new FocusEvent('focusin', { bubbles: true }));
    vi.advanceTimersByTime(20000);
    await estabilizar();
    expect(imagenActiva(el)).toBe('0');
    hero.dispatchEvent(new FocusEvent('focusout', { bubbles: true }));
    vi.advanceTimersByTime(6100);
    await estabilizar();
    expect(imagenActiva(el)).toBe('1');
  });

  it('CA-3-02 las flechas y los puntos cambian de imagen', async () => {
    const { el, estabilizar } = await montar(CarruselHero, ENTRADAS, [provideRouter([])]);
    (el.querySelector('[aria-label="Imagen siguiente"]') as HTMLButtonElement).click();
    await estabilizar();
    expect(imagenActiva(el)).toBe('1');
    (el.querySelector('[aria-label="Imagen anterior"]') as HTMLButtonElement).click();
    (el.querySelector('[aria-label="Imagen anterior"]') as HTMLButtonElement).click();
    await estabilizar();
    expect(imagenActiva(el)).toBe('2');
    (el.querySelectorAll('[data-testid="punto"]')[1] as HTMLButtonElement).click();
    await estabilizar();
    expect(imagenActiva(el)).toBe('1');
  });

  it('CA-3-02 se maneja con el teclado: flechas izquierda y derecha, Inicio y Fin', async () => {
    const { el, estabilizar } = await montar(CarruselHero, ENTRADAS, [provideRouter([])]);
    const hero = el.querySelector('section') as HTMLElement;
    hero.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
    await estabilizar();
    expect(imagenActiva(el)).toBe('1');
    hero.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', bubbles: true }));
    await estabilizar();
    expect(imagenActiva(el)).toBe('0');
    hero.dispatchEvent(new KeyboardEvent('keydown', { key: 'End', bubbles: true }));
    await estabilizar();
    expect(imagenActiva(el)).toBe('2');
    hero.dispatchEvent(new KeyboardEvent('keydown', { key: 'Home', bubbles: true }));
    await estabilizar();
    expect(imagenActiva(el)).toBe('0');
  });

  it('CA-3-02 los puntos indican la imagen actual para los lectores de pantalla', async () => {
    const { el, estabilizar } = await montar(CarruselHero, ENTRADAS, [provideRouter([])]);
    (el.querySelector('[aria-label="Imagen siguiente"]') as HTMLButtonElement).click();
    await estabilizar();
    const puntos = [...el.querySelectorAll('[data-testid="punto"]')];
    expect(puntos.map((p) => p.getAttribute('aria-current'))).toEqual([null, 'true', null]);
    expect(el.querySelector('[data-testid="posicion"]')!.textContent).toContain('Imagen 2 de 3');
  });

  it('CA-3-02 respeta prefers-reduced-motion: no avanza solo', async () => {
    simularMovimientoReducido(true);
    const { el, estabilizar } = await montar(CarruselHero, ENTRADAS, [provideRouter([])]);
    vi.advanceTimersByTime(30000);
    await estabilizar();
    expect(imagenActiva(el)).toBe('0');
    (el.querySelector('[aria-label="Imagen siguiente"]') as HTMLButtonElement).click();
    await estabilizar();
    expect(imagenActiva(el)).toBe('1');
  });

  it('CA-3-03 sin imágenes el hero usa el color primario con los textos y sin carrusel', async () => {
    const { el } = await montar(CarruselHero, { ...ENTRADAS, imagenes: [] }, [provideRouter([])]);
    expect(el.querySelector('img')).toBeNull();
    expect(el.querySelector('[data-testid="punto"]')).toBeNull();
    expect(el.querySelector('[aria-label="Imagen siguiente"]')).toBeNull();
    expect(el.querySelector('section')!.classList.contains('hero--color')).toBe(true);
    expect(el.querySelector('h1')!.textContent).toContain('Muévete con estilo');
    expect(el.querySelector('a[data-testid="hero-cta"]')).not.toBeNull();
  });

  it('CA-3-03 con una sola imagen no hay controles ni avance', async () => {
    const { el, estabilizar } = await montar(
      CarruselHero,
      { ...ENTRADAS, imagenes: [IMAGENES[0]] },
      [provideRouter([])],
    );
    vi.advanceTimersByTime(20000);
    await estabilizar();
    expect(el.querySelectorAll('img').length).toBe(1);
    expect(el.querySelector('[aria-label="Imagen siguiente"]')).toBeNull();
  });

  it('CA-3-01 sin texto de botón o sin enlace no se dibuja el botón', async () => {
    const { el } = await montar(CarruselHero, { ...ENTRADAS, ctaTexto: null }, [provideRouter([])]);
    expect(el.querySelector('a[data-testid="hero-cta"]')).toBeNull();
  });

  it('CA-3-01 un enlace https del botón abre como enlace externo', async () => {
    const { el } = await montar(CarruselHero, { ...ENTRADAS, ctaEnlace: 'https://tienda.pe/oferta' }, [
      provideRouter([]),
    ]);
    expect(el.querySelector('a[data-testid="hero-cta"]')!.getAttribute('href')).toBe(
      'https://tienda.pe/oferta',
    );
  });
});
