import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import type { catalogApi } from 'shared';

export interface DatosDialogoAjuste {
  stock: catalogApi.Stock;
}

/** Ajuste positivo o negativo de un SKU; exige motivo y no deja el físico por debajo de lo reservado (CA-2-40). */
@Component({
  selector: 'fs-dialogo-ajuste',
  imports: [MatDialogModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    mat-dialog-content {
      display: grid;
      gap: 12px;
      min-width: min(420px, 90vw);
    }
  `,
  template: `
    <h2 mat-dialog-title>Ajustar stock de {{ datos.stock.sku }}</h2>
    <mat-dialog-content>
      <p class="fs-suave">
        Físico {{ datos.stock.fisico }} · reservado {{ datos.stock.reservado }} · disponible
        {{ datos.stock.disponible }}
      </p>
      <label class="fs-campo">
        <span class="fs-campo__etiqueta">Cantidad (negativa para restar)</span>
        <input
          class="fs-input"
          name="cantidad"
          type="number"
          step="1"
          [value]="cantidad()"
          (input)="cantidad.set($any($event.target).value)"
        />
      </label>
      <label class="fs-campo">
        <span class="fs-campo__etiqueta">Motivo</span>
        <input
          class="fs-input"
          name="motivo"
          [value]="motivo()"
          (input)="motivo.set($any($event.target).value)"
        />
      </label>
      @if (error(); as e) {
        <p class="fs-campo__error" role="alert">{{ e }}</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions>
      <button type="button" class="fs-btn fs-btn--contorno" [mat-dialog-close]="null">
        Cancelar
      </button>
      <button
        type="button"
        class="fs-btn fs-btn--primario"
        [disabled]="!valido()"
        [mat-dialog-close]="resultado()"
      >
        Registrar ajuste
      </button>
    </mat-dialog-actions>
  `,
})
export class DialogoAjuste {
  protected readonly datos = inject<DatosDialogoAjuste>(MAT_DIALOG_DATA);
  protected readonly cantidad = signal('');
  protected readonly motivo = signal('');

  private readonly numero = computed(() => Number(this.cantidad()));
  protected readonly error = computed(() => {
    if (this.cantidad().trim() === '') return null;
    const n = this.numero();
    if (!Number.isInteger(n) || n === 0) return 'La cantidad debe ser un entero distinto de cero.';
    const { fisico, reservado } = this.datos.stock;
    if (n < 0 && fisico + n < reservado)
      return `Ese ajuste dejaría el físico por debajo de lo reservado (${reservado}).`;
    if (this.motivo().trim() === '') return 'Escribe el motivo del ajuste.';
    return null;
  });
  protected readonly valido = computed(
    () => this.cantidad().trim() !== '' && this.error() === null,
  );
  protected readonly resultado = computed<catalogApi.AjusteRequest>(() => ({
    varianteId: this.datos.stock.varianteId,
    cantidad: this.numero(),
    motivo: this.motivo().trim(),
  }));
}
