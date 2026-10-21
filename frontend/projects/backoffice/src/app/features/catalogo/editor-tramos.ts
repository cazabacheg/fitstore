import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import type { catalogApi } from 'shared';

/** Editor de tramos de descuento por volumen (política global y modo personalizado de un producto). */
@Component({
  selector: 'fs-editor-tramos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: grid;
      gap: var(--espacio-3);
    }
    .tramo {
      display: flex;
      gap: var(--espacio-3);
      align-items: center;
    }
    .tramo input {
      width: 96px;
    }
    .error {
      color: var(--fs-color-error);
      margin: 0;
    }
  `,
  template: `
    @for (t of tramos(); track $index; let i = $index) {
      <div class="tramo">
        <label>
          Desde
          <input
            class="fs-input"
            type="number"
            min="1"
            [attr.aria-label]="'Unidades del tramo ' + (i + 1)"
            [value]="t.unidadesMinimas"
            [disabled]="deshabilitado()"
            (input)="cambiar(i, 'unidadesMinimas', $any($event.target).value)"
          />
          un.
        </label>
        <label>
          −
          <input
            class="fs-input"
            type="number"
            min="0"
            max="50"
            [attr.aria-label]="'Porcentaje del tramo ' + (i + 1)"
            [value]="t.porcentaje"
            [disabled]="deshabilitado()"
            (input)="cambiar(i, 'porcentaje', $any($event.target).value)"
          />
          %
        </label>
        @if (!deshabilitado()) {
          <button
            type="button"
            class="fs-btn fs-btn--texto fs-btn--sm"
            [attr.aria-label]="'Quitar tramo ' + (i + 1)"
            (click)="quitar(i)"
          >
            Quitar
          </button>
        }
      </div>
    }
    @for (e of errores(); track e) {
      <p class="error" role="alert">{{ e }}</p>
    }
    @if (!deshabilitado()) {
      <div>
        <button
          type="button"
          class="fs-btn fs-btn--contorno fs-btn--sm"
          [disabled]="tramos().length >= max()"
          (click)="agregar()"
        >
          Agregar tramo
        </button>
      </div>
    }
    @if (vistaPrevia()) {
      <p class="fs-suave">En la tienda: {{ vistaPrevia() }}</p>
    }
  `,
})
export class EditorTramos {
  readonly tramos = model<catalogApi.Tramo[]>([]);
  readonly max = input(10);
  readonly deshabilitado = input(false);

  protected readonly errores = computed(() => {
    const t = this.tramos();
    const errores: string[] = [];
    if (t.some((x, i) => i > 0 && x.unidadesMinimas <= t[i - 1].unidadesMinimas))
      errores.push('Las unidades deben ir de menor a mayor');
    if (t.some((x, i) => i > 0 && x.porcentaje <= t[i - 1].porcentaje))
      errores.push('Los porcentajes deben ir de menor a mayor');
    if (t.some((x) => !(x.porcentaje >= 0 && x.porcentaje <= 50)))
      errores.push('El porcentaje debe estar entre 0 y 50');
    if (t.some((x) => !Number.isInteger(x.unidadesMinimas) || x.unidadesMinimas < 1))
      errores.push('Las unidades deben ser un número entero desde 1');
    return errores;
  });
  readonly valido = computed(() => this.errores().length === 0);
  /** Texto como lo muestra la tienda (CA-2-37). */
  protected readonly vistaPrevia = computed(() =>
    this.tramos()
      .map((t) => `${t.unidadesMinimas} un. −${t.porcentaje} %`)
      .join(' · '),
  );

  protected cambiar(indice: number, campo: keyof catalogApi.Tramo, texto: string): void {
    const valor = texto.trim() === '' ? 0 : Number(texto);
    this.tramos.update((lista) =>
      lista.map((t, i) => (i === indice ? { ...t, [campo]: valor } : t)),
    );
  }

  protected agregar(): void {
    const ultimo = this.tramos().at(-1);
    this.tramos.update((lista) => [
      ...lista,
      ultimo
        ? {
            unidadesMinimas: ultimo.unidadesMinimas + 3,
            porcentaje: Math.min(ultimo.porcentaje + 5, 50),
          }
        : { unidadesMinimas: 3, porcentaje: 5 },
    ]);
  }

  protected quitar(indice: number): void {
    this.tramos.update((lista) => lista.filter((_, i) => i !== indice));
  }
}
