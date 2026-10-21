import { Component, computed, input, model } from '@angular/core';

let siguiente = 0;

/** Campo de texto del backoffice con etiqueta, ayuda y error (clases `fs-campo` y `fs-input` del sistema de diseño). */
@Component({
  selector: 'fs-campo-formulario',
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
        (input)="valor.set($any($event.target).value)"
      />
      @if (ayuda()) {
        <span class="fs-campo__ayuda" [id]="id + '-ayuda'">{{ ayuda() }}</span>
      }
      @if (error()) {
        <span class="fs-campo__error" [id]="id + '-error'">{{ error() }}</span>
      }
    </div>
  `,
})
export class CampoFormulario {
  protected readonly id = `fs-campo-bo-${siguiente++}`;

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
}
