import { ChangeDetectionStrategy, Component, inject, signal, viewChild } from '@angular/core';
import { ToastService, catalogApi } from 'shared';
import { MaestrosAdminService } from 'shared/api/catalog-service';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { EstadoCargando } from '../../ui/estados/estado-cargando';
import { EditorTramos } from './editor-tramos';

/** P-2-06 (segunda mitad): política global de descuentos por volumen (CA-2-36, CA-2-37). */
@Component({
  selector: 'fs-descuentos-page',
  imports: [EncabezadoPagina, EditorTramos, EstadoCargando],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: grid;
      gap: var(--espacio-5);
    }
    section {
      display: grid;
      gap: var(--espacio-4);
      max-width: 560px;
    }
    label.interruptor {
      display: flex;
      gap: 8px;
      align-items: center;
    }
  `,
  template: `
    <fs-encabezado-pagina titulo="Descuentos por volumen" />
    @if (cargado()) {
      <section>
        <label class="interruptor">
          <input
            type="checkbox"
            name="activo"
            [checked]="activo()"
            (change)="activo.set(!activo())"
          />
          Aplicar descuentos por volumen en toda la tienda
        </label>
        @if (!activo()) {
          <p class="fs-suave">
            Con el interruptor apagado ninguna cotización aplica descuentos y la tienda dejará de
            mostrar las etiquetas de descuento.
          </p>
        }
        <fs-editor-tramos [(tramos)]="tramos" />
        <div>
          <button
            type="button"
            class="fs-btn fs-btn--primario"
            [disabled]="!editor()?.valido()"
            (click)="guardar()"
          >
            Guardar
          </button>
        </div>
      </section>
    } @else {
      <fs-estado-cargando />
    }
  `,
})
export class DescuentosPage {
  private readonly api = inject(MaestrosAdminService);
  private readonly toasts = inject(ToastService);

  protected readonly editor = viewChild(EditorTramos);
  protected readonly cargado = signal(false);
  protected readonly activo = signal(true);
  protected readonly tramos = signal<catalogApi.Tramo[]>([]);

  constructor() {
    this.api.obtenerPoliticaDescuentos().subscribe((r) => {
      this.activo.set(r.data?.activo ?? true);
      this.tramos.set(r.data?.tramos ?? []);
      this.cargado.set(true);
    });
  }

  protected guardar(): void {
    this.api
      .guardarPoliticaDescuentos({ activo: this.activo(), tramos: this.tramos() })
      .subscribe((r) => {
        if (r.data) {
          this.activo.set(r.data.activo);
          this.tramos.set(r.data.tramos);
        }
        this.toasts.mostrar('exito', 'Política de descuentos guardada.');
      });
  }
}
