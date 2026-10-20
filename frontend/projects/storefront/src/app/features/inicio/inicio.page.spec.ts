import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideFitstoreHttp } from 'shared';
import { montar } from '../../testing';
import { CATEGORIAS, configuracionPublica, destacados, respuesta } from './datos-prueba';
import { InicioPage } from './inicio.page';

function proveedores() {
  return [provideRouter([]), provideFitstoreHttp({ carrito: false }), provideHttpClientTesting()];
}

/** Monta la página y responde las tres peticiones que hace (settings, categorías y destacados). */
async function abrir(
  opciones: {
    config?: ReturnType<typeof configuracionPublica>;
    categorias?: unknown;
    destacados?: unknown;
    catalogoCaido?: boolean;
  } = {},
) {
  const montaje = await montar(InicioPage, {}, proveedores());
  const ctrl = TestBed.inject(HttpTestingController);
  ctrl
    .expectOne('/api/v1/settings/public')
    .flush(respuesta(opciones.config ?? configuracionPublica()));
  const cat = ctrl.expectOne('/api/v1/categorias');
  const des = ctrl.expectOne('/api/v1/productos/destacados');
  if (opciones.catalogoCaido) {
    cat.flush(
      { success: false, mensaje: 'caído', codigo: 'SERVICIO_NO_DISPONIBLE' },
      { status: 503, statusText: 'x' },
    );
    des.flush(
      { success: false, mensaje: 'caído', codigo: 'SERVICIO_NO_DISPONIBLE' },
      { status: 503, statusText: 'x' },
    );
  } else {
    cat.flush(respuesta(opciones.categorias ?? CATEGORIAS));
    des.flush(respuesta(opciones.destacados ?? destacados(4)));
  }
  await montaje.estabilizar();
  return montaje;
}

