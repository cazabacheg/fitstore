import { configuracionAdmin, escribir, montarComponente, porId } from './datos-prueba';
import { ContenidoTab, type DatosContenido } from './contenido-tab';

const CONTENIDO = configuracionAdmin().contenido;

function montar(extra: Record<string, unknown> = {}) {
  return montarComponente(ContenidoTab, { contenido: CONTENIDO, ...extra });
}

async function guardar(
  el: HTMLElement,
  comp: ContenidoTab,
  estabilizar: () => Promise<void>,
): Promise<DatosContenido[]> {
  const guardadas: DatosContenido[] = [];
  comp.guardar.subscribe((d) => guardadas.push(d));
  porId<HTMLButtonElement>(el, 'guardar-contenido').click();
  await estabilizar();
  return guardadas;
}

describe('ContenidoTab (P-3-04, pestaña Contenido)', () => {
  it('CA-3-13 muestra el hero, el contacto, las redes y el horario guardados', async () => {
    const { el } = await montar();
    expect((porId(el, 'hero-titulo') as HTMLInputElement).value).toBe('Muévete con estilo');
    expect((porId(el, 'hero-cta-texto') as HTMLInputElement).value).toBe('Ver novedades');
    expect((porId(el, 'hero-cta-enlace') as HTMLInputElement).value).toBe('/productos');
    expect((porId(el, 'email') as HTMLInputElement).value).toBe('ventas@fitstore.pe');
    expect((porId(el, 'whatsapp') as HTMLInputElement).value).toBe('987654321');
    expect((porId(el, 'instagram') as HTMLInputElement).value).toBe(
      'https://instagram.com/fitstore.pe',
    );
    expect((porId(el, 'horario') as HTMLInputElement).value).toContain('Lunes a sábado');
    expect((porId(el, 'tienda-direccion') as HTMLInputElement).value).toContain('Galería Gamarra');
  });

  it('CA-3-13 guarda el hero con un enlace interno o una URL https', async () => {
    const { el, estabilizar, comp } = await montar();
    escribir(porId(el, 'hero-titulo'), 'Nueva colección');
    escribir(porId(el, 'hero-cta-enlace'), '/productos?categoria=leggings');
    await estabilizar();
    let guardadas = await guardar(el, comp as ContenidoTab, estabilizar);
    expect(guardadas[0].heroTitulo).toBe('Nueva colección');
    expect(guardadas[0].heroCtaEnlace).toBe('/productos?categoria=leggings');

    escribir(porId(el, 'hero-cta-enlace'), 'https://tienda.pe/oferta');
    await estabilizar();
    guardadas = await guardar(el, comp as ContenidoTab, estabilizar);
    expect(guardadas.at(-1)?.heroCtaEnlace).toBe('https://tienda.pe/oferta');
  });

  it('CA-3-13 un enlace que no es ruta de la tienda ni https (javascript:, http:, //) se rechaza', async () => {
    const { el, estabilizar, comp } = await montar();
    const guardadas: unknown[] = [];
    (comp as ContenidoTab).guardar.subscribe((d) => guardadas.push(d));
    for (const enlace of ['javascript:alert(1)', 'http://tienda.pe', '//malo.com', 'productos']) {
      escribir(porId(el, 'hero-cta-enlace'), enlace);
      await estabilizar();
      porId<HTMLButtonElement>(el, 'guardar-contenido').click();
      await estabilizar();
      expect(porId(el, 'hero-cta-enlace').getAttribute('aria-invalid'), enlace).toBe('true');
    }
    expect(el.textContent).toContain('Usa una ruta de la tienda (/productos) o una URL https');
    expect(guardadas).toEqual([]);
  });

  it('CA-3-13 el título del hero es obligatorio', async () => {
    const { el, estabilizar, comp } = await montar();
    escribir(porId(el, 'hero-titulo'), '');
    await estabilizar();
    expect(await guardar(el, comp as ContenidoTab, estabilizar)).toEqual([]);
    expect(porId(el, 'hero-titulo').getAttribute('aria-invalid')).toBe('true');
  });

  it('CA-3-15 se pueden agregar hasta 3 beneficios y el botón se deshabilita al llegar a 3', async () => {
    const { el, estabilizar } = await montar();
    expect(el.querySelectorAll('[data-testid="beneficio-fila"]').length).toBe(1);
    const agregar = porId<HTMLButtonElement>(el, 'agregar-beneficio');
    agregar.click();
    await estabilizar();
    agregar.click();
    await estabilizar();
    expect(el.querySelectorAll('[data-testid="beneficio-fila"]').length).toBe(3);
    expect(agregar.disabled).toBe(true);
  });

  it('CA-3-15 el texto de un beneficio admite hasta 60 caracteres y no puede quedar vacío', async () => {
    const { el, estabilizar, comp } = await montar();
    const guardadas: unknown[] = [];
    (comp as ContenidoTab).guardar.subscribe((d) => guardadas.push(d));
    escribir(el.querySelector('[data-testid="beneficio-texto"]'), 'x'.repeat(61));
    await estabilizar();
    porId<HTMLButtonElement>(el, 'guardar-contenido').click();
    await estabilizar();
    expect(el.querySelector('[data-testid="beneficio-texto"]')!.getAttribute('aria-invalid')).toBe(
      'true',
    );
    expect(guardadas).toEqual([]);

    escribir(el.querySelector('[data-testid="beneficio-texto"]'), '');
    await estabilizar();
    porId<HTMLButtonElement>(el, 'guardar-contenido').click();
    await estabilizar();
    expect(guardadas).toEqual([]);
  });

  it('CA-3-15 sin beneficios se guarda una lista vacía y avisa que la franja no se mostrará', async () => {
    const { el, estabilizar, comp } = await montar();
    (el.querySelector('[data-testid="quitar-beneficio"]') as HTMLButtonElement).click();
    await estabilizar();
    expect(el.querySelectorAll('[data-testid="beneficio-fila"]').length).toBe(0);
    expect(el.textContent).toContain('la franja no se mostrará');
    const guardadas = await guardar(el, comp as ContenidoTab, estabilizar);
    expect(guardadas[0].beneficios).toEqual([]);
  });

  it('CA-3-15 guarda los beneficios en su orden', async () => {
    const { el, estabilizar, comp } = await montar();
    porId<HTMLButtonElement>(el, 'agregar-beneficio').click();
    await estabilizar();
    const iconos = el.querySelectorAll('[data-testid="beneficio-icono"]');
    const textos = el.querySelectorAll('[data-testid="beneficio-texto"]');
    escribir(iconos[1], '↩');
    escribir(textos[1], 'Cambios hasta 30 días');
    await estabilizar();
    const guardadas = await guardar(el, comp as ContenidoTab, estabilizar);
    expect(guardadas[0].beneficios).toEqual([
      { icono: '🚚', texto: 'Envíos a todo el Perú' },
      { icono: '↩', texto: 'Cambios hasta 30 días' },
    ]);
  });

  it('CA-3-16 valida el formato del correo, del WhatsApp y de las URLs de redes (https)', async () => {
    const { el, estabilizar, comp } = await montar();
    const guardadas: unknown[] = [];
    (comp as ContenidoTab).guardar.subscribe((d) => guardadas.push(d));
    escribir(porId(el, 'email'), 'no-es-correo');
    escribir(porId(el, 'whatsapp'), '12345');
    escribir(porId(el, 'facebook'), 'http://facebook.com/tienda');
    escribir(porId(el, 'tiktok'), 'tiktok.com/@tienda');
    await estabilizar();
    porId<HTMLButtonElement>(el, 'guardar-contenido').click();
    await estabilizar();
    for (const id of ['email', 'whatsapp', 'facebook', 'tiktok']) {
      expect(porId(el, id).getAttribute('aria-invalid'), id).toBe('true');
    }
    expect(guardadas).toEqual([]);
  });

  it('CA-3-16 los campos opcionales vacíos se envían como null', async () => {
    const { el, estabilizar, comp } = await montar();
    escribir(porId(el, 'email'), '');
    escribir(porId(el, 'instagram'), '');
    escribir(porId(el, 'hero-subtitulo'), '');
    await estabilizar();
    const [datos] = await guardar(el, comp as ContenidoTab, estabilizar);
    expect(datos.email).toBeNull();
    expect(datos.instagramUrl).toBeNull();
    expect(datos.facebookUrl).toBeNull();
    expect(datos.heroSubtitulo).toBeNull();
  });

  it('CA-3-16 guarda la tienda física con su dirección y el código del distrito', async () => {
    const { el, estabilizar, comp } = await montar();
    escribir(porId(el, 'tienda-referencia'), 'Frente al mercado');
    await estabilizar();
    const [datos] = await guardar(el, comp as ContenidoTab, estabilizar);
    expect(datos.tiendaFisica).toEqual({
      direccion: 'Galería Gamarra, Jr. Gamarra 700, tienda 210',
      ubigeo: '150115',
      referencia: 'Frente al mercado',
    });
  });

  it('CA-3-16 con dirección pero sin distrito no se puede guardar la tienda física', async () => {
    const { el, estabilizar, comp } = await montar({
      contenido: { ...CONTENIDO, tiendaFisica: null },
    });
    escribir(porId(el, 'tienda-direccion'), 'Jr. Gamarra 700');
    await estabilizar();
    expect(await guardar(el, comp as ContenidoTab, estabilizar)).toEqual([]);
    expect(el.textContent).toContain('Elige el distrito');
  });

  it('CA-3-16 sin tienda física se guarda null', async () => {
    const { el, estabilizar, comp } = await montar({
      contenido: { ...CONTENIDO, tiendaFisica: null },
    });
    const [datos] = await guardar(el, comp as ContenidoTab, estabilizar);
    expect(datos.tiendaFisica).toBeNull();
  });

  it('CA-3-16 el error 400 del servidor en un campo se muestra donde corresponde', async () => {
    const { el } = await montar({ erroresServidor: { heroCtaEnlace: 'Enlace no permitido' } });
    expect(porId(el, 'hero-cta-enlace').getAttribute('aria-invalid')).toBe('true');
    expect(el.textContent).toContain('Enlace no permitido');
  });
});
