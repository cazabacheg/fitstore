import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import type { catalogApi } from 'shared';

/** Tabla editable de la guía de tallas: filas y columnas que se agregan y se quitan (CA-2-20). */
@Component({
  selector: 'fs-editor-guia-tallas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: grid;
      gap: var(--espacio-3);
    }
    .envoltorio {
      overflow-x: auto;
    }
    table {
      border-collapse: collapse;
    }
    th,
    td {
      padding: 4px;
    }
    input {
      width: 110px;
    }
  `,
  template: `
    <div class="envoltorio">
      <table>
        <thead>
          <tr>
            @for (c of guia().columnas; track $index; let j = $index) {
              <th>
                <input
                  class="fs-input"
                  [attr.aria-label]="'Encabezado de la columna ' + (j + 1)"
                  [value]="c"
                  [disabled]="deshabilitado()"
                  (input)="cambiarColumna(j, $any($event.target).value)"
                />
                @if (!deshabilitado()) {
                  <button
                    type="button"
                    class="fs-btn fs-btn--texto fs-btn--sm"
                    [attr.aria-label]="'Quitar columna ' + (j + 1)"
                    (click)="quitarColumna(j)"
                  >
                    ×
                  </button>
                }
              </th>
            }
            <th></th>
          </tr>
        </thead>
        <tbody>
          @for (fila of guia().filas; track $index; let i = $index) {
            <tr>
              @for (celda of fila; track $index; let j = $index) {
                <td>
                  <input
                    class="fs-input"
                    [attr.aria-label]="
                      'Fila ' + (i + 1) + ', columna ' + (guia().columnas[j] || j + 1)
                    "
                    [value]="celda"
                    [disabled]="deshabilitado()"
                    (input)="cambiarCelda(i, j, $any($event.target).value)"
                  />
                </td>
              }
              <td>
                @if (!deshabilitado()) {
                  <button
                    type="button"
                    class="fs-btn fs-btn--texto fs-btn--sm"
                    [attr.aria-label]="'Quitar fila ' + (i + 1)"
                    (click)="quitarFila(i)"
                  >
                    ×
                  </button>
                }
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
    @if (!deshabilitado()) {
      <div>
        <button type="button" class="fs-btn fs-btn--contorno fs-btn--sm" (click)="agregarFila()">
          Agregar fila
        </button>
        <button type="button" class="fs-btn fs-btn--contorno fs-btn--sm" (click)="agregarColumna()">
          Agregar columna
        </button>
      </div>
    }
  `,
})
export class EditorGuiaTallas {
  readonly guia = model.required<catalogApi.GuiaTallas>();
  readonly deshabilitado = input(false);

  protected cambiarColumna(j: number, valor: string): void {
    this.guia.update((g) => ({ ...g, columnas: g.columnas.map((c, k) => (k === j ? valor : c)) }));
  }
  protected cambiarCelda(i: number, j: number, valor: string): void {
    this.guia.update((g) => ({
      ...g,
      filas: g.filas.map((f, a) => (a === i ? f.map((c, b) => (b === j ? valor : c)) : f)),
    }));
  }
  protected agregarFila(): void {
    this.guia.update((g) => ({ ...g, filas: [...g.filas, g.columnas.map(() => '')] }));
  }
  protected quitarFila(i: number): void {
    this.guia.update((g) => ({ ...g, filas: g.filas.filter((_, a) => a !== i) }));
  }
  protected agregarColumna(): void {
    this.guia.update((g) => ({
      ...g,
      columnas: [...g.columnas, ''],
      filas: g.filas.map((f) => [...f, '']),
    }));
  }
  protected quitarColumna(j: number): void {
    this.guia.update((g) => ({
      ...g,
      columnas: g.columnas.filter((_, k) => k !== j),
      filas: g.filas.map((f) => f.filter((_, k) => k !== j)),
    }));
  }
}
