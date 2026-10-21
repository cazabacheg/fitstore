import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { ToastService, catalogApi } from 'shared';
import { DialogoService } from '../../ui/dialogo/dialogo.service';
import { GeneradorMatriz } from './generador-matriz';

/** Pestaña Variantes: generador de la matriz y tabla de variantes (SKU, precio propio, activar, eliminar). */
@Component({
  selector: 'fs-variantes-producto',
  imports: [GeneradorMatriz],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: grid;
      gap: var(--espacio-5);
    }
    table {
      width: 100%;
      border-collapse: collapse;
    }
    th,
    td {
      text-align: left;
      padding: 8px;
      border-bottom: 1px solid var(--fs-color-linea);
    }
    td.num {
      text-align: right;
    }
    .inactiva {
      opacity: 0.55;
    }
    input.precio {
      width: 110px;
    }
  `,
  template: `
    @if (!soloLectura()) {
      <fs-generador-matriz
        [tallas]="tallas()"
        [colores]="colores()"
        [existentes]="existentes()"
        (generar)="generar($event)"
      />
    }
    @if (producto().variantes.length === 0) {
      <p class="fs-suave">Aún no hay variantes. Genera la matriz de tallas y colores.</p>
    } @else {
      <table>
        <thead>
          <tr>
            <th>SKU</th>
            <th>Talla</th>
            <th>Color</th>
            <th>Precio propio (S/)</th>
            <th class="num">Físico</th>
            <th class="num">Disponible</th>
            <th>Estado</th>
            @if (!soloLectura()) {
              <th>Acciones</th>
            }
          </tr>
        </thead>
        <tbody>
          @for (v of producto().variantes; track v.id) {
            <tr [attr.data-variante]="v.id" [class.inactiva]="!v.activo">
              <td>{{ v.sku }}</td>
              <td>{{ v.talla }}</td>
              <td>{{ v.color }}</td>
              <td>
                <input
                  class="fs-input precio"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="Base"
                  [attr.aria-label]="'Precio de ' + v.sku"
                  [value]="precios()[v.id] ?? v.precio ?? ''"
                  [disabled]="soloLectura()"
                  (input)="poner(v.id, $any($event.target).value)"
                />
              </td>
              <td class="num">{{ v.fisico }}</td>
              <td class="num">{{ v.disponible }}</td>
              <td>{{ v.activo ? 'Activa' : 'Inactiva' }}</td>
              @if (!soloLectura()) {
                <td>
                  <button
                    type="button"
                    class="fs-btn fs-btn--texto fs-btn--sm"
                    (click)="guardar(v)"
                  >
                    Guardar
                  </button>
                  <button
                    type="button"
                    class="fs-btn fs-btn--texto fs-btn--sm"
                    (click)="alternar(v)"
                  >
                    {{ v.activo ? 'Desactivar' : 'Activar' }}
                  </button>
                  <button
                    type="button"
                    class="fs-btn fs-btn--texto fs-btn--sm"
                    (click)="eliminar(v)"
                  >
                    Eliminar
                  </button>
                </td>
              }
            </tr>
          }
        </tbody>
      </table>
    }
  `,
})
export class VariantesProducto {
  private readonly api = inject(catalogApi.ProductosAdminService);
  private readonly dialogo = inject(DialogoService);
  private readonly toasts = inject(ToastService);

  readonly producto = input.required<catalogApi.ProductoAdmin>();
  readonly tallas = input<catalogApi.Talla[]>([]);
  readonly colores = input<catalogApi.Color[]>([]);
  readonly soloLectura = input(false);
  readonly cambio = output<catalogApi.ProductoAdmin>();
  /** Se borró una variante: la respuesta no trae el producto, así que se pide que se recargue. */
  readonly recargar = output<void>();

  protected readonly precios = signal<Record<number, string>>({});
  protected readonly existentes = computed(() =>
    this.producto().variantes.map((v) => ({ tallaId: v.tallaId, colorId: v.colorId })),
  );

  protected poner(id: number, texto: string): void {
    this.precios.update((p) => ({ ...p, [id]: texto }));
  }

  protected generar(matriz: { tallaIds: number[]; colorIds: number[] }): void {
    this.api.generarMatrizVariantes(this.producto().id, matriz).subscribe((r) => {
      if (r.data) this.cambio.emit(r.data);
      this.toasts.mostrar('exito', 'Variantes generadas.');
    });
  }

  protected guardar(v: catalogApi.VarianteAdmin): void {
    const texto = this.precios()[v.id];
    const precio =
      texto === undefined ? (v.precio ?? null) : texto.trim() === '' ? null : Number(texto);
    this.api.editarVariante(v.id, { sku: v.sku, precio }).subscribe((r) => {
      if (r.data) this.cambio.emit(r.data);
      this.toasts.mostrar('exito', 'Variante guardada.');
    });
  }

  protected alternar(v: catalogApi.VarianteAdmin): void {
    (v.activo ? this.api.desactivarVariante(v.id) : this.api.activarVariante(v.id)).subscribe(
      (r) => {
        if (r.data) this.cambio.emit(r.data);
      },
    );
  }

  protected eliminar(v: catalogApi.VarianteAdmin): void {
    this.dialogo
      .confirmar({
        titulo: `¿Eliminar la variante ${v.sku}?`,
        mensaje: 'Si tiene movimientos, usa "Desactivar".',
        accion: 'Eliminar',
        peligro: true,
      })
      .subscribe((si) => {
        if (si) this.api.eliminarVariante(v.id).subscribe(() => this.recargar.emit());
      });
  }
}
