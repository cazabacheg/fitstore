import { Component, input, model, output } from '@angular/core';
import { FsBoton } from '../boton/boton';
import { FsDialogo } from './dialogo';

/**
 * Confirmación antes de una acción destructiva o irreversible: "Volver" a la izquierda y la acción con su verbo
 * a la derecha (sistema de diseño: Dialogo).
 */
@Component({
  selector: 'fs-dialogo-confirmacion',
  imports: [FsDialogo, FsBoton],
  template: `
    <fs-dialogo [titulo]="titulo()" [(abierto)]="abierto" (cerrado)="cancelado.emit()">
      @if (texto()) {
        <p style="margin: 0">{{ texto() }}</p>
      }
      <button fsAcciones type="button" fsBoton variante="contorno" (click)="alVolver()">
        {{ textoVolver() }}
      </button>
      <button
        fsAcciones
        type="button"
        fsBoton
        [variante]="peligro() ? 'peligro' : 'primario'"
        (click)="alConfirmar()"
      >
        {{ textoConfirmar() }}
      </button>
    </fs-dialogo>
  `,
})
export class DialogoConfirmacion {
  readonly titulo = input.required<string>();
  readonly texto = input('');
  readonly textoConfirmar = input('Confirmar');
  readonly textoVolver = input('Volver');
  readonly peligro = input(false);
  readonly abierto = model(false);
  readonly confirmado = output<void>();
  readonly cancelado = output<void>();

  protected alVolver(): void {
    this.abierto.set(false);
    this.cancelado.emit();
  }

  protected alConfirmar(): void {
    this.abierto.set(false);
    this.confirmado.emit();
  }
}
