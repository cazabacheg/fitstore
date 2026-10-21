import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import type { catalogApi } from 'shared';

export interface DatosDialogoMinimo {
  stock: catalogApi.Stock;
}

/** Umbral de stock bajo propio de un SKU; vacío vuelve al de la tienda (CA-2-43). */
@Component({
  selector: 'fs-dialogo-minimo',
  imports: [MatDialogModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Mínimo de {{ datos.stock.sku }}</h2>
    <mat-dialog-content>
      <label class="fs-campo">
        <span class="fs-campo__etiqueta">Avisar cuando queden</span>
        <input
          class="fs-input"
          name="minimo"
          type="number"
          min="0"
          step="1"
          [value]="valor()"
          (input)="valor.set($any($event.target).value)"
        />
        <span class="fs-campo__ayuda"
          >Ahora se usa {{ datos.stock.minimo }}
          {{ datos.stock.minimoPropio ? '(propio de este SKU)' : '(el de la tienda)' }}.</span
        >
      </label>
    </mat-dialog-content>
    <mat-dialog-actions>
      <button type="button" class="fs-btn fs-btn--contorno" [mat-dialog-close]="null">
        Cancelar
      </button>
      <button type="button" class="fs-btn fs-btn--contorno" [mat-dialog-close]="{ minimo: null }">
        Usar el de la tienda
      </button>
      <button
        type="button"
        class="fs-btn fs-btn--primario"
        [disabled]="!valido()"
        [mat-dialog-close]="{ minimo: numero() }"
      >
        Guardar
      </button>
    </mat-dialog-actions>
  `,
})
export class DialogoMinimo {
  protected readonly datos = inject<DatosDialogoMinimo>(MAT_DIALOG_DATA);
  protected readonly valor = signal(
    this.datos.stock.minimoPropio ? String(this.datos.stock.minimo) : '',
  );
  protected readonly numero = computed(() => Number(this.valor()));
  protected readonly valido = computed(
    () => this.valor().trim() !== '' && Number.isInteger(this.numero()) && this.numero() >= 0,
  );
}
