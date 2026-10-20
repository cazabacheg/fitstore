import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Página 404 de la tienda (CA-0b-19). */
@Component({
  selector: 'fs-pagina-404',
  imports: [RouterLink],
  template: `
    <section class="fs-pagina" style="padding-block: 48px">
      <div class="fs-estado">
        <span class="fs-icono" aria-hidden="true">search_off</span>
        <h1 class="fs-estado__titulo">Página no encontrada</h1>
        <p class="fs-suave">La dirección que abriste no existe o ya no está disponible.</p>
        <a class="fs-btn fs-btn--primario" routerLink="/">Volver al inicio</a>
      </div>
    </section>
  `,
})
export class Pagina404 {}
