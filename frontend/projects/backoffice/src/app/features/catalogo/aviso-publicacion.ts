import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Lista de lo que falta para publicar un producto (409 `NO_PUBLICABLE`, CA-2-24). */
@Component({
  selector: 'fs-aviso-publicacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    div {
      padding: var(--espacio-3) var(--espacio-4);
      border: 1px solid var(--fs-color-error);
      border-radius: var(--radio-md);
    }
    ul {
      margin: var(--espacio-2) 0 0;
      padding-left: 20px;
    }
  `,
  template: `
    @if (faltantes().length > 0) {
      <div role="alert">
        <strong>Falta completar el producto para publicarlo:</strong>
        <ul>
          @for (f of faltantes(); track f) {
            <li>{{ f }}</li>
          }
        </ul>
      </div>
    }
  `,
})
export class AvisoPublicacion {
  readonly faltantes = input<string[]>([]);
}
