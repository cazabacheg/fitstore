import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { errorApi, escribir, montarComponente, porId, respuesta } from './datos-prueba';
import { PoliticasPage } from './politicas.page';

const VERSIONES = [
  { version: 2, publicadaEn: '2026-10-19T15:00:00Z', publicadaPor: 'admin', vigente: true },
  { version: 1, publicadaEn: '2026-09-01T15:00:00Z', publicadaPor: null, vigente: false },
];
const TEXTO =
  '# Términos y condiciones\n\n' + 'Texto vigente de los términos de la tienda. '.repeat(3);

const politica = (tipo: string, version: number, contenido = TEXTO) => ({
  tipo,
  version,
  contenido,
  publicadaEn: '2026-10-19T15:00:00Z',
});

/** Monta la pantalla y responde la política vigente y su historial. */
async function abrir(tipo = 'terminos') {
  const montaje = await montarComponente(PoliticasPage);
  const ctrl = TestBed.inject(HttpTestingController);
  ctrl.expectOne(`/api/v1/settings/politicas/${tipo}`).flush(respuesta(politica(tipo, 2)));
  ctrl.expectOne(`/api/v1/admin/settings/politicas/${tipo}/versiones`).flush(respuesta(VERSIONES));
  await montaje.estabilizar();
  return { ...montaje, ctrl };
}

