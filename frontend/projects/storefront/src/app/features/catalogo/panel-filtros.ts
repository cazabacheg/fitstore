import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import type { catalogApi } from 'shared';
import { capitalizar } from '../../ui/capitalizar';
import { aQuery, type Filtros } from './filtros';

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
      margin-bottom: 12px;
    }
    label {
      display: flex;
      gap: 8px;
      align-items: center;
    }
    .titulo {
      display: block;
      font-weight: 600;
      margin-bottom: 12px;
    }
    nav a {
      color: inherit;
      text-decoration: none;
      display: inline-block;
      padding-block: 2px;
    }
    nav a:hover {
      text-decoration: underline;
    }
    nav a.activa,
    nav a.activa-padre {
      font-weight: 700;
      color: var(--fs-color-primario-texto);
    }
    nav a.activa {
      text-decoration: underline;
      text-underline-offset: 4px;
    }
    .interruptor {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;
      font-weight: 600;
      cursor: pointer;
    }
    .interruptor input {
      appearance: none;
      flex: none;
      position: relative;
      width: 40px;
      height: 22px;
      margin: 0;
      border-radius: 999px;
      background: var(--fs-color-borde-control, #8a8478);
      cursor: pointer;
      transition: background 0.15s;
    }
    .interruptor input::after {
      content: '';
      position: absolute;
      top: 2px;
      left: 2px;
      width: 18px;
      height: 18px;
      border-radius: 50%;
      background: var(--fs-color-superficie, #fff);
      transition: transform 0.15s;
    }
    .interruptor input:checked {
      background: var(--fs-color-primario-texto, #000);
    }
    .interruptor input:checked::after {
      transform: translateX(18px);
    }
    .interruptor input:focus-visible {
      outline: 2px solid var(--fs-color-foco, #000);
      outline-offset: 2px;
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
    <label class="interruptor">
      Solo disponibles
      <input
        type="checkbox"
        role="switch"
        data-filtro="disponible"
        [checked]="filtros().disponible"
        (change)="cambio.emit({ ...filtros(), disponible: !filtros().disponible, pagina: 0 })"
      />
    </label>
    <nav aria-label="Categorías">
      <strong class="titulo">Categorías</strong>
      <ul>
        <li>
          <a
            [routerLink]="['/productos']"
            [queryParams]="parametros()"
            [class.activa]="!filtros().categoria"
            [attr.aria-current]="!filtros().categoria ? 'page' : null"
            >Todos los productos</a
          >
        </li>
        @for (c of arbol(); track c.id) {
          <li>
            <a
              [routerLink]="['/categoria', c.slug]"
              [queryParams]="parametros()"
              [class.activa]="filtros().categoria === c.slug"
              [class.activa-padre]="esPadreActiva(c)"
              [attr.aria-current]="filtros().categoria === c.slug ? 'page' : null"
              >{{ c.nombre }}</a
            >
            @if (c.hijas.length) {
              <ul>
                @for (h of c.hijas; track h.id) {
                  <li>
                    <a
                      [routerLink]="['/categoria', h.slug]"
                      [queryParams]="parametros()"
                      [class.activa]="filtros().categoria === h.slug"
                      [attr.aria-current]="filtros().categoria === h.slug ? 'page' : null"
                      >{{ h.nombre }}</a
                    >
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
          {{ nombreColor(c.nombre) }}
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
  `,
})
export class PanelFiltros {
  readonly filtros = input.required<Filtros>();
  readonly arbol = input<catalogApi.CategoriaNodo[]>([]);
  readonly tallas = input<catalogApi.Talla[]>([]);
  readonly colores = input<catalogApi.Color[]>([]);
  readonly cambio = output<Filtros>();

  /** Filtros que viajan al cambiar de categoría: todos, menos la página (la categoría nueva empieza en la primera). */
  protected readonly parametros = computed(() => aQuery({ ...this.filtros(), pagina: 0 }));
  protected readonly nombreColor = capitalizar;

  /** La categoría padre de la activa también se resalta. */
  protected esPadreActiva(c: catalogApi.CategoriaNodo): boolean {
    const actual = this.filtros().categoria;
    return !!actual && c.hijas.some((h) => h.slug === actual);
  }

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
