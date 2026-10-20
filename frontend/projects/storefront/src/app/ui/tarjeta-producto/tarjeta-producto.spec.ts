import { provideRouter } from '@angular/router';
import { FsTarjetaProducto } from './tarjeta-producto';
import { montar } from '../../testing';

const PRODUCTO = {
  id: 1,
  slug: 'legging-negro',
  nombre: 'Legging negro',
  imagenUrl: null,
  precioDesde: 59.9,
  colores: [{ id: 1, nombre: 'Negro', hex: '#000000', activo: true }],
  etiquetaDescuento: '−10 %',
  agotado: false,
};

describe('Tarjeta de producto de ui/', () => {
  it('CA-0b-25 sin producto es un esqueleto', async () => {
    const { el } = await montar(FsTarjetaProducto, {}, [provideRouter([])]);
    expect(el.querySelector('.fs-esqueleto')).not.toBeNull();
    expect(el.querySelector('a')).toBeNull();
  });

  it('CA-0b-25 con producto es un enlace a /producto/:slug con nombre, precio y etiqueta', async () => {
    const { el } = await montar(FsTarjetaProducto, { producto: PRODUCTO }, [provideRouter([])]);
    expect(el.querySelector('a')!.getAttribute('href')).toBe('/producto/legging-negro');
    expect(el.textContent).toContain('Legging negro');
    expect(el.textContent).toContain('S/ 59.90');
    expect(el.textContent).toContain('−10 %');
    expect(el.querySelector('.fs-muestra')!.getAttribute('title')).toBe('Negro');
  });

  it('CA-0b-25 agotado atenúa la imagen y dice "Agotado"', async () => {
    const { el } = await montar(
      FsTarjetaProducto,
      { producto: { ...PRODUCTO, agotado: true } },
      [provideRouter([])],
    );
    expect(el.querySelector('a')!.classList).toContain('fs-tarjeta-producto--agotado');
    expect(el.textContent).toContain('Agotado');
  });
});
