import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Estado vacío: dice qué falta y cómo empezar; la acción se proyecta como contenido. */
@Component({
  selector: 'fs-estado-vacio',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="fs-estado">
      <span class="fs-icono" aria-hidden="true">{{ icono() }}</span>
      <h2 class="fs-estado__titulo">{{ titulo() }}</h2>
      @if (texto()) {
        <p class="fs-suave">{{ texto() }}</p>
      }
      <ng-content />
    </section>
  `,
})
export class EstadoVacio {
  readonly titulo = input('Sin resultados');
  readonly texto = input('');
  readonly icono = input('inbox');
}
