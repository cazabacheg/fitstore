import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TemaService, provideFitstoreHttp } from 'shared';

/** Hoja de estilos global del backoffice, tal como se publica (se lee del disco: el runner corre en Node). */
let CSS = '';
beforeAll(async () => {
  const modulo = 'node:fs'; // variable: el proyecto no trae @types/node
  const fs = (await import(/* @vite-ignore */ modulo)) as {
    readFileSync(ruta: string, codificacion: string): string;
  };
  const proceso = (globalThis as unknown as { process: { cwd(): string } }).process;
  CSS = fs.readFileSync(`${proceso.cwd()}/projects/backoffice/src/styles.css`, 'utf8');
});

/** Valor declarado de una variable en `:root`, sin comentarios. */
function declarada(nombre: string): string | undefined {
  const sinComentarios = CSS.replace(/\/\*[\s\S]*?\*\//g, '');
  for (const bloque of sinComentarios.matchAll(/:root\s*\{([^}]*)\}/g)) {
    const m = new RegExp(`${nombre}\\s*:\\s*([^;]+);`).exec(bloque[1]);
    if (m) return m[1].trim();
  }
  return undefined;
}
/** Resuelve `var(--x)` contra lo que el TemaService escribió en `:root`. */
function resolver(nombre: string): string {
  const valor = declarada(nombre) ?? document.documentElement.style.getPropertyValue(nombre);
  const m = /^var\((--[a-z0-9-]+)\)$/.exec(valor);
  return m ? resolver(m[1]) : valor;
}

describe('Tema de Angular Material 3 del backoffice (styles.css)', () => {
  it('CA-0b-05 importa el tema base de Material y sobrescribe sus tokens con los de la marca', () => {
    expect(CSS).toMatch(/@import\s+['"]@angular\/material\/prebuilt-themes\/[a-z-]+\.css['"]/);
  });

  it.each([
    ['--mat-sys-primary', 'var(--fs-color-primario)'],
    ['--mat-sys-on-primary', 'var(--fs-color-en-primario)'],
    ['--mat-sys-primary-container', 'var(--fs-color-primario-suave)'],
    ['--mat-sys-on-primary-container', 'var(--fs-color-primario-texto)'],
    ['--mat-sys-secondary', 'var(--fs-color-secundario)'],
    ['--mat-sys-on-secondary', 'var(--fs-color-en-secundario)'],
    ['--mat-sys-tertiary', 'var(--fs-color-acento)'],
    ['--mat-sys-on-tertiary', 'var(--fs-color-en-acento)'],
    ['--mat-sys-background', 'var(--fs-color-fondo)'],
    ['--mat-sys-surface', 'var(--fs-color-superficie)'],
    ['--mat-sys-on-surface', 'var(--fs-color-texto)'],
    ['--mat-sys-on-background', 'var(--fs-color-texto)'],
    ['--mat-sys-on-surface-variant', 'var(--fs-color-texto-suave)'],
    ['--mat-sys-outline', 'var(--fs-color-borde-control)'],
    ['--mat-sys-outline-variant', 'var(--fs-color-linea)'],
    ['--mat-sys-error', 'var(--fs-color-error)'],
    ['--mat-sys-error-container', 'var(--fs-color-error-suave)'],
    ['--mat-button-text-label-text-color', 'var(--fs-color-primario-texto)'],
  ])('CA-0b-05 %s toma %s', (token, valor) => {
    expect(declarada(token)).toBe(valor);
  });

  it('CA-0b-05 la tipografía (Poppins) y los radios de Material son fijos y vienen de los tokens', () => {
    expect(declarada('--mat-sys-body-large-font')).toBe('var(--font-poppins)');
    expect(declarada('--mat-sys-title-large-font')).toBe('var(--font-poppins)');
    expect(declarada('--mat-sys-corner-medium')).toBe('var(--radio-md)');
  });

  it('CA-0b-05 si settings devuelve el primario #E11D48, el primario de Material es #E11D48', async () => {
    document.documentElement.removeAttribute('style');
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideFitstoreHttp({ carrito: false }), provideHttpClientTesting()],
    });
    const tema = TestBed.inject(TemaService);
    const inicio = tema.iniciar();
    TestBed.inject(HttpTestingController)
      .expectOne('/api/v1/settings/public')
      .flush({
        success: true,
        mensaje: 'Operación exitosa',
        data: {
          identidad: { nombre: 'Moda Activa' },
          tema: {
            colorPrimario: '#E11D48',
            colorSecundario: '#111827',
            colorAcento: '#F59E0B',
            colorFondo: '#FFFFFF',
            colorTexto: '#111827',
          },
        },
      });
    await inicio;
    expect(resolver('--mat-sys-primary').toLowerCase()).toBe('#e11d48');
    expect(resolver('--mat-sys-tertiary').toLowerCase()).toBe('#f59e0b');
    expect(resolver('--mat-sys-secondary').toLowerCase()).toBe('#111827');
  });
});

describe('Estilos de las filas marcadas de FsTabla', () => {
  let TABLA = '';
  beforeAll(async () => {
    const modulo = 'node:fs';
    const fs = (await import(/* @vite-ignore */ modulo)) as {
      readFileSync(ruta: string, codificacion: string): string;
    };
    const proceso = (globalThis as unknown as { process: { cwd(): string } }).process;
    TABLA = fs.readFileSync(
      `${proceso.cwd()}/projects/backoffice/src/app/ui/tabla/fs-tabla.css`,
      'utf8',
    );
  });
  const reglas = (css: string, selector: RegExp) =>
    [...css.replace(/\/\*[\s\S]*?\*\//g, '').matchAll(/([^{}]+)\{([^}]*)\}/g)]
      .filter((r) => selector.test(r[1]))
      .map((r) => r[2])
      .join(';');

  it('CA-0b-28 las filas por vencer o vencidas se marcan con un contorno de color, sin rellenar la fila', () => {
    // Con relleno, una insignia del mismo color (p. ej. "Pendiente de pago" sobre una fila aviso) se perdía.
    expect(reglas(CSS, /fs-fila--aviso/)).not.toMatch(/background/);
    expect(reglas(CSS, /fs-fila--error/)).not.toMatch(/background/);
    expect(reglas(CSS, /fs-fila--aviso\s+td:first-child/)).toMatch(
      /box-shadow:\s*inset 4px 0 0 var\(--fs-color-aviso\)/,
    );
    expect(reglas(CSS, /fs-fila--error\s+td:first-child/)).toMatch(
      /box-shadow:\s*inset 4px 0 0 var\(--fs-color-error\)/,
    );
  });

  it('CA-0b-28b una fila marcada de una tabla con detalle conserva la mano al pasar el mouse', () => {
    expect(reglas(TABLA, /fs-fila--(aviso|error)/)).not.toMatch(/cursor/);
    expect(reglas(TABLA, /fs-fila--clicable/)).toMatch(/cursor:\s*pointer/);
  });
});
