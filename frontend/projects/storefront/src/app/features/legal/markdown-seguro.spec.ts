import { renderizarMarkdownSeguro } from './markdown-seguro';

/** Vectores clásicos de XSS que el Markdown de una política nunca debe dejar pasar al DOM (CA-3-21, S3). */
const ATAQUES = [
  '<script>window.__xss = 1</script>',
  '<img src=x onerror="window.__xss = 2">',
  '<a href="javascript:window.__xss = 3">clic</a>',
  '[clic](javascript:window.__xss=4)',
  '<iframe src="https://malo.example"></iframe>',
  '<svg onload="window.__xss = 5"></svg>',
  '<div onclick="window.__xss = 6">x</div>',
  '<object data="https://malo.example"></object>',
  '<form action="https://malo.example"><input name="x"></form>',
  '<style>body{display:none}</style>',
  '<math><mi xlink:href="javascript:window.__xss=7">x</mi></math>',
  '<a href="data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==">x</a>',
];

function aDom(html: string): HTMLElement {
  const contenedor = document.createElement('div');
  contenedor.innerHTML = html;
  return contenedor;
}

describe('renderizarMarkdownSeguro', () => {
  it.each(ATAQUES)('CA-3-21 neutraliza %s', (ataque) => {
    const dom = aDom(renderizarMarkdownSeguro(`# Título\n\n${ataque}\n\nTexto normal`));
    expect(dom.querySelector('script, iframe, object, embed, form, style, svg, math')).toBeNull();
    for (const elemento of Array.from(dom.querySelectorAll('*'))) {
      for (const atributo of Array.from(elemento.attributes)) {
        expect(atributo.name.startsWith('on')).toBe(false);
        if (['href', 'src', 'xlink:href'].includes(atributo.name)) {
          expect(atributo.value.trim().toLowerCase().startsWith('javascript:')).toBe(false);
          expect(atributo.value.trim().toLowerCase().startsWith('data:')).toBe(false);
        }
      }
    }
    expect(dom.textContent).toContain('Texto normal');
    expect((window as unknown as Record<string, unknown>)['__xss']).toBeUndefined();
  });

  it('CA-3-21 conserva el formato legítimo: títulos, listas, negritas y enlaces https', () => {
    const dom = aDom(
      renderizarMarkdownSeguro(
        '# Términos\n\n## 1. Alcance\n\nTexto con **negrita** y [enlace](https://tienda.pe/ayuda).\n\n- uno\n- dos\n\n1. primero\n2. segundo\n',
      ),
    );
    expect(dom.querySelector('h1')!.textContent).toBe('Términos');
    expect(dom.querySelector('h2')!.textContent).toBe('1. Alcance');
    expect(dom.querySelector('strong')!.textContent).toBe('negrita');
    expect(dom.querySelector('a')!.getAttribute('href')).toBe('https://tienda.pe/ayuda');
    expect(dom.querySelectorAll('ul li').length).toBe(2);
    expect(dom.querySelectorAll('ol li').length).toBe(2);
  });

  it('CA-3-21 conserva las tablas de los textos modelo', () => {
    const dom = aDom(
      renderizarMarkdownSeguro('| Modalidad | Plazo |\n|---|---|\n| Delivery | 1 a 3 días |\n'),
    );
    expect(dom.querySelector('table')).not.toBeNull();
    expect(dom.querySelectorAll('td').length).toBe(2);
  });

  it('CA-3-21 los enlaces externos se abren sin pasar la referencia (noopener)', () => {
    const dom = aDom(renderizarMarkdownSeguro('[sitio](https://tienda.pe)'));
    const enlace = dom.querySelector('a')!;
    expect(enlace.getAttribute('rel')).toContain('noopener');
  });

  it('CA-3-21 un texto vacío da un resultado vacío', () => {
    expect(renderizarMarkdownSeguro('')).toBe('');
  });
});
