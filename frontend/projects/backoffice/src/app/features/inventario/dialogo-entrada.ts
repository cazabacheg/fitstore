import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { catalogApi } from 'shared';

export interface DatosDialogoEntrada {
  /** SKU ya conocidos (los de la página); los demás se buscan al escribir. */
  opciones: catalogApi.Stock[];
}

interface Linea {
  sku: string;
  cantidad: string;
}

/** Entrada de stock de varias líneas (CA-2-39): cada línea es un SKU y su cantidad. */
@Component({
  selector: 'fs-dialogo-entrada',
  imports: [MatDialogModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .linea {
      display: grid;
      grid-template-columns: 1fr 110px auto;
      gap: 8px;
      margin-bottom: 8px;
      align-items: start;
    }
    mat-dialog-content {
      display: grid;
      gap: 8px;
      min-width: min(480px, 90vw);
    }
  `,
  template: `
    <h2 mat-dialog-title>Registrar entrada de stock</h2>
    <mat-dialog-content>
      @for (l of lineas(); track $index; let i = $index) {
        <div class="linea">
          <div>
            <input
              class="fs-input"
              list="skus-entrada"
              [attr.aria-label]="'SKU de la línea ' + (i + 1)"
              placeholder="SKU"
              [value]="l.sku"
              (input)="cambiar(i, 'sku', $any($event.target).value)"
            />
            @if (l.sku.trim() !== '' && !resolver(l.sku)) {
              <p class="fs-campo__error">No encontramos el SKU {{ l.sku.trim() }}.</p>
            }
          </div>
          <input
            class="fs-input"
            type="number"
            min="1"
            [attr.aria-label]="'Cantidad de la línea ' + (i + 1)"
            placeholder="Cantidad"
            [value]="l.cantidad"
            (input)="cambiar(i, 'cantidad', $any($event.target).value)"
          />
          @if (lineas().length > 1) {
            <button
              type="button"
              class="fs-btn fs-btn--texto fs-btn--sm"
              [attr.aria-label]="'Quitar la línea ' + (i + 1)"
              (click)="quitar(i)"
            >
              Quitar
            </button>
          }
        </div>
      }
      <datalist id="skus-entrada">
        @for (o of opciones(); track o.varianteId) {
          <option [value]="o.sku">{{ o.producto }} · {{ o.talla }} · {{ o.color }}</option>
        }
      </datalist>
      <div>
        <button type="button" class="fs-btn fs-btn--contorno fs-btn--sm" (click)="agregar()">
          Agregar línea
        </button>
      </div>
      <label class="fs-campo">
        <span class="fs-campo__etiqueta">Motivo (opcional)</span>
        <input
          class="fs-input"
          name="motivo"
          placeholder="Compra a proveedor"
          [value]="motivo()"
          (input)="motivo.set($any($event.target).value)"
        />
      </label>
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
        Registrar entrada
      </button>
    </mat-dialog-actions>
  `,
})
export class DialogoEntrada {
  private readonly datos = inject<DatosDialogoEntrada>(MAT_DIALOG_DATA);
  private readonly api = inject(catalogApi.InventarioService);

  protected readonly lineas = signal<Linea[]>([{ sku: '', cantidad: '' }]);
  protected readonly motivo = signal('');
  protected readonly opciones = signal<catalogApi.Stock[]>(this.datos.opciones);

  protected resolver(sku: string): catalogApi.Stock | undefined {
    const buscado = sku.trim().toLowerCase();
    return this.opciones().find((o) => o.sku.toLowerCase() === buscado);
  }

  protected readonly valido = computed(() =>
    this.lineas().every((l) => {
      const n = Number(l.cantidad);
      return !!this.resolver(l.sku) && Number.isInteger(n) && n >= 1;
    }),
  );

  protected readonly resultado = computed<catalogApi.EntradaRequest>(() => ({
    lineas: this.lineas().map((l) => ({
      varianteId: this.resolver(l.sku)?.varianteId ?? 0,
      cantidad: Number(l.cantidad),
    })),
    motivo: this.motivo().trim() || null,
  }));

  protected cambiar(indice: number, campo: keyof Linea, valor: string): void {
    this.lineas.update((l) => l.map((x, i) => (i === indice ? { ...x, [campo]: valor } : x)));
    if (campo === 'sku' && valor.trim().length >= 3 && !this.resolver(valor))
      this.buscar(valor.trim());
  }

  protected agregar(): void {
    this.lineas.update((l) => [...l, { sku: '', cantidad: '' }]);
  }

  protected quitar(indice: number): void {
    this.lineas.update((l) => l.filter((_, i) => i !== indice));
  }

  /** Trae de la API los SKU que coinciden con lo escrito y los suma a las opciones. */
  private buscar(texto: string): void {
    this.api.listarStock(texto, undefined, undefined, undefined, 0, 8).subscribe((r) => {
      const nuevos = (r.data?.contenido ?? []).filter(
        (s) => !this.opciones().some((o) => o.varianteId === s.varianteId),
      );
      if (nuevos.length) this.opciones.update((o) => [...o, ...nuevos]);
    });
  }
}
