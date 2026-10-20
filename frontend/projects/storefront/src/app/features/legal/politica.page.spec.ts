import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideFitstoreHttp } from 'shared';
import { PoliticaPage } from './politica.page';

const respuesta = <T>(data: T) => ({ success: true, mensaje: 'Operación exitosa', data });

function politica(contenido: string, version = 3) {
  return {
    tipo: 'terminos',
    version,
    contenido,
    publicadaEn: '2026-10-19T15:00:00Z',
  };
}

async function abrir(url: string) {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([{ path: 'politicas/:tipo', component: PoliticaPage }]),
      provideFitstoreHttp({ carrito: false }),
      provideHttpClientTesting(),
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  const ctrl = TestBed.inject(HttpTestingController);
  const el = harness.routeNativeElement as HTMLElement;
  const estabilizar = async () => {
    harness.detectChanges();
    await harness.fixture.whenStable();
  };
  return { harness, ctrl, el, estabilizar };
}

describe('PoliticaPage (P-3-02)', () => {
  it('CA-3-21 /politicas/terminos pide la versión vigente y muestra el texto con su número y fecha', async () => {
    const { ctrl, el, estabilizar } = await abrir('/politicas/terminos');
    const peticion = ctrl.expectOne((r) => r.url === '/api/v1/settings/politicas/terminos');
    expect(peticion.request.params.has('version')).toBe(false);
    peticion.flush(respuesta(politica('# Términos y condiciones\n\nTexto **vigente**.')));
    await estabilizar();

    expect(el.querySelector('h1')!.textContent).toContain('Términos y condiciones');
    expect(el.querySelector('strong')!.textContent).toBe('vigente');
    expect(el.querySelector('[data-testid="version"]')!.textContent).toContain('Versión 3');
    expect(el.querySelector('[data-testid="version"]')!.textContent).toContain(
      '19 de octubre de 2026',
    );
  });

  it('CA-3-21 con ?version=2 pide y muestra una versión anterior', async () => {
    const { ctrl, el, estabilizar } = await abrir('/politicas/privacidad?version=2');
    const peticion = ctrl.expectOne((r) => r.url === '/api/v1/settings/politicas/privacidad');
    expect(peticion.request.params.get('version')).toBe('2');
    peticion.flush(
      respuesta({ ...politica('# Privacidad\n\nTexto viejo.', 2), tipo: 'privacidad' }),
    );
    await estabilizar();

    expect(el.querySelector('[data-testid="version"]')!.textContent).toContain('Versión 2');
    expect(el.querySelector('[data-testid="aviso-anterior"]')).not.toBeNull();
  });

  it('CA-3-21 la versión vigente no muestra el aviso de versión anterior', async () => {
    const { ctrl, el, estabilizar } = await abrir('/politicas/terminos');
    ctrl
      .expectOne((r) => r.url.endsWith('/politicas/terminos'))
      .flush(respuesta(politica('# T\n\ntexto')));
    await estabilizar();
    expect(el.querySelector('[data-testid="aviso-anterior"]')).toBeNull();
  });

  it('CA-3-21 un <script> o un onerror= del Markdown no llegan al DOM', async () => {
    (window as unknown as Record<string, unknown>)['__xss'] = undefined;
    const malicioso = [
      '# Política',
      '',
      '<script>window.__xss = "script"</script>',
      '',
      '<img src="x" onerror="window.__xss = \'onerror\'">',
      '',
      '[clic aquí](javascript:window.__xss="enlace")',
      '',
      '<a href="javascript:window.__xss=1" onclick="window.__xss=2">otro</a>',
      '',
      'Texto legítimo que sí se ve.',
    ].join('\n');
    const { ctrl, el, estabilizar } = await abrir('/politicas/terminos');
    ctrl
      .expectOne((r) => r.url.endsWith('/politicas/terminos'))
      .flush(respuesta(politica(malicioso)));
    await estabilizar();

    expect(el.querySelector('script')).toBeNull();
    expect(el.innerHTML).not.toContain('onerror');
    expect(el.innerHTML).not.toContain('onclick');
    expect(el.innerHTML.toLowerCase()).not.toContain('javascript:');
    expect(el.textContent).toContain('Texto legítimo que sí se ve.');
    el.querySelector<HTMLElement>('img')?.dispatchEvent(new Event('error'));
    el.querySelectorAll<HTMLElement>('a').forEach((a) => a.click());
    expect((window as unknown as Record<string, unknown>)['__xss']).toBeUndefined();
  });

  it('CA-3-21 una política que no existe muestra el estado "no encontrada"', async () => {
    const { ctrl, el, estabilizar } = await abrir('/politicas/terminos?version=99');
    ctrl
      .expectOne((r) => r.url.endsWith('/politicas/terminos'))
      .flush(
        { success: false, mensaje: 'No encontramos lo que buscas', codigo: 'NO_ENCONTRADO' },
        { status: 404, statusText: 'x' },
      );
    await estabilizar();
    expect(el.querySelector('[data-testid="politica-no-encontrada"]')).not.toBeNull();
    expect(el.querySelector('h1')!.textContent).toContain('No encontramos');
  });

  it('CA-3-21 un tipo de política desconocido no llama a la API y muestra "no encontrada"', async () => {
    const { ctrl, el, estabilizar } = await abrir('/politicas/cookies');
    ctrl.expectNone((r) => r.url.includes('/politicas/'));
    await estabilizar();
    expect(el.querySelector('[data-testid="politica-no-encontrada"]')).not.toBeNull();
  });

  it('CA-3-21 si falla la carga ofrece reintentar', async () => {
    const { ctrl, el, estabilizar } = await abrir('/politicas/terminos');
    ctrl
      .expectOne((r) => r.url.endsWith('/politicas/terminos'))
      .flush(
        { success: false, mensaje: 'Error', codigo: 'ERROR_INTERNO' },
        { status: 500, statusText: 'x' },
      );
    await estabilizar();
    const reintentar = el.querySelector('[data-testid="reintentar"]') as HTMLButtonElement;
    expect(reintentar).not.toBeNull();
    reintentar.click();
    ctrl
      .expectOne((r) => r.url.endsWith('/politicas/terminos'))
      .flush(respuesta(politica('# Ya cargó')));
    await estabilizar();
    expect(el.querySelector('h1')!.textContent).toContain('Ya cargó');
  });
});
