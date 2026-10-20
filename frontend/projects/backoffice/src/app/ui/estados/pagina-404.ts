import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Ruta que no existe (CA-0b-19). */
@Component({
  selector: 'fs-pagina-404',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="fs-estado">
      <span class="fs-icono" aria-hidden="true">search_off</span>
      <h1 class="fs-estado__titulo">Página no encontrada</h1>
      <p class="fs-suave">La dirección que abriste no existe o cambió de lugar.</p>
      <a class="fs-btn fs-btn--primario" routerLink="/">Volver al inicio</a>
    </section>
  `,
})
export class Pagina404 {}
