import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import type { catalogApi } from 'shared';

export interface CombinacionVariante {
  tallaId: number;
  colorId: number;
}

/** Elige tallas y colores y propone crear todas las combinaciones que aún no existen (CA-2-22). */
@Component({
  selector: 'fs-generador-matriz',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: grid;
      gap: var(--espacio-3);
    }
    fieldset {
      border: 0;
      padding: 0;
      margin: 0;
      display: flex;
      flex-wrap: wrap;
      gap: var(--espacio-3);
    }
    legend {
      font-weight: 600;
      margin-bottom: var(--espacio-2);
    }
    label {
      display: inline-flex;
      gap: 6px;
      align-items: center;
    }
  `,
  template: `
    <fieldset>
      <legend>Tallas</legend>
      @for (t of tallas(); track t.id) {
        <label
          ><input
            type="checkbox"
            data-talla
            [checked]="tallasMarcadas().has(t.id)"
            (change)="alternar(tallasMarcadas, t.id)"
          />
          {{ t.codigo }}</label
        >
      }
    </fieldset>
    <fieldset>
      <legend>Colores</legend>
      @for (c of colores(); track c.id) {
        <label
          ><input
            type="checkbox"
            data-color
            [checked]="coloresMarcados().has(c.id)"
            (change)="alternar(coloresMarcados, c.id)"
          />
          {{ c.nombre }}</label
        >
      }
    </fieldset>
    @if (yaExisten() > 0) {
      <p class="fs-suave">{{ yaExisten() }} ya existen y se omiten.</p>
    }
    <div>
      <button
        type="button"
        class="fs-btn fs-btn--primario"
        [disabled]="nuevas() === 0"
        (click)="emitir()"
      >
        Generar {{ nuevas() }} {{ nuevas() === 1 ? 'variante' : 'variantes' }}
      </button>
    </div>
  `,
})
export class GeneradorMatriz {
  readonly tallas = input<catalogApi.Talla[]>([]);
  readonly colores = input<catalogApi.Color[]>([]);
  readonly existentes = input<CombinacionVariante[]>([]);
  readonly generar = output<{ tallaIds: number[]; colorIds: number[] }>();

  protected readonly tallasMarcadas = signal<Set<number>>(new Set());
  protected readonly coloresMarcados = signal<Set<number>>(new Set());

  private readonly total = computed(() => this.tallasMarcadas().size * this.coloresMarcados().size);
  protected readonly yaExisten = computed(() => {
    const t = this.tallasMarcadas();
    const c = this.coloresMarcados();
    return this.existentes().filter((e) => t.has(e.tallaId) && c.has(e.colorId)).length;
  });
  protected readonly nuevas = computed(() => this.total() - this.yaExisten());

  protected alternar(conjunto: ReturnType<typeof signal<Set<number>>>, id: number): void {
    conjunto.update((actual) => {
      const nuevo = new Set(actual);
      if (!nuevo.delete(id)) nuevo.add(id);
      return nuevo;
    });
  }

  protected emitir(): void {
    const orden = <T extends { id: number }>(lista: T[], marcados: Set<number>) =>
      lista.filter((x) => marcados.has(x.id)).map((x) => x.id);
    this.generar.emit({
      tallaIds: orden(this.tallas(), this.tallasMarcadas()),
      colorIds: orden(this.colores(), this.coloresMarcados()),
    });
  }
}
