import { configuracionAdmin, escribir, montarComponente, porId } from './datos-prueba';
import { IdentidadTab } from './identidad-tab';

const IDENTIDAD = configuracionAdmin().identidad;

function montar(extra: Record<string, unknown> = {}) {
  return montarComponente(IdentidadTab, { identidad: IDENTIDAD, ...extra });
}

const archivo = (nombre: string, tipo: string, bytes = 1000) =>
  new File([new Uint8Array(bytes)], nombre, { type: tipo });

async function elegir(el: HTMLElement, id: string, f: File, estabilizar: () => Promise<void>) {
  const entrada = porId<HTMLInputElement>(el, id);
  Object.defineProperty(entrada, 'files', { value: [f], configurable: true });
  entrada.dispatchEvent(new Event('change', { bubbles: true }));
  await estabilizar();
}

describe('IdentidadTab (P-3-04, pestaña Identidad)', () => {
  it('CA-3-06 muestra el nombre, el eslogan, la razón social, el RUC y la dirección fiscal guardados', async () => {
    const { el } = await montar();
    expect((porId(el, 'nombre') as HTMLInputElement).value).toBe('FitStore');
    expect((porId(el, 'eslogan') as HTMLInputElement).value).toBe(
      'Ropa deportiva hecha en Gamarra.',
    );
    expect((porId(el, 'razon-social') as HTMLInputElement).value).toBe('Moda Activa S.A.C.');
    expect((porId(el, 'ruc') as HTMLInputElement).value).toBe('20123456789');
    expect((porId(el, 'direccion-fiscal') as HTMLInputElement).value).toContain('Gamarra 700');
  });

  it('CA-3-06 guardar emite los datos editados', async () => {
    const { el, estabilizar, comp } = await montar();
    const guardadas: unknown[] = [];
    (comp as IdentidadTab).guardar.subscribe((d) => guardadas.push(d));
    escribir(porId(el, 'nombre'), 'Moda Activa');
    escribir(porId(el, 'ruc'), '20987654321');
    await estabilizar();
    porId<HTMLButtonElement>(el, 'guardar-identidad').click();
    expect(guardadas).toEqual([
      {
        nombre: 'Moda Activa',
        eslogan: 'Ropa deportiva hecha en Gamarra.',
        razonSocial: 'Moda Activa S.A.C.',
        ruc: '20987654321',
        direccionFiscal: 'Jr. Gamarra 700, tienda 210, La Victoria, Lima',
      },
    ]);
  });

  it('CA-3-06 un RUC que no tiene 11 dígitos se marca en su campo y no se envía', async () => {
    const { el, estabilizar, comp } = await montar();
    const guardadas: unknown[] = [];
    (comp as IdentidadTab).guardar.subscribe((d) => guardadas.push(d));
    for (const ruc of ['123', '2012345678a', '201234567890']) {
      escribir(porId(el, 'ruc'), ruc);
      await estabilizar();
      porId<HTMLButtonElement>(el, 'guardar-identidad').click();
      await estabilizar();
      expect(porId(el, 'ruc').getAttribute('aria-invalid')).toBe('true');
      expect(el.textContent).toContain('El RUC debe tener 11 dígitos');
    }
    expect(guardadas).toEqual([]);
  });

  it('CA-3-06 el 400 del servidor por RUC se muestra en el campo RUC', async () => {
    const { el } = await montar({ erroresServidor: { ruc: 'El RUC debe tener 11 dígitos' } });
    expect(porId(el, 'ruc').getAttribute('aria-invalid')).toBe('true');
    expect(el.textContent).toContain('El RUC debe tener 11 dígitos');
  });

  it('CA-3-06 el nombre y la razón social son obligatorios', async () => {
    const { el, estabilizar, comp } = await montar();
    const guardadas: unknown[] = [];
    (comp as IdentidadTab).guardar.subscribe((d) => guardadas.push(d));
    escribir(porId(el, 'nombre'), '');
    escribir(porId(el, 'razon-social'), '');
    await estabilizar();
    porId<HTMLButtonElement>(el, 'guardar-identidad').click();
    await estabilizar();
    expect(porId(el, 'nombre').getAttribute('aria-invalid')).toBe('true');
    expect(porId(el, 'razon-social').getAttribute('aria-invalid')).toBe('true');
    expect(guardadas).toEqual([]);
  });

  it('CA-3-07 el logo claro en PNG se acepta y se envía para subir', async () => {
    const { el, estabilizar, comp } = await montar();
    const subidas: { tipo: string; archivo: File }[] = [];
    (comp as IdentidadTab).subirImagen.subscribe((s) => subidas.push(s));
    const png = archivo('logo.png', 'image/png');
    await elegir(el, 'archivo-logo-claro', png, estabilizar);
    expect(subidas).toEqual([{ tipo: 'LOGO_CLARO', archivo: png }]);
  });

  it('CA-3-07 el logo oscuro acepta WebP y el favicon acepta ICO', async () => {
    const { el, estabilizar, comp } = await montar();
    const subidas: { tipo: string }[] = [];
    (comp as IdentidadTab).subirImagen.subscribe((s) => subidas.push(s));
    await elegir(el, 'archivo-logo-oscuro', archivo('logo.webp', 'image/webp'), estabilizar);
    await elegir(el, 'archivo-favicon', archivo('favicon.ico', 'image/x-icon'), estabilizar);
    expect(subidas.map((s) => s.tipo)).toEqual(['LOGO_OSCURO', 'FAVICON']);
  });

  it('CA-3-07 un SVG o un archivo de otro tipo se rechaza con un mensaje y no se envía', async () => {
    const { el, estabilizar, comp } = await montar();
    const subidas: unknown[] = [];
    (comp as IdentidadTab).subirImagen.subscribe((s) => subidas.push(s));
    await elegir(el, 'archivo-logo-claro', archivo('logo.svg', 'image/svg+xml'), estabilizar);
    expect(porId(el, 'rechazo-logo-claro').textContent).toContain('PNG o WebP');
    await elegir(el, 'archivo-logo-oscuro', archivo('logo.jpg', 'image/jpeg'), estabilizar);
    expect(porId(el, 'rechazo-logo-oscuro').textContent).toContain('PNG o WebP');
    await elegir(el, 'archivo-logo-claro', archivo('logo.ico', 'image/x-icon'), estabilizar);
    expect(subidas).toEqual([]);
  });

  it('CA-3-07 un archivo de más de 2 MB se rechaza', async () => {
    const { el, estabilizar, comp } = await montar();
    const subidas: unknown[] = [];
    (comp as IdentidadTab).subirImagen.subscribe((s) => subidas.push(s));
    await elegir(
      el,
      'archivo-favicon',
      archivo('favicon.png', 'image/png', 2 * 1024 * 1024 + 1),
      estabilizar,
    );
    expect(porId(el, 'rechazo-favicon').textContent).toContain('2 MB');
    expect(subidas).toEqual([]);
  });

  it('CA-3-07 muestra el logo actual cuando existe', async () => {
    const { el } = await montar({
      identidad: { ...IDENTIDAD, logoClaroUrl: '/media/tienda/logo_claro/a.png' },
    });
    expect(porId(el, 'logo-claro-actual').getAttribute('src')).toBe(
      '/media/tienda/logo_claro/a.png',
    );
  });
});
