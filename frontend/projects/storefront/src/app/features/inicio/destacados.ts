import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import type { catalogApi } from 'shared';
import { TarjetaDestacado } from './tarjeta-destacado';

/** Cantidad máxima de destacados en la landing (CA-3-04). */
export const MAXIMO_DESTACADOS = 8;

/** Sección "Destacados": hasta 8 productos en el orden de la API. Sin destacados no se muestra. */
@Component({
  selector: 'fs-destacados',
  imports: [RouterLink, TarjetaDestacado],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (visibles().length > 0) {
      <section class="fs-pagina seccion" data-testid="seccion-destacados">
        <div class="seccion__cabecera">
          <h2 class="seccion__titulo">Destacados</h2>
          <a class="fs-btn fs-btn--texto" routerLink="/productos">Ver todo ›</a>
        </div>
        <div class="fs-rejilla-4">
          @for (p of visibles(); track p.id) {
            <fs-tarjeta-destacado [producto]="p" />
          }
        </div>
      </section>
    }
  `,
  styles: `
    .seccion {
      padding-block: 64px;
    }
    .seccion__cabecera {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      gap: 16px;
      margin-bottom: 28px;
    }
    .seccion__titulo {
      margin: 0;
      font-size: 26px;
      line-height: 32px;
      font-weight: 600;
      letter-spacing: -0.01em;
    }
  `,
})
export class Destacados {
  readonly productos = input<catalogApi.ProductoTarjeta[]>([]);
  protected readonly visibles = computed(() => this.productos().slice(0, MAXIMO_DESTACADOS));
}
