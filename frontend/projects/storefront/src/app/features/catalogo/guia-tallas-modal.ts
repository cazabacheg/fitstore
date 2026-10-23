import { ChangeDetectionStrategy, Component, inject, input, model } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TiendaService } from 'shared/api/catalog-service';
import { EMPTY, map } from 'rxjs';
import { FsDialogo } from '../../ui/dialogo/dialogo';

/** Guía de tallas de la categoría del producto (o la de su padre, según el servidor). */
@Component({
  selector: 'fs-guia-tallas-modal',
  imports: [FsDialogo],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    table {
      width: 100%;
      border-collapse: collapse;
    }
    th,
    td {
      border-width: 1px;
      border-style: solid;
      border-color: var(--fs-color-linea, #ddd);
      padding: 8px 12px;
      text-align: center;
    }
    th {
      background: var(--fs-color-superficie-alterna, #f5f5f5);
      font-weight: 600;
    }
  `,
  template: `
    <fs-dialogo titulo="Guía de tallas" [cerrarConFondo]="true" [(abierto)]="abierto">
      @if (guia.value(); as g) {
        <table class="fs-tabla-guia">
          <thead>
            <tr>
              @for (c of g.columnas; track $index) {
                <th>{{ c }}</th>
              }
            </tr>
          </thead>
          <tbody>
            @for (fila of g.filas; track $index) {
              <tr>
                @for (celda of fila; track $index) {
                  <td>{{ celda }}</td>
                }
              </tr>
            }
          </tbody>
        </table>
        @if (g.nota) {
          <p>{{ g.nota }}</p>
        }
      } @else {
        <p>Cargando…</p>
      }
      <button
        fsAcciones
        type="button"
        class="fs-btn fs-btn--secundario"
        (click)="abierto.set(false)"
      >
        Cerrar
      </button>
    </fs-dialogo>
  `,
})
export class GuiaTallasModal {
  private readonly tienda = inject(TiendaService);
  readonly categoria = input.required<string>();
  readonly abierto = model(false);

  /** La tabla se pide la primera vez que se abre. */
  protected readonly guia = rxResource({
    params: () => (this.abierto() ? this.categoria() : undefined),
    stream: ({ params }) =>
      params ? this.tienda.obtenerGuiaTallas(params).pipe(map((r) => r.data ?? null)) : EMPTY,
  });
}
