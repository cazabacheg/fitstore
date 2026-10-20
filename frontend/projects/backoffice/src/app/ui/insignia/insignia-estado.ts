import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { insigniaDe, type TipoEstado } from './estados';

/** Insignia de estado de pedido, pago, entrega o reembolso: siempre con la palabra, el color solo refuerza. */
@Component({
  selector: 'fs-insignia-estado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="fs-insignia" [class]="'fs-insignia fs-insignia--' + datos().variante">{{
    datos().etiqueta
  }}</span>`,
})
export class InsigniaEstado {
  readonly tipo = input.required<TipoEstado>();
  /** Código del estado tal como lo devuelve la API (`PENDIENTE_PAGO`). */
  readonly estado = input.required<string>();
  protected readonly datos = computed(() => insigniaDe(this.tipo(), this.estado()));
}
