import { Component, computed, input, output } from '@angular/core';
import { FsBoton } from '../boton/boton';

/** Esqueleto de una vista que carga (sistema de diseño: Estados). */
@Component({
  selector: 'fs-estado-cargando',
  template: `
    <div
      role="status"
      aria-busy="true"
      aria-label="Cargando"
      style="display: grid; gap: var(--espacio-3)"
    >
      @for (f of lista(); track f) {
        <div class="fs-esqueleto" style="height: 20px"></div>
      }
    </div>
  `,
})
export class EstadoCargando {
  readonly filas = input(3);
  protected readonly lista = computed(() => Array.from({ length: this.filas() }, (_, i) => i));
}

/** Vista sin datos: dice qué falta y cómo empezar. */
@Component({
  selector: 'fs-estado-vacio',
  imports: [FsBoton],
  template: `
    <div class="fs-estado">
      <span class="fs-icono" aria-hidden="true">inbox</span>
      <h2 class="fs-estado__titulo">{{ titulo() }}</h2>
      @if (texto()) {
        <p class="fs-suave">{{ texto() }}</p>
      }
      @if (accion()) {
        <button type="button" fsBoton variante="primario" (click)="accionar.emit()">
          {{ accion() }}
        </button>
      }
    </div>
  `,
})
export class EstadoVacio {
  readonly titulo = input.required<string>();
  readonly texto = input('');
  readonly accion = input('');
  readonly accionar = output<void>();
}

/** Vista que falló: dice qué pasó y ofrece "Reintentar". */
@Component({
  selector: 'fs-estado-error',
  imports: [FsBoton],
  template: `
    <div class="fs-estado fs-estado--error" role="alert">
      <span class="fs-icono" aria-hidden="true">error</span>
      <h2 class="fs-estado__titulo">{{ titulo() }}</h2>
      <p class="fs-suave">{{ texto() }}</p>
      <button type="button" fsBoton variante="contorno" (click)="reintentar.emit()">
        Reintentar
      </button>
    </div>
  `,
})
export class EstadoError {
  readonly titulo = input('No pudimos cargar esto');
  readonly texto = input('Inténtalo en un momento.');
  readonly reintentar = output<void>();
}
