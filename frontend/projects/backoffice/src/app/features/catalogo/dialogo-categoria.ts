import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import type { catalogApi } from 'shared';

export interface DatosDialogoCategoria {
  /** Sin categoría: se crea una nueva. */
  categoria: (catalogApi.CategoriaAdmin | catalogApi.CategoriaAdminHija) | null;
  raices: catalogApi.CategoriaAdmin[];
}

/** Crear o renombrar una categoría. Solo una raíz puede ser padre (máximo 2 niveles, CA-2-17). */
@Component({
  selector: 'fs-dialogo-categoria',
  imports: [MatDialogModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ datos.categoria ? 'Editar categoría' : 'Nueva categoría' }}</h2>
    <mat-dialog-content>
      <label class="fs-campo">
        <span class="fs-campo__etiqueta">Nombre</span>
        <input
          class="fs-input"
          name="nombre"
          [value]="nombre()"
          (input)="nombre.set($any($event.target).value)"
        />
      </label>
      <label class="fs-campo">
        <span class="fs-campo__etiqueta">Dentro de</span>
        <select
          class="fs-select"
          name="padre"
          [disabled]="!puedeCambiarPadre"
          (change)="padre.set(+$any($event.target).value || null)"
        >
          <option value="" [selected]="padre() === null">Ninguna (raíz)</option>
          @for (r of opciones; track r.id) {
            <option [value]="r.id" [selected]="padre() === r.id">{{ r.nombre }}</option>
          }
        </select>
      </label>
    </mat-dialog-content>
    <mat-dialog-actions>
      <button type="button" class="fs-btn fs-btn--contorno" [mat-dialog-close]="null">
        Cancelar
      </button>
      <button
        type="button"
        class="fs-btn fs-btn--primario"
        [disabled]="nombre().trim() === ''"
        [mat-dialog-close]="{ nombre: nombre().trim(), padreId: padre() }"
      >
        Guardar
      </button>
    </mat-dialog-actions>
  `,
})
export class DialogoCategoria {
  protected readonly datos = inject<DatosDialogoCategoria>(MAT_DIALOG_DATA);
  protected readonly nombre = signal(this.datos.categoria?.nombre ?? '');
  protected readonly padre = signal<number | null>(this.datos.categoria?.padreId ?? null);
  protected readonly opciones = this.datos.raices.filter((r) => r.id !== this.datos.categoria?.id);
  /** Una raíz con hijas no puede pasar a ser hija. */
  protected readonly puedeCambiarPadre = !(
    this.datos.categoria &&
    'hijas' in this.datos.categoria &&
    this.datos.categoria.hijas.length > 0
  );
}
