import { CdkDrag, CdkDragHandle, CdkDropList } from '@angular/cdk/drag-drop';
import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, model, output } from '@angular/core';
import type { catalogApi } from 'shared';

type Nodo = catalogApi.CategoriaAdmin | catalogApi.CategoriaAdminHija;

/** Árbol de categorías de 2 niveles, arrastrable dentro de cada grupo de hermanas (CA-2-18). Sin tabla (R11 no aplica). */
@Component({
  selector: 'fs-arbol-categorias',
  imports: [CdkDropList, CdkDrag, CdkDragHandle, NgTemplateOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    ul {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      gap: 6px;
    }
    ul ul {
      margin: 6px 0 0 28px;
    }
    .nodo {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 6px 10px;
      border: 1px solid var(--fs-color-linea);
      border-radius: var(--radio-md);
      background: var(--fs-color-superficie);
    }
    .nodo[aria-current='true'] {
      border-color: var(--fs-color-primario);
    }
    .nodo.inactiva .nombre {
      opacity: 0.55;
      text-decoration: line-through;
    }
    .asa {
      cursor: grab;
      color: var(--fs-color-texto-suave);
    }
    .nombre {
      flex: 1;
      text-align: left;
      border: 0;
      background: transparent;
      font: inherit;
      cursor: pointer;
    }
    .cuenta {
      color: var(--fs-color-texto-suave);
      font-size: 13px;
    }
  `,
  template: `
    <ul cdkDropList (cdkDropListDropped)="soltar($event, categorias())">
      @for (c of categorias(); track c.id) {
        <li cdkDrag>
          <ng-container *ngTemplateOutlet="nodo; context: { $implicit: c }" />
          @if (c.hijas.length) {
            <ul cdkDropList (cdkDropListDropped)="soltar($event, c.hijas)">
              @for (h of c.hijas; track h.id) {
                <li cdkDrag>
                  <ng-container *ngTemplateOutlet="nodo; context: { $implicit: h }" />
                </li>
              }
            </ul>
          }
        </li>
      }
    </ul>
    <ng-template #nodo let-n>
      <div
        class="nodo"
        [class.inactiva]="!n.activo"
        [attr.data-categoria]="n.id"
        [attr.aria-current]="seleccionada() === n.id"
      >
        <span class="asa" aria-hidden="true" cdkDragHandle>⠿</span>
        <button
          type="button"
          class="nombre"
          data-seleccionar
          (click)="seleccionada.set(n.id); elegir.emit(n)"
        >
          {{ n.nombre }}
        </button>
        <span class="cuenta">{{ n.cantidadProductos }} productos</span>
        <button
          type="button"
          class="fs-btn fs-btn--texto fs-btn--sm"
          [attr.aria-label]="'Editar ' + n.nombre"
          (click)="editar.emit(n)"
        >
          Editar
        </button>
        <button
          type="button"
          class="fs-btn fs-btn--texto fs-btn--sm"
          [attr.aria-label]="(n.activo ? 'Desactivar ' : 'Activar ') + n.nombre"
          (click)="alternar.emit(n)"
        >
          {{ n.activo ? 'Desactivar' : 'Activar' }}
        </button>
        <button
          type="button"
          class="fs-btn fs-btn--texto fs-btn--sm"
          [attr.aria-label]="'Eliminar ' + n.nombre"
          (click)="eliminar.emit(n)"
        >
          Eliminar
        </button>
      </div>
    </ng-template>
  `,
})
export class ArbolCategorias {
  readonly categorias = input<catalogApi.CategoriaAdmin[]>([]);
  readonly seleccionada = model<number | null>(null);
  readonly elegir = output<Nodo>();
  readonly editar = output<Nodo>();
  readonly alternar = output<Nodo>();
  readonly eliminar = output<Nodo>();
  /** Nueva posición (desde 0) de una categoría entre sus hermanas. */
  readonly ordenar = output<{ id: number; orden: number }>();

  /** Un arrastre dentro de un grupo de hermanas: `lista` es ese grupo en el orden mostrado. */
  soltar(evento: { previousIndex: number; currentIndex: number }, lista: Nodo[]): void {
    if (evento.previousIndex === evento.currentIndex) return;
    this.ordenar.emit({ id: lista[evento.previousIndex].id, orden: evento.currentIndex });
  }
}
