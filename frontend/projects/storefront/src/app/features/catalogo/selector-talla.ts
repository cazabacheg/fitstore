import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import type { catalogApi } from 'shared';

/** Tallas del producto: las agotadas (en el color elegido) se ven tachadas y no se pueden elegir. */
@Component({
  selector: 'fs-selector-talla',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
    button {
      min-width: 44px;
      min-height: 44px;
      padding: 0 12px;
      border-radius: var(--fs-radio-md, 8px);
      border: 1px solid var(--fs-color-linea, #ccc);
      background: transparent;
      cursor: pointer;
      font: inherit;
    }
    button[aria-pressed='true'] {
      border-color: var(--fs-color-primario, #000);
      box-shadow: 0 0 0 1px var(--fs-color-primario, #000);
    }
    button.tachada {
      text-decoration: line-through;
      opacity: 0.45;
      cursor: not-allowed;
    }
  `,
  template: `
    @for (t of tallas(); track t.id) {
      <button
        type="button"
        [class.tachada]="agotadas().includes(t.id)"
        [disabled]="agotadas().includes(t.id)"
        [attr.aria-pressed]="valor() === t.id"
        (click)="valor.set(t.id)"
      >
        {{ t.codigo }}
      </button>
    }
  `,
})
export class SelectorTalla {
  readonly tallas = input<catalogApi.Talla[]>([]);
  /** Ids de las tallas sin stock en el color elegido. */
  readonly agotadas = input<number[]>([]);
  readonly valor = model<number | null>(null);
}
