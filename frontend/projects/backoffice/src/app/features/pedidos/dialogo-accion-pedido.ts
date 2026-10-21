import { ChangeDetectionStrategy, Component, Injectable, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { Observable, map } from 'rxjs';

/** Qué pide una acción sobre el pedido antes de ejecutarse. */
export interface DatosAccionPedido {
  titulo: string;
  mensaje?: string;
  /** Verbo exacto del botón principal ("Marcar listo para despacho"), nunca "Aceptar". */
  accion: string;
  peligro?: boolean;
  /** Campo de texto: comentario opcional del avance, o motivo obligatorio de una cancelación o devolución. */
  comentario?: { etiqueta: string; obligatorio: boolean; maximo: number };
  /** Casilla que el operador debe marcar ("Verifiqué el DNI del destinatario"). */
  confirmacion?: string;
}

/** Lo que el operador escribió o marcó. */
export interface ResultadoAccionPedido {
  comentario: string | null;
  confirmado: boolean;
}

/**
 * Diálogo de las acciones del operador sobre un pedido: comentario opcional al avanzar (DialogoComentario), "Recogido" con
 * la casilla del DNI (DialogoRecogido), "No recogido" con motivo obligatorio (DialogoNoRecogido) y cancelar con motivo
 * (DialogoCancelarStaff). Una sola pieza configurable para que las cuatro se vean y se comporten igual.
 */
@Component({
  selector: 'fs-dialogo-accion-pedido',
  imports: [MatDialogModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ datos.titulo }}</h2>
    <mat-dialog-content>
      @if (datos.mensaje) {
        <p>{{ datos.mensaje }}</p>
      }
      @if (datos.comentario; as c) {
        <label class="fs-campo">
          {{ c.etiqueta }}{{ c.obligatorio ? '' : ' (opcional)' }}
          <textarea
            class="fs-input"
            rows="3"
            [attr.maxlength]="c.maximo"
            [attr.aria-label]="c.etiqueta"
            [value]="texto()"
            (input)="texto.set(valor($event))"
          ></textarea>
        </label>
        <span class="fs-suave">{{ texto().length }} / {{ c.maximo }}</span>
      }
      @if (datos.confirmacion; as etiqueta) {
        <label class="fs-check">
          <input type="checkbox" [checked]="marcada()" (change)="marcada.set(marcadaEn($event))" />
          {{ etiqueta }}
        </label>
      }
    </mat-dialog-content>
    <mat-dialog-actions>
      <button type="button" class="fs-btn fs-btn--contorno" (click)="cerrar.close(undefined)">Volver</button>
      <button
        type="button"
        class="fs-btn"
        [class.fs-btn--primario]="!datos.peligro"
        [class.fs-btn--peligro]="datos.peligro"
        [disabled]="!valido()"
        (click)="aceptar()"
      >
        {{ datos.accion }}
      </button>
    </mat-dialog-actions>
  `,
})
export class DialogoAccionPedido {
  protected readonly datos = inject<DatosAccionPedido>(MAT_DIALOG_DATA);
  protected readonly cerrar = inject<MatDialogRef<DialogoAccionPedido, ResultadoAccionPedido | undefined>>(MatDialogRef);

  protected readonly texto = signal('');
  protected readonly marcada = signal(false);
  protected readonly valido = computed(() => {
    const motivoOk = !this.datos.comentario?.obligatorio || this.texto().trim().length >= 3;
    const casillaOk = !this.datos.confirmacion || this.marcada();
    return motivoOk && casillaOk;
  });

  protected valor(evento: Event): string {
    return (evento.target as HTMLTextAreaElement).value;
  }

  protected marcadaEn(evento: Event): boolean {
    return (evento.target as HTMLInputElement).checked;
  }

  protected aceptar(): void {
    if (!this.valido()) return;
    const comentario = this.texto().trim();
    this.cerrar.close({ comentario: comentario === '' ? null : comentario, confirmado: this.marcada() });
  }
}

@Injectable({ providedIn: 'root' })
export class DialogoAccionPedidoService {
  private readonly dialogo = inject(MatDialog);

  /** Emite lo escrito si se confirma, o `null` si se cierra con "Volver" o Esc. */
  abrir(datos: DatosAccionPedido): Observable<ResultadoAccionPedido | null> {
    return this.dialogo
      .open<DialogoAccionPedido, DatosAccionPedido, ResultadoAccionPedido | undefined>(DialogoAccionPedido, {
        data: datos,
        maxWidth: '480px',
        width: '100%',
        restoreFocus: true,
      })
      .afterClosed()
      .pipe(map((resultado) => resultado ?? null));
  }
}
