import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';

/** Cantidad a agregar: entre 1 y el máximo disponible de la variante. */
@Component({
  selector: 'fs-selector-cantidad',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      border: 1px solid var(--fs-color-linea, #ccc);
      border-radius: var(--fs-radio-md, 8px);
    }
    button {
      width: 44px;
      height: 44px;
      border: 0;
      background: transparent;
      cursor: pointer;
      font-size: 20px;
    }
    button:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
    input {
      width: 48px;
      text-align: center;
      border: 0;
      font: inherit;
      background: transparent;
    }
  `,
  template: `
    <button
      type="button"
      aria-label="Menos unidades"
      [disabled]="deshabilitado() || valor() <= 1"
      (click)="cambiar(valor() - 1)"
    >
      −
    </button>
    <input
      type="text"
      inputmode="numeric"
      aria-label="Cantidad"
      [value]="valor()"
      [disabled]="deshabilitado()"
      (change)="cambiar(+$any($event.target).value); $any($event.target).value = valor()"
    />
    <button
      type="button"
      aria-label="Más unidades"
      [disabled]="deshabilitado() || valor() >= maximo()"
      (click)="cambiar(valor() + 1)"
    >
      +
    </button>
  `,
})
export class SelectorCantidad {
  readonly maximo = input(1);
  readonly deshabilitado = input(false);
  readonly valor = model(1);

  protected cambiar(n: number): void {
    const entero = Number.isFinite(n) ? Math.trunc(n) : 1;
    this.valor.set(Math.min(Math.max(entero, 1), Math.max(this.maximo(), 1)));
  }
}