describe('InicioPage (P-3-01)', () => {
  beforeEach(() => localStorage.clear());

  it('CA-3-01 muestra el hero, la franja de beneficios, las categorías raíz, los destacados y la tienda física', async () => {
    const { el } = await abrir();
    expect(el.querySelector('h1')!.textContent).toContain('Muévete con estilo');
    expect(el.querySelectorAll('img[data-testid="imagen-hero"]').length).toBe(3);
    expect(el.querySelectorAll('[data-testid="beneficio"]').length).toBe(3);
    expect(el.textContent).toContain('Envíos a todo el Perú');
    expect(
      [...el.querySelectorAll('[data-testid="categoria"]')].map((c) => c.textContent?.trim()),
    ).toEqual(
      expect.arrayContaining([
        expect.stringContaining('Polos y tops'),
        expect.stringContaining('Casacas y polerones'),
      ]),
    );
    expect(el.querySelectorAll('[data-testid="destacado"]').length).toBe(4);
    expect(el.querySelector('[data-testid="tienda-fisica"]')!.textContent).toContain(
      'Jr. Gamarra 700',
    );
    expect(el.querySelector('[data-testid="tienda-fisica"]')!.textContent).toContain(
      'La Victoria, Lima, Lima',
    );
  });

  it('CA-3-01 las categorías enlazan al listado de la categoría', async () => {
    const { el } = await abrir();
    const primera = el.querySelector('a[data-testid="categoria"]') as HTMLAnchorElement;
    expect(primera.getAttribute('href')).toContain('polos-y-tops');
  });

  it('CA-3-01 la tienda física muestra el horario y el enlace de WhatsApp', async () => {
    const { el } = await abrir();
    const tienda = el.querySelector('[data-testid="tienda-fisica"]')!;
    expect(tienda.textContent).toContain('Lunes a sábado de 9:00 a 19:00');
    const wa = tienda.querySelector('a[href^="https://wa.me/"]') as HTMLAnchorElement;
    expect(wa.getAttribute('href')).toBe('https://wa.me/51987654321');
  });

  it('CA-3-01 sin tienda física no se dibuja ese bloque', async () => {
    const { el } = await abrir({ config: configuracionPublica({ tiendaFisica: null }) });
    expect(el.querySelector('[data-testid="tienda-fisica"]')).toBeNull();
  });

  it('CA-3-03 sin imágenes de landing el hero se muestra con el color primario y los textos, sin carrusel', async () => {
    const { el } = await abrir({ config: configuracionPublica({ imagenesLanding: [] }) });
    expect(el.querySelector('img[data-testid="imagen-hero"]')).toBeNull();
    expect(el.querySelector('section.hero--color')).not.toBeNull();
    expect(el.querySelector('h1')!.textContent).toContain('Muévete con estilo');
  });

  it('CA-3-04 los destacados son máximo 8, en el orden recibido, con la tarjeta de producto', async () => {
    const { el } = await abrir({ destacados: destacados(10) });
    const tarjetas = [...el.querySelectorAll('[data-testid="destacado"]')];
    expect(tarjetas.length).toBe(8);
    expect(tarjetas[0].textContent).toContain('Producto 1');
    expect(tarjetas[7].textContent).toContain('Producto 8');
    expect(tarjetas[0].querySelector('.fs-tarjeta-producto__nombre')).not.toBeNull();
    expect(tarjetas[0].getAttribute('href')).toContain('/producto/producto-1');
  });

  it('CA-3-04 sin destacados la sección no se muestra', async () => {
    const { el } = await abrir({ destacados: [] });
    expect(el.querySelector('[data-testid="seccion-destacados"]')).toBeNull();
    expect(el.querySelector('h1')).not.toBeNull();
  });

  it('CA-3-04 con el catálogo caído los destacados y las categorías se ocultan y el resto de la página se ve', async () => {
    const { el } = await abrir({ catalogoCaido: true });
    expect(el.querySelector('[data-testid="seccion-destacados"]')).toBeNull();
    expect(el.querySelector('[data-testid="seccion-categorias"]')).toBeNull();
    expect(el.querySelector('h1')!.textContent).toContain('Muévete con estilo');
    expect(el.querySelectorAll('[data-testid="beneficio"]').length).toBe(3);
    expect(el.querySelector('[data-testid="tienda-fisica"]')).not.toBeNull();
  });

  it('CA-3-15 sin beneficios la franja no se muestra, sin espacio vacío', async () => {
    const { el } = await abrir({ config: configuracionPublica({ beneficios: [] }) });
    expect(el.querySelector('[data-testid="franja-beneficios"]')).toBeNull();
  });

  it('CA-3-05 los cambios del admin se ven al recargar la landing, sin recompilar', async () => {
    const { el } = await abrir({
      config: configuracionPublica({
        heroTitulo: 'Nueva colección otoño',
        beneficios: [{ icono: '⭐', texto: 'Garantía' }],
      }),
    });
    expect(el.querySelector('h1')!.textContent).toContain('Nueva colección otoño');
    expect(el.textContent).toContain('Garantía');
    expect(el.textContent).not.toContain('Muévete con estilo');
  });

  it('CA-3-01 mientras carga la configuración se muestra un esqueleto y no se rompe si settings falla', async () => {
    const { el, estabilizar } = await montar(InicioPage, {}, proveedores());
    expect(el.querySelector('[data-testid="cargando"]')).not.toBeNull();
    const ctrl = TestBed.inject(HttpTestingController);
    ctrl
      .expectOne('/api/v1/settings/public')
      .flush({ success: false, mensaje: 'x', codigo: 'ERROR' }, { status: 500, statusText: 'x' });
    ctrl.expectOne('/api/v1/categorias').flush(respuesta([]));
    ctrl.expectOne('/api/v1/productos/destacados').flush(respuesta([]));
    await estabilizar();
    expect(el.querySelector('[data-testid="error-landing"]')).not.toBeNull();
  });
});
