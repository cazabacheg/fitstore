import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface DatosConfirmacion {
  /** Pregunta o acción: "¿Cancelar el pedido FS-000124?". */
  titulo: string;
  mensaje?: string;
  /** Verbo exacto de lo que hace el botón: "Cancelar pedido" (nunca "Aceptar"). */
  accion: string;
  /** Acción destructiva: el botón va en rojo. */
  peligro?: boolean;
}

/** Diálogo de confirmación: salida a la izquierda ("Volver"), la acción con su verbo a la derecha. */
@Component({
  selector: 'fs-dialogo-confirmacion',
  imports: [MatDialogModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ datos.titulo }}</h2>
    @if (datos.mensaje) {
      <mat-dialog-content>{{ datos.mensaje }}</mat-dialog-content>
    }
    <mat-dialog-actions>
      <button type="button" class="fs-btn fs-btn--contorno" [mat-dialog-close]="false">
        Volver
      </button>
      <button
        type="button"
        class="fs-btn"
        [class.fs-btn--primario]="!datos.peligro"
        [class.fs-btn--peligro]="datos.peligro"
        [mat-dialog-close]="true"
        cdkFocusInitial
      >
        {{ datos.accion }}
      </button>
    </mat-dialog-actions>
  `,
})
export class DialogoConfirmacion {
  protected readonly datos = inject<DatosConfirmacion>(MAT_DIALOG_DATA);
}
