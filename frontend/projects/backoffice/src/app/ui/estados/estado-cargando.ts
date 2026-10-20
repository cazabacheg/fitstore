import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Esqueleto con la forma de lo que viene (filas); nunca un spinner a pantalla completa. */
@Component({
  selector: 'fs-estado-cargando',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="fs-cargando" role="status" aria-label="Cargando">
      @for (n of lineasLista(); track n) {
        <div class="fs-esqueleto fs-cargando__linea"></div>
      }
    </div>
  `,
  styles: `
    .fs-cargando {
      display: grid;
      gap: var(--espacio-3);
      padding: var(--espacio-4);
    }
    .fs-cargando__linea {
      height: 20px;
    }
  `,
})
export class EstadoCargando {
  readonly lineas = input(5);
  protected readonly lineasLista = computed(() =>
    Array.from({ length: this.lineas() }, (_, i) => i),
  );
}
