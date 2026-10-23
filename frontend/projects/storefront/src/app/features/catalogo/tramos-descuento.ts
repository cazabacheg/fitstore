import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import type { catalogApi } from 'shared';

/**
 * Barra de tramos de descuento por volumen (CA-2-11, CA-2-67 a CA-2-69): cada tramo es un botón que fija su cantidad,
 * la barra se llena de color según la cantidad elegida y los tramos que el stock no alcanza se ven grises.
 */
@Component({
  selector: 'fs-tramos-descuento',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .tramos {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(0, 1fr));
      border: 1px solid var(--fs-color-linea, #ddd);
      border-radius: var(--fs-radio-md, 8px);
      overflow: hidden;
      margin-top: 6px;
    }
    .tramo {
      padding: 12px 8px;
      border: 0;
      border-bottom: 3px solid transparent;
      background: linear-gradient(
        to right,
        var(--fs-color-primario-suave, #eef) var(--relleno, 0%),
        transparent var(--relleno, 0%)
      );
      color: var(--fs-color-texto);
      font: inherit;
      font-weight: 600;
      text-align: center;
      cursor: pointer;
      transition: background 0.15s;
    }
    .tramo + .tramo {
      border-left: 1px solid var(--fs-color-linea, #ddd);
    }
    .tramo.alcanzado {
      border-bottom-color: var(--fs-color-primario-texto, #000);
      color: var(--fs-color-primario-texto, #000);
    }
    .tramo.sin-stock {
      background: var(--fs-color-superficie-alterna, #eee);
      color: var(--fs-color-texto-suave, #666);
      opacity: 0.55;
      cursor: not-allowed;
      filter: grayscale(1);
    }
    .tramo:focus-visible {
      outline: 2px solid var(--fs-color-foco, #000);
      outline-offset: -2px;
    }
    small {
      display: block;
      margin-top: 6px;
      color: var(--fs-color-texto-suave, #666);
    }
  `,
  template: `
    @if (tramos().length) {
      <section aria-label="Compra por cantidad y ahorra">
        <strong>Compra por cantidad y ahorra</strong>
        <div class="tramos">
          @for (t of vista(); track t.unidadesMinimas) {
            <button
              type="button"
              class="tramo"
              [class.alcanzado]="t.alcanzado"
              [class.sin-stock]="t.sinStock"
              [style.--relleno]="t.relleno + '%'"
              [disabled]="t.sinStock"
              [attr.aria-pressed]="t.alcanzado"
              [attr.title]="t.sinStock ? 'No hay stock suficiente para este tramo' : null"
              (click)="cantidad.set(t.unidadesMinimas)"
            >
              {{ t.unidadesMinimas }} un. −{{ t.porcentaje }} %
            </button>
          }
        </div>
        <small>Se combinan tallas y colores del mismo producto.</small>
      </section>
    }
  `,
})
export class TramosDescuento {
  readonly tramos = input<catalogApi.Tramo[]>([]);
  /** Cantidad elegida; al hacer clic en un tramo pasa a ser sus unidades mínimas. */
  readonly cantidad = model(1);
  /** Unidades que se pueden comprar (stock conocido de la variante, o el mayor del color si aún no hay talla). */
  readonly maximo = input(Number.MAX_SAFE_INTEGER);

  protected readonly vista = computed(() => {
    const q = this.cantidad();
    let base = 1;
    return this.tramos().map((t) => {
      const relleno = Math.round(
        Math.min(Math.max((q - base) / Math.max(t.unidadesMinimas - base, 1), 0), 1) * 100,
      );
      base = t.unidadesMinimas;
      return {
        ...t,
        relleno,
        alcanzado: q >= t.unidadesMinimas,
        sinStock: t.unidadesMinimas > this.maximo(),
      };
    });
  });
}
