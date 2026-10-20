import { FsGaleria } from './galeria';
import { montar } from '../../testing';

const IMAGENES = [
  { url: '/a.jpg', alt: 'Legging negro, vista 1' },
  { url: '/b.jpg', alt: 'Legging negro, vista 2' },
  { url: '/c.jpg', alt: 'Legging negro, vista 3' },
];

describe('Galería de ui/', () => {
  it('CA-0b-25 muestra la foto principal y una miniatura por imagen', async () => {
    const { el } = await montar(FsGaleria, { imagenes: IMAGENES });
    const principal = el.querySelector('.fs-galeria__principal img') as HTMLImageElement;
    expect(principal.getAttribute('src')).toBe('/a.jpg');
    expect(principal.alt).toBe('Legging negro, vista 1');
    expect(el.querySelectorAll('.fs-galeria__miniatura')).toHaveLength(3);
    expect(el.querySelectorAll('[aria-current="true"]')).toHaveLength(1);
  });

  it('CA-0b-25 elegir una miniatura cambia la foto principal', async () => {
    const { el, estabilizar } = await montar(FsGaleria, { imagenes: IMAGENES });
    (el.querySelectorAll('.fs-galeria__miniatura')[1] as HTMLButtonElement).click();
    await estabilizar();
    expect(el.querySelector('.fs-galeria__principal img')!.getAttribute('src')).toBe('/b.jpg');
  });

  it('CA-0b-25 las flechas del teclado recorren las fotos', async () => {
    const { el, estabilizar } = await montar(FsGaleria, { imagenes: IMAGENES });
    const raiz = el.querySelector('.fs-galeria') as HTMLElement;
    raiz.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
    await estabilizar();
    expect(el.querySelector('.fs-galeria__principal img')!.getAttribute('src')).toBe('/b.jpg');
    raiz.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', bubbles: true }));
    await estabilizar();
    expect(el.querySelector('.fs-galeria__principal img')!.getAttribute('src')).toBe('/a.jpg');
  });

  it('CA-0b-25 al cambiar las imágenes (otro color) vuelve a la principal', async () => {
    const { fixture, el, estabilizar } = await montar(FsGaleria, { imagenes: IMAGENES });
    (el.querySelectorAll('.fs-galeria__miniatura')[2] as HTMLButtonElement).click();
    await estabilizar();
    fixture.componentRef.setInput('imagenes', [{ url: '/z.jpg', alt: 'Otro color' }]);
    await estabilizar();
    expect(el.querySelector('.fs-galeria__principal img')!.getAttribute('src')).toBe('/z.jpg');
  });

  it('CA-0b-25 sin imágenes muestra un marcador', async () => {
    const { el } = await montar(FsGaleria, { imagenes: [] });
    expect(el.querySelector('.fs-galeria__principal img')).toBeNull();
    expect(el.querySelector('.fs-galeria__principal')).not.toBeNull();
  });
});
