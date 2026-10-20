import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Título de la página y su acción principal (una sola por vista), usado por todas las pantallas del backoffice. */
@Component({
  selector: 'fs-encabezado-pagina',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="fs-encabezado-pagina">
      <h1 class="fs-titulo-pagina">{{ titulo() }}</h1>
      <div class="fs-encabezado-pagina__accion"><ng-content select="[accion]" /></div>
    </header>
  `,
  styles: `
    .fs-encabezado-pagina {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: var(--espacio-4);
      flex-wrap: wrap;
    }
  `,
})
export class EncabezadoPagina {
  readonly titulo = input.required<string>();
}
