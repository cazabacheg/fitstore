import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { EstadoServicio } from 'shared';

/** Banner "Servicio no disponible": aparece con un 503 y no rompe el resto de la app (CA-0b-13). */
@Component({
  selector: 'fs-banner-servicio-no-disponible',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (estado.noDisponible()) {
      <div class="fs-banner" role="alert">
        <span class="fs-icono" aria-hidden="true">cloud_off</span>
        <strong>Servicio no disponible</strong>
        <span>Inténtalo en un momento.</span>
      </div>
    }
  `,
  styles: `
    .fs-banner {
      display: flex;
      gap: var(--espacio-2);
      align-items: center;
      justify-content: center;
      padding: var(--espacio-2) var(--espacio-4);
      background: var(--fs-color-aviso-suave);
      color: var(--fs-color-aviso);
      font-size: 14px;
    }
  `,
})
export class BannerServicioNoDisponible {
  protected readonly estado = inject(EstadoServicio);
}
