import { Component, computed, input, model } from '@angular/core';
import { idUnico } from '../id-unico';

/** Campo de formulario con etiqueta, ayuda y error (sistema de diseño: Campo). */
@Component({
  selector: 'fs-campo',
  template: `
    <div class="fs-campo" [class.fs-campo--error]="!!error()">
      <label class="fs-campo__etiqueta" [for]="id">{{ etiqueta() }}</label>
      <input
        class="fs-input"
        [id]="id"
        [type]="tipo()"
        [placeholder]="placeholder()"
        [disabled]="deshabilitado()"
        [value]="valor()"
        [attr.aria-invalid]="error() ? 'true' : null"
        [attr.aria-describedby]="descripcion()"
        (input)="alEscribir($event)"
      />
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
export class FsCampo {
  protected readonly id = idUnico('fs-campo');

  readonly etiqueta = input.required<string>();
  readonly tipo = input('text');
  readonly placeholder = input('');
  readonly ayuda = input('');
  readonly error = input('');
  readonly deshabilitado = input(false);
  readonly valor = model('');

  protected readonly descripcion = computed(() => {
    const ids = [this.ayuda() ? `${this.id}-ayuda` : '', this.error() ? `${this.id}-error` : ''];
    return ids.filter(Boolean).join(' ') || null;
  });

  protected alEscribir(evento: Event): void {
    this.valor.set((evento.target as HTMLInputElement).value);
  }
}
