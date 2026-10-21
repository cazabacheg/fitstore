import DOMPurify from 'dompurify';
import { marked } from 'marked';

let preparado = false;

/** Los enlaces externos del texto no pasan la referencia de la tienda ni el control de la ventana. */
function preparar(): void {
  if (preparado) return;
  DOMPurify.addHook('afterSanitizeAttributes', (nodo) => {
    if (nodo.tagName === 'A' && nodo.getAttribute('href')) {
      nodo.setAttribute('rel', 'noopener noreferrer');
    }
  });
  preparado = true;
}

/**
 * Convierte el Markdown de una política en HTML seguro (SPEC-3 S3): siempre `marked` → `DOMPurify.sanitize`, y el
 * resultado va a `[innerHTML]`. Nunca se usa `bypassSecurityTrust*`. El backend guarda el texto tal cual.
 * Quedan fuera los scripts, los manejadores `on*`, los enlaces `javascript:` y `data:`, los marcos, formularios,
 * estilos, SVG y MathML.
 */
export function renderizarMarkdownSeguro(texto: string): string {
  if (!texto) return '';
  preparar();
  const html = marked.parse(texto, { async: false, gfm: true });
  return DOMPurify.sanitize(html, {
    USE_PROFILES: { html: true },
    FORBID_TAGS: [
      'style',
      'form',
      'input',
      'button',
      'textarea',
      'select',
      'iframe',
      'object',
      'embed',
    ],
    FORBID_ATTR: ['style'],
  });
}
