import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import type { catalogApi } from 'shared';

/** Categorías raíz de la landing ("Compra por categoría"). Sin categorías (o con el catálogo caído) se oculta. */
@Component({
  selector: 'fs-categorias-raiz',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (categorias().length > 0) {
      <section class="fs-pagina seccion" data-testid="seccion-categorias">
        <h2 class="seccion__titulo">Compra por categoría</h2>
        <div class="categorias">
          @for (c of categorias(); track c.id) {
            <a class="categoria" data-testid="categoria" [routerLink]="['/categoria', c.slug]">
              <span class="categoria__nombre">{{ c.nombre }}</span>
              <span class="fs-icono categoria__flecha" aria-hidden="true">arrow_forward</span>
            </a>
          }
        </div>
      </section>
    }
  `,
  styles: `
    .seccion {
      padding-block: 64px 32px;
    }
    .seccion__titulo {
      margin: 0 0 28px;
      font-size: 26px;
      line-height: 32px;
      font-weight: 600;
      letter-spacing: -0.01em;
    }
    .categorias {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
      gap: 16px;
    }
    .categoria {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      min-height: 140px;
      padding: 20px;
      border-radius: var(--fs-radio-md, 8px);
      background: var(--fs-color-superficie-alterna);
      border: 1px solid var(--fs-color-linea);
      color: var(--fs-color-texto);
      text-decoration: none;
    }
    .categoria:hover {
      border-color: var(--fs-color-primario);
    }
    .categoria__nombre {
      font-size: 17px;
      line-height: 24px;
      font-weight: 600;
    }
    .categoria__flecha {
      color: var(--fs-color-primario-texto);
    }
  `,
})
export class CategoriasRaiz {
  readonly categorias = input<catalogApi.CategoriaNodo[]>([]);
}
