import { Component, input } from '@angular/core';

/** Franja superior con un anuncio. Sin texto no se pinta nada. */
@Component({
  selector: 'fs-barra-anuncio',
  template: `
    @if (texto()) {
      <div class="fs-anuncio" role="region" aria-label="Anuncio">{{ texto() }}</div>
    }
  `,
})
export class BarraAnuncio {
  readonly texto = input('');
}
