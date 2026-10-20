import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Sección a la que el rol de la sesión no tiene acceso (CA-0b-17). */
@Component({
  selector: 'fs-pagina-403',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="fs-estado fs-estado--error">
      <span class="fs-icono" aria-hidden="true">lock</span>
      <h1 class="fs-estado__titulo">No tienes permiso para ver esta sección</h1>
      <p class="fs-suave">Pide acceso a un administrador si lo necesitas.</p>
      <a class="fs-btn fs-btn--primario" routerLink="/">Volver al inicio</a>
    </section>
  `,
})
export class Pagina403 {}
