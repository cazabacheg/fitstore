import { Router } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { crearRutas } from '../../app.routes';
import { montar } from '../../testing/montar';
import { SESION_ADMIN } from '../../testing/sesiones';

const SECCIONES = [
  'Botones',
  'Insignias de estado',
  'Estados',
  'Tabla paginada',
  'Barra de filtros',
  'Selector de ubigeo',
  'Subida de imágenes',
  'Toast',
  'Diálogo de confirmación',
  'Banner de servicio no disponible',
];

describe('Catálogo /_ui', () => {
  it('CA-0b-25 en desarrollo /_ui muestra cada componente de ui/ con sus variantes y estados', async () => {
    const harness = await montar(undefined, { url: '/_ui' });
    const el = harness.routeNativeElement as HTMLElement;
    expect(TestBed.inject(Router).url).toBe('/_ui');
    const titulos = [...el.querySelectorAll('h2')].map((h) => h.textContent?.trim());
    for (const seccion of SECCIONES) expect(titulos, seccion).toContain(seccion);
    // variantes de insignia y estados de la tabla
    expect(el.querySelectorAll('.fs-insignia').length).toBeGreaterThanOrEqual(20);
    for (const v of ['exito', 'aviso', 'info', 'error', 'marca'])
      expect(el.querySelector(`.fs-insignia--${v}`), v).not.toBeNull();
    expect(el.querySelector('fs-estado-cargando')).not.toBeNull();
    expect(el.querySelector('fs-estado-vacio')).not.toBeNull();
    expect(el.querySelector('fs-estado-error')).not.toBeNull();
    expect(el.querySelector('fs-tabla')).not.toBeNull();
    expect(el.querySelector('fs-selector-ubigeo')).not.toBeNull();
  }, 20_000); // Monta todos los componentes de ui/: con la suite completa en paralelo pasa de los 5 s por defecto.

  it('CA-0b-25 /_ui es pública: se ve sin sesión, porque no depende de la API', async () => {
    const harness = await montar(undefined, { url: '/_ui' });
    expect(TestBed.inject(Router).url).toBe('/_ui');
    expect((harness.routeNativeElement as HTMLElement).textContent).toContain(
      'Catálogo de componentes',
    );
  });

  it('CA-0b-25 en producción la ruta /_ui no existe (cae en la 404)', () => {
    const rutas = JSON.stringify(crearRutas(false), (_k, v) =>
      typeof v === 'function' ? 'fn' : v,
    );
    expect(rutas).not.toContain('_ui');
    expect(
      JSON.stringify(crearRutas(true), (_k, v) => (typeof v === 'function' ? 'fn' : v)),
    ).toContain('"path":"_ui"');
  });

  it('CA-0b-25 en producción, entrar a /_ui muestra la página 404', async () => {
    const { RouterTestingHarness } = await import('@angular/router/testing');
    const { provideRouter } = await import('@angular/router');
    TestBed.configureTestingModule({ providers: [provideRouter(crearRutas(false))] });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/_ui');
    expect((harness.routeNativeElement as HTMLElement).textContent).toContain(
      'Página no encontrada',
    );
    void SESION_ADMIN;
  });
});
