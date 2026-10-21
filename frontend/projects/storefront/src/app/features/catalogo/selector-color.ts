import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import type { catalogApi } from 'shared';

/** Muestras de color: las que no tienen stock en ninguna talla se ven opacas, tachadas y no se pueden elegir. */
@Component({
  selector: 'fs-selector-color',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
    }
    button {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      border: 1px solid var(--fs-color-linea, #ccc);
      cursor: pointer;
      position: relative;
      padding: 0;
    }
    button[aria-pressed='true'] {
      outline: 2px solid var(--fs-color-primario, #000);
      outline-offset: 2px;
    }
    button.tachado {
      opacity: 0.4;
      cursor: not-allowed;
    }
    button.tachado::after {
      content: '';
      position: absolute;
      inset: 50% -2px auto;
      border-top: 2px solid #777;
      transform: rotate(-45deg);
    }
  `,
  template: `
    @for (c of colores(); track c.id) {
      <button
        type="button"
        [title]="c.nombre"
        [attr.aria-label]="c.nombre + (c.conStock ? '' : ' (sin stock)')"
        [style.background]="c.hex"
        [class.tachado]="!c.conStock"
        [disabled]="!c.conStock"
        [attr.aria-pressed]="valor() === c.id"
        (click)="valor.set(c.id)"
      ></button>
    }
  `,
})
export class SelectorColor {
  readonly colores = input<catalogApi.ColorDetalle[]>([]);
  readonly valor = model<number | null>(null);
}
