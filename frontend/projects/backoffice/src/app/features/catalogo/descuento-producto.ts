import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  linkedSignal,
  output,
  viewChild,
} from '@angular/core';
import { ToastService, catalogApi } from 'shared';
import { EditorTramos } from './editor-tramos';

/** Pestaña Descuento: política global, sin descuento o tramos propios del producto (CA-2-38). */
@Component({
  selector: 'fs-descuento-producto',
  imports: [EditorTramos],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: grid;
      gap: var(--espacio-4);
      max-width: 560px;
    }
  `,
  template: `
    <label class="fs-campo">
      <span class="fs-campo__etiqueta">Descuento por volumen</span>
      <select
        class="fs-select"
        name="modoDescuento"
        [disabled]="soloLectura()"
        (change)="modo.set($any($event.target).value)"
      >
        <option value="GLOBAL" [selected]="modo() === 'GLOBAL'">
          Usar la política global de la tienda
        </option>
        <option value="PERSONALIZADO" [selected]="modo() === 'PERSONALIZADO'">
          Tramos propios de este producto
        </option>
        <option value="SIN_DESCUENTO" [selected]="modo() === 'SIN_DESCUENTO'">Sin descuento</option>
      </select>
    </label>
    @if (modo() === 'PERSONALIZADO') {
      <fs-editor-tramos [(tramos)]="tramos" [deshabilitado]="soloLectura()" />
    }
    @if (!soloLectura()) {
      <div>
        <button
          type="button"
          class="fs-btn fs-btn--primario"
          [disabled]="modo() === 'PERSONALIZADO' && !editor()?.valido()"
          (click)="guardar()"
        >
          Guardar descuento
        </button>
      </div>
    }
  `,
})
export class DescuentoProducto {
  private readonly api = inject(catalogApi.ProductosAdminService);
  private readonly toasts = inject(ToastService);

  readonly producto = input.required<catalogApi.ProductoAdmin>();
  readonly soloLectura = input(false);
  readonly cambio = output<catalogApi.ProductoAdmin>();

  protected readonly editor = viewChild(EditorTramos);
  protected readonly modo = linkedSignal<catalogApi.ModoDescuento>(
    () => this.producto().modoDescuento,
  );
  protected readonly tramos = linkedSignal<catalogApi.Tramo[]>(
    () => this.producto().tramosPropios ?? [],
  );

  protected guardar(): void {
    const personalizado = this.modo() === 'PERSONALIZADO';
    const cuerpo: catalogApi.DescuentoProductoRequest = personalizado
      ? { modo: this.modo(), tramos: this.tramos() }
      : { modo: this.modo() };
    this.api.configurarDescuentoProducto(this.producto().id, cuerpo).subscribe((r) => {
      if (r.data) this.cambio.emit(r.data);
      this.toasts.mostrar('exito', 'Descuento guardado.');
    });
  }
}