describe('PoliticasPage (P-3-05)', () => {
  it('CA-3-20 carga el texto vigente en el editor y el historial de versiones', async () => {
    const { el } = await abrir();
    expect((porId(el, 'editor') as HTMLTextAreaElement).value).toBe(TEXTO);
    const filas = el.querySelectorAll('[data-testid="version-fila"]');
    expect(filas.length).toBe(2);
    expect(filas[0].textContent).toContain('Versión 2');
    expect(filas[0].textContent).toContain('Vigente');
    expect(filas[1].textContent).toContain('Versión 1');
    expect(filas[1].textContent).not.toContain('Vigente');
  });

  it('CA-3-20 muestra las cuatro políticas para elegir', async () => {
    const { el } = await abrir();
    for (const t of ['terminos', 'privacidad', 'cambios-devoluciones', 'envios']) {
      expect(el.querySelector(`[data-testid="tipo-${t}"]`), t).not.toBeNull();
    }
  });

  it('CA-3-20 elegir otra política carga su texto y su historial', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    porId<HTMLButtonElement>(el, 'tipo-privacidad').click();
    await estabilizar();
    ctrl
      .expectOne('/api/v1/settings/politicas/privacidad')
      .flush(respuesta(politica('privacidad', 1, '# Privacidad\n\n' + 'x'.repeat(80))));
    ctrl
      .expectOne('/api/v1/admin/settings/politicas/privacidad/versiones')
      .flush(respuesta([VERSIONES[1]]));
    await estabilizar();
    expect((porId(el, 'editor') as HTMLTextAreaElement).value).toContain('# Privacidad');
    expect(el.querySelectorAll('[data-testid="version-fila"]').length).toBe(1);
  });

  it('CA-3-20 la vista previa muestra el Markdown ya formateado mientras se escribe', async () => {
    const { el, estabilizar } = await abrir();
    escribir(
      porId(el, 'editor'),
      '# Nuevo título\n\nTexto con **negrita** y una lista:\n\n- uno\n- dos\n\n' +
        'relleno '.repeat(10),
    );
    await estabilizar();
    const previa = porId(el, 'vista-previa');
    expect(previa.querySelector('h1')!.textContent).toBe('Nuevo título');
    expect(previa.querySelector('strong')!.textContent).toBe('negrita');
    expect(previa.querySelectorAll('li').length).toBe(2);
  });

  it('CA-3-21 la vista previa pasa por DOMPurify: un <script> o un onerror= no llegan al DOM', async () => {
    (window as unknown as Record<string, unknown>)['__xss'] = undefined;
    const { el, estabilizar } = await abrir();
    escribir(
      porId(el, 'editor'),
      '# Aviso\n\n<script>window.__xss = 1</script>\n\n<img src="x" onerror="window.__xss = 2">\n\n[clic](javascript:window.__xss=3)\n\nTexto legítimo ' +
        'relleno '.repeat(8),
    );
    await estabilizar();
    const previa = porId(el, 'vista-previa');
    expect(previa.querySelector('script')).toBeNull();
    expect(previa.innerHTML).not.toContain('onerror');
    expect(previa.innerHTML.toLowerCase()).not.toContain('javascript:');
    expect(previa.textContent).toContain('Texto legítimo');
    previa.querySelectorAll<HTMLElement>('a').forEach((a) => a.click());
    expect((window as unknown as Record<string, unknown>)['__xss']).toBeUndefined();
  });

  it('CA-3-20 publicar crea la versión siguiente con el texto escrito y actualiza el historial', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    const nuevo = '# Términos 2\n\n' + 'Texto nuevo de los términos. '.repeat(4);
    escribir(porId(el, 'editor'), nuevo);
    await estabilizar();
    porId<HTMLButtonElement>(el, 'publicar').click();
    await estabilizar();

    const post = ctrl.expectOne('/api/v1/admin/settings/politicas/terminos');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ contenido: nuevo });
    post.flush(respuesta(politica('terminos', 3, nuevo)), { status: 201, statusText: 'Created' });
    ctrl
      .expectOne('/api/v1/admin/settings/politicas/terminos/versiones')
      .flush(
        respuesta([
          { version: 3, publicadaEn: '2026-10-20T15:00:00Z', publicadaPor: 'admin', vigente: true },
          ...VERSIONES.map((v) => ({ ...v, vigente: false })),
        ]),
      );
    await estabilizar();
    expect(porId(el, 'mensaje-publicada').textContent).toContain('versión 3');
    expect(el.querySelectorAll('[data-testid="version-fila"]').length).toBe(3);
  });

  it('CA-3-20 el texto debe tener entre 50 y 50 000 caracteres para publicar', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    escribir(porId(el, 'editor'), 'muy corto');
    await estabilizar();
    expect(porId<HTMLButtonElement>(el, 'publicar').disabled).toBe(true);
    expect(el.textContent).toContain('al menos 50 caracteres');
    porId<HTMLButtonElement>(el, 'publicar').click();
    ctrl.expectNone('/api/v1/admin/settings/politicas/terminos');

    escribir(porId(el, 'editor'), 'x'.repeat(50_001));
    await estabilizar();
    expect(porId<HTMLButtonElement>(el, 'publicar').disabled).toBe(true);
  });

  it('CA-3-20 explica que las versiones anteriores no se editan y que el texto se guarda tal cual', async () => {
    const { el } = await abrir();
    expect(el.textContent).toContain('las anteriores no se pueden editar');
  });

  it('CA-3-20 cada versión del historial tiene un enlace "ver" que abre la política de la tienda en otra pestaña', async () => {
    const { el } = await abrir();
    const enlaces = [...el.querySelectorAll<HTMLAnchorElement>('a[data-testid="ver-version"]')];
    expect(enlaces.length).toBe(2);
    expect(enlaces[1].getAttribute('href')).toMatch(/\/politicas\/terminos\?version=1$/);
    expect(enlaces[0].getAttribute('target')).toBe('_blank');
    expect(enlaces[0].getAttribute('rel')).toContain('noopener');
  });

  it('CA-3-20 si falla al publicar muestra el mensaje del servidor y conserva el texto', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    const nuevo = 'Texto nuevo de los términos de la tienda. '.repeat(3);
    escribir(porId(el, 'editor'), nuevo);
    await estabilizar();
    porId<HTMLButtonElement>(el, 'publicar').click();
    await estabilizar();
    ctrl
      .expectOne('/api/v1/admin/settings/politicas/terminos')
      .flush(errorApi('VALIDACION', 'Revisa los datos ingresados'), {
        status: 400,
        statusText: 'Bad Request',
      });
    await estabilizar();
    expect(porId(el, 'error-publicar').textContent).toContain('Revisa los datos ingresados');
    expect((porId(el, 'editor') as HTMLTextAreaElement).value).toBe(nuevo);
  });

  it('CA-3-20 si no carga la política muestra el error con "Reintentar"', async () => {
    const montaje = await montarComponente(PoliticasPage);
    const ctrl = TestBed.inject(HttpTestingController);
    ctrl
      .expectOne('/api/v1/settings/politicas/terminos')
      .flush(errorApi('ERROR_INTERNO', 'x'), { status: 500, statusText: 'x' });
    ctrl.expectOne('/api/v1/admin/settings/politicas/terminos/versiones').flush(respuesta([]));
    await montaje.estabilizar();
    expect(montaje.el.querySelector('fs-estado-error')).not.toBeNull();
  });
});
