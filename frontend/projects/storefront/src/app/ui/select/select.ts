import { Component, computed, input, model } from '@angular/core';
import { idUnico } from '../id-unico';

export interface OpcionSelect {
  valor: string;
  etiqueta: string;
}

/** Select nativo con etiqueta, ayuda y error (sistema de diseño: Campo con `fs-select`). */
@Component({
  selector: 'fs-select',
  template: `
    <div class="fs-campo" [class.fs-campo--error]="!!error()">
      <label class="fs-campo__etiqueta" [for]="id">{{ etiqueta() }}</label>
      <select
        class="fs-select"
        [id]="id"
        [disabled]="deshabilitado()"
        [attr.aria-invalid]="error() ? 'true' : null"
        [attr.aria-describedby]="descripcion()"
        (change)="alElegir($event)"
      >
        @if (placeholder()) {
          <option value="" [selected]="!valor()">{{ placeholder() }}</option>
        }
        @for (o of opciones(); track o.valor) {
          <option [value]="o.valor" [selected]="o.valor === valor()">{{ o.etiqueta }}</option>
        }
      </select>
      @if (ayuda()) {
        <span class="fs-campo__ayuda" [id]="id + '-ayuda'">{{ ayuda() }}</span>
      }
      @if (error()) {
        <span class="fs-campo__error" [id]="id + '-error'">
          <span class="fs-icono" aria-hidden="true">error</span>{{ error() }}
        </span>
      }
    </div>
  `,
})
export class FsSelect {
  protected readonly id = idUnico('fs-select');

  readonly etiqueta = input.required<string>();
  readonly opciones = input<OpcionSelect[]>([]);
  readonly placeholder = input('');
  readonly ayuda = input('');
  readonly error = input('');
  readonly deshabilitado = input(false);
  readonly valor = model('');

  protected readonly descripcion = computed(() => {
    const ids = [this.ayuda() ? `${this.id}-ayuda` : '', this.error() ? `${this.id}-error` : ''];
    return ids.filter(Boolean).join(' ') || null;
  });

  protected alElegir(evento: Event): void {
    this.valor.set((evento.target as HTMLSelectElement).value);
  }
}
