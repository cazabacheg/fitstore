import { Component, inject } from '@angular/core';
import { EstadoServicio } from 'shared';

/** Franja "Servicio no disponible" que se enciende con un 503 y no bloquea el resto de la página. */
@Component({
  selector: 'fs-banner-servicio-no-disponible',
  template: `
    @if (estado.noDisponible()) {
      <div class="fs-banner" role="alert">
        <div class="fs-pagina">
          <span class="fs-icono" aria-hidden="true">cloud_off</span>
          <span><strong>Servicio no disponible.</strong> Inténtalo en un momento.</span>
        </div>
      </div>
    }
  `,
})
export class BannerServicioNoDisponible {
  protected readonly estado = inject(EstadoServicio);
}
