import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import type { catalogApi } from 'shared';

/** "3 un. −5 % · 6 un. −10 %…" con la aclaración de que se combinan tallas y colores (CA-2-11). */
@Component({
  selector: 'fs-tramos-descuento',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .tramos {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(0, 1fr));
      border: 1px solid var(--fs-color-linea, #ddd);
      border-radius: var(--fs-radio-md, 8px);
      overflow: hidden;
    }
    .tramos div {
      padding: 12px 8px;
      text-align: center;
      font-weight: 600;
    }
    .tramos div + div {
      border-left: 1px solid var(--fs-color-linea, #ddd);
    }
    small {
      display: block;
      margin-top: 6px;
      color: var(--fs-color-texto-suave, #666);
    }
  `,
  template: `
    @if (tramos().length) {
      <section aria-label="Compra por cantidad y ahorra">
        <strong>Compra por cantidad y ahorra</strong>
        <div class="tramos">
          @for (t of tramos(); track t.unidadesMinimas) {
            <div>{{ t.unidadesMinimas }} un. −{{ t.porcentaje }} %</div>
          }
        </div>
        <small>Se combinan tallas y colores del mismo producto.</small>
      </section>
    }
  `,
})
export class TramosDescuento {
  readonly tramos = input<catalogApi.Tramo[]>([]);
}
