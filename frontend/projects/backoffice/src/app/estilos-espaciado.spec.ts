/** Reglas compartidas de espaciado del backoffice (CA-0b-31): se leen del disco, el runner corre en Node. */
interface Fs {
  readFileSync(ruta: string, codificacion: string): string;
  readdirSync(ruta: string, opciones: { withFileTypes: true }): { name: string; isDirectory(): boolean }[];
}
let fs: Fs;
let RAIZ = '';
let GLOBAL = '';
let TOKENS = '';

beforeAll(async () => {
  const modulo = 'node:fs'; // variable: el proyecto no trae @types/node
  fs = (await import(/* @vite-ignore */ modulo)) as Fs;
  RAIZ = `${(globalThis as unknown as { process: { cwd(): string } }).process.cwd()}/projects`;
  GLOBAL = fs.readFileSync(`${RAIZ}/backoffice/src/styles.css`, 'utf8');
  TOKENS = fs.readFileSync(`${RAIZ}/shared/src/lib/theme/tokens.css`, 'utf8');
});

function archivos(carpeta: string): string[] {
  return fs.readdirSync(carpeta, { withFileTypes: true }).flatMap((e) => {
    const ruta = `${carpeta}/${e.name}`;
    if (e.isDirectory()) return archivos(ruta);
    return /\.(ts|html|css)$/.test(e.name) && !/\.spec\.ts$/.test(e.name) ? [ruta] : [];
  });
}

const sinComentarios = (css: string) => css.replace(/\/\*[\s\S]*?\*\//g, '');

describe('Estilos compartidos del backoffice: espaciado', () => {
  it('CA-0b-31 toda variable CSS que usan las pantallas está definida (una variable sin definir anula la regla en silencio)', () => {
    const definidas = new Set<string>();
    const usos = new Map<string, string>();
    const fuentes = [`${RAIZ}/backoffice/src/styles.css`, ...archivos(`${RAIZ}/backoffice/src/app`)];
    const tokens = `${GLOBAL}\n${TOKENS}`;
    for (const m of tokens.matchAll(/(--[a-z0-9-]+)\s*:/g)) definidas.add(m[1]);
    for (const ruta of fuentes) {
      const texto = sinComentarios(fs.readFileSync(ruta, 'utf8'));
      for (const m of texto.matchAll(/(--[a-z0-9-]+)\s*:/g)) definidas.add(m[1]);
      // `var(--x)` sin respaldo: con respaldo (`var(--x, 8px)`) la regla sigue funcionando
      for (const m of texto.matchAll(/var\(\s*(--[a-z0-9-]+)\s*\)/g)) usos.set(m[1], ruta.replace(`${RAIZ}/`, ''));
    }
    const sinDefinir = [...usos].filter(([nombre]) => !definidas.has(nombre) && !/^--(mat|mdc|cdk)-/.test(nombre));
    expect(sinDefinir.map(([nombre, ruta]) => `${nombre} (${ruta})`)).toEqual([]);
  });

  it('CA-0b-31 la página que carga el shell separa sus bloques: el host de cada pantalla es una rejilla con espacio entre filas', () => {
    const css = sinComentarios(GLOBAL);
    const regla = /\.fs-bo__contenido\s*>\s*(?::where\()?\*\)?\s*\{([^}]*)\}/.exec(css);
    expect(regla?.[1]).toMatch(/display:\s*grid/);
    expect(regla?.[1]).toMatch(/gap:\s*var\(--espacio-4\)/);
    expect(regla?.[1]).toMatch(/min-width:\s*0/);
  });

  it('CA-0b-31 existen las clases que las pantallas ya usan: tarjeta, título de sección y accesibilidad', () => {
    const css = sinComentarios(GLOBAL);
    for (const clase of ['fs-tarjeta', 'fs-titulo-seccion', 'fs-sr-only', 'fs-acceso', 'fs-acceso__tarjeta', 'fs-acceso__titulo', 'fs-pagina', 'fs-grid-detalle', 'fs-historial', 'fs-chip-cliente']) {
      expect(css).toMatch(new RegExp(`\\.${clase}\\b`));
    }
    expect(/\.fs-tarjeta\s*\{([^}]*)\}/.exec(css)?.[1]).toMatch(/padding:\s*var\(--espacio-(4|5|6)\)/);
  });

  it('CA-0b-31 el título del formulario de acceso queda separado de la primera etiqueta y de la marca', () => {
    const css = sinComentarios(GLOBAL);
    const titulo = /\.fs-acceso__titulo\s*\{([^}]*)\}/.exec(css)?.[1] ?? '';
    expect(titulo).toMatch(/margin:\s*0\s+0\s+var\(--espacio-6\)/);
    const marca = /\.fs-acceso__marca\s*\{([^}]*)\}/.exec(css)?.[1] ?? '';
    expect(marca).toMatch(/margin:\s*0\s+0\s+var\(--espacio-4\)/);
  });

  it('CA-0b-35 los botones de los diálogos quedan separados, alineados a la derecha y con aire alrededor', () => {
    const css = sinComentarios(GLOBAL);
    const regla = /\.mat-mdc-dialog-actions\s*\{([^}]*)\}/.exec(css)?.[1] ?? '';
    expect(regla).toMatch(/gap:\s*var\(--espacio-(2|3)\)/);
    expect(regla).toMatch(/justify-content:\s*flex-end/);
    expect(regla).toMatch(/padding:\s*var\(--espacio-\d\)/);
  });
});
