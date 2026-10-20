import { readFileSync } from 'node:fs';

const estilos = readFileSync(`${process.cwd()}/projects/storefront/src/styles.css`, 'utf8');

describe('Tema de Tailwind de la tienda', () => {
  it('CA-0b-05 el @theme toma los colores de marca de las variables de settings', () => {
    const tema = estilos.slice(estilos.indexOf('@theme'));
    for (const v of [
      '--fs-color-primario',
      '--fs-color-secundario',
      '--fs-color-acento',
      '--fs-color-fondo',
      '--fs-color-texto',
      '--fs-color-en-primario',
      '--fs-color-primario-texto',
    ]) {
      expect(tema, v).toContain(`var(${v})`);
    }
  });

  it('CA-0b-05 la tipografía, los radios y las sombras salen de los tokens fijos', () => {
    const tema = estilos.slice(estilos.indexOf('@theme'));
    expect(tema).toContain('var(--font-poppins)');
    expect(tema).toContain('var(--radio-md)');
    expect(tema).toContain('var(--sombra-1)');
  });

  it('CA-0b-05 los botones usan el color primario de la marca y no un color literal', () => {
    expect(estilos).toMatch(/\.fs-btn--primario\s*\{[^}]*var\(--fs-color-primario\)/);
    const sinTokens = estilos
      .split('\n')
      .filter((l: string) => !l.includes('@import') && /#[0-9a-fA-F]{3,6}\b/.test(l));
    expect(sinTokens).toEqual([]);
  });
});
