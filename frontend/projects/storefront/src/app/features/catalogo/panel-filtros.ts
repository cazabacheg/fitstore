import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import type { catalogApi } from 'shared';
import type { Filtros } from './filtros';

/** Panel de filtros del listado: categorías, talla, color, precio y "solo disponibles". Emite el filtro nuevo completo. */
@Component({
  selector: 'fs-panel-filtros',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: grid;
      gap: var(--espacio-5, 20px);
      align-content: start;
    }
    fieldset {
      border: 0;
      padding: 0;
      margin: 0;
      display: grid;
      gap: 8px;
    }
    legend {
      font-weight: 600;
      margin-bottom: 8px;
    }
    label {
      display: flex;
      gap: 8px;
      align-items: center;
    }
    ul {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      gap: 6px;
    }
    ul ul {
      padding-left: 16px;
      margin-top: 6px;
    }
    .precio {
      display: flex;
      gap: 8px;
    }
    .precio input {
      width: 100%;
    }
    .muestra {
      width: 14px;
      height: 14px;
      border-radius: 50%;
      border: 1px solid var(--fs-color-linea, #ddd);
    }
  `,
  template: `
    <nav aria-label="Categorías">
      <strong>Categorías</strong>
      <ul>
        <li><a [routerLink]="['/productos']">Todos los productos</a></li>
        @for (c of arbol(); track c.id) {
          <li>
            <a [routerLink]="['/categoria', c.slug]">{{ c.nombre }}</a>
            @if (c.hijas.length) {
              <ul>
                @for (h of c.hijas; track h.id) {
                  <li>
                    <a [routerLink]="['/categoria', h.slug]">{{ h.nombre }}</a>
                  </li>
                }
              </ul>
            }
          </li>
        }
      </ul>
    </nav>
    <fieldset>
      <legend>Talla</legend>
      @for (t of tallas(); track t.id) {
        <label>
          <input
            type="checkbox"
            [attr.data-filtro]="'talla-' + t.codigo"
            [checked]="filtros().tallas.includes(t.codigo)"
            (change)="alternarTalla(t.codigo)"
          />
          {{ t.codigo }}
        </label>
      }
    </fieldset>
    <fieldset>
      <legend>Color</legend>
      @for (c of colores(); track c.id) {
        <label>
          <input
            type="checkbox"
            [attr.data-filtro]="'color-' + c.id"
            [checked]="filtros().colores.includes(c.id)"
            (change)="alternarColor(c.id)"
          />
          <span class="muestra" [style.background]="c.hex"></span>
          {{ c.nombre }}
        </label>
      }
    </fieldset>
    <fieldset>
      <legend>Precio (S/)</legend>
      <div class="precio">
        <input
          type="number"
          min="0"
          aria-label="Precio mínimo"
          data-filtro="min"
          [value]="filtros().min ?? ''"
          (change)="precio('min', $any($event.target).value)"
        />
        <input
          type="number"
          min="0"
          aria-label="Precio máximo"
          data-filtro="max"
          [value]="filtros().max ?? ''"
          (change)="precio('max', $any($event.target).value)"
        />
      </div>
    </fieldset>
    <label>
      <input
        type="checkbox"
        data-filtro="disponible"
        [checked]="filtros().disponible"
        (change)="cambio.emit({ ...filtros(), disponible: !filtros().disponible, pagina: 0 })"
      />
      Solo disponibles
    </label>
  `,
})
export class PanelFiltros {
  readonly filtros = input.required<Filtros>();
  readonly arbol = input<catalogApi.CategoriaNodo[]>([]);
  readonly tallas = input<catalogApi.Talla[]>([]);
  readonly colores = input<catalogApi.Color[]>([]);
  readonly cambio = output<Filtros>();

  protected alternarTalla(codigo: string): void {
    const f = this.filtros();
    const tallas = f.tallas.includes(codigo)
      ? f.tallas.filter((t) => t !== codigo)
      : [...f.tallas, codigo];
    this.cambio.emit({ ...f, tallas, pagina: 0 });
  }

  protected alternarColor(id: number): void {
    const f = this.filtros();
    const colores = f.colores.includes(id) ? f.colores.filter((c) => c !== id) : [...f.colores, id];
    this.cambio.emit({ ...f, colores, pagina: 0 });
  }

  protected precio(lado: 'min' | 'max', texto: string): void {
    const n = texto.trim() === '' ? null : Number(texto);
    this.cambio.emit({ ...this.filtros(), [lado]: Number.isFinite(n) ? n : null, pagina: 0 });
  }
}
