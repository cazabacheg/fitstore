import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import type { catalogApi } from 'shared';
import { capitalizar } from '../../ui/capitalizar';
import type { Filtros } from './filtros';

/** Chips de los filtros activos; cada uno quita su filtro. */
@Component({
  selector: 'fs-chips-filtros-activos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
    .chip-filtro {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 4px 4px 4px 12px;
      border-radius: 999px;
      background: var(--fs-color-primario-suave, #eef);
      font-size: 14px;
    }
    .chip-filtro button {
      border: 0;
      background: transparent;
      cursor: pointer;
      font-size: 16px;
      line-height: 1;
      padding: 2px 6px;
    }
  `,
  template: `
    @for (chip of chips(); track chip.clave) {
      <span class="chip-filtro">
        {{ chip.texto }}
        <button
          type="button"
          [attr.aria-label]="'Quitar filtro ' + chip.texto"
          (click)="cambio.emit(chip.sin(filtros()))"
        >
          ×
        </button>
      </span>
    }
  `,
})
export class ChipsFiltrosActivos {
  readonly filtros = input.required<Filtros>();
  readonly colores = input<catalogApi.Color[]>([]);
  readonly cambio = output<Filtros>();

  protected readonly chips = computed(() => {
    const f = this.filtros();
    const lista: { clave: string; texto: string; sin: (f: Filtros) => Filtros }[] = [];
    if (f.q)
      lista.push({
        clave: 'q',
        texto: `Búsqueda: ${f.q}`,
        sin: (x) => ({ ...x, q: '', pagina: 0 }),
      });
    for (const t of f.tallas)
      lista.push({
        clave: 't' + t,
        texto: `Talla ${t}`,
        sin: (x) => ({ ...x, tallas: x.tallas.filter((y) => y !== t), pagina: 0 }),
      });
    for (const id of f.colores) {
      const nombre = capitalizar(this.colores().find((c) => c.id === id)?.nombre ?? `Color ${id}`);
      lista.push({
        clave: 'c' + id,
        texto: nombre,
        sin: (x) => ({ ...x, colores: x.colores.filter((y) => y !== id), pagina: 0 }),
      });
    }
    if (f.min !== null || f.max !== null)
      lista.push({
        clave: 'p',
        texto: `S/ ${f.min ?? 0} – ${f.max !== null ? 'S/ ' + f.max : 'más'}`,
        sin: (x) => ({ ...x, min: null, max: null, pagina: 0 }),
      });
    if (f.disponible)
      lista.push({
        clave: 'd',
        texto: 'Solo disponibles',
        sin: (x) => ({ ...x, disponible: false, pagina: 0 }),
      });
    return lista;
  });
}
