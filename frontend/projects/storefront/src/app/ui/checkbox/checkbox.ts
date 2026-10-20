import { Component, input, model } from '@angular/core';

/** Casilla de verificación con su etiqueta (sistema de diseño: Campo con `fs-check`). */
@Component({
  selector: 'fs-checkbox',
  template: `
    <label class="fs-check">
      <input
        type="checkbox"
        [checked]="marcado()"
        [disabled]="deshabilitado()"
        (change)="alCambiar($event)"
      />
      <span>{{ etiqueta() }}</span>
    </label>
  `,
})
export class FsCheckbox {
  readonly etiqueta = input.required<string>();
  readonly deshabilitado = input(false);
  readonly marcado = model(false);

  protected alCambiar(evento: Event): void {
    this.marcado.set((evento.target as HTMLInputElement).checked);
  }
}
