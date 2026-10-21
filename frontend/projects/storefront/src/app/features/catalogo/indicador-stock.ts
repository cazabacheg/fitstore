import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import type { catalogApi } from 'shared';

/** "Agotado", "¡Últimas N unidades!" o "Disponible" según la variante elegida (CA-2-09). */
@Component({
  selector: 'fs-indicador-stock',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (variante(); as v) {
      <p role="status">
        @switch (v.disponibilidad) {
          @case ('AGOTADO') {
            Agotado
          }
          @case ('ULTIMAS') {
            ¡Últimas {{ v.ultimasUnidades }} unidades!
          }
          @default {
            Disponible
          }
        }
      </p>
    }
  `,
})
export class IndicadorStock {
  readonly variante = input<catalogApi.VariantePublica | null>(null);
}
