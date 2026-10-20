import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/** Estado de error: qué pasó y "Reintentar". No bloquea el resto de la página. */
@Component({
  selector: 'fs-estado-error',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="fs-estado fs-estado--error" role="alert">
      <span class="fs-icono" aria-hidden="true">error</span>
      <h2 class="fs-estado__titulo">{{ titulo() }}</h2>
      @if (texto()) {
        <p class="fs-suave">{{ texto() }}</p>
      }
      <button type="button" class="fs-btn fs-btn--contorno" (click)="reintentar.emit()">
        Reintentar
      </button>
    </section>
  `,
})
export class EstadoError {
  readonly titulo = input('No pudimos cargar la información');
  readonly texto = input('');
  readonly reintentar = output<void>();
}
