import { Component, input } from '@angular/core';
import { PrecioPipe } from 'shared';
import { FsBadge } from '../badge/badge';

/** Precio en soles con el original tachado y el descuento (sistema de diseño: Precio). */
@Component({
  selector: 'fs-precio',
  imports: [PrecioPipe, FsBadge],
  template: `
    <span class="fs-precio">
      <span class="fs-precio__actual">{{ monto() | precio }}</span>
      @if (antes() !== null && antes() !== undefined) {
        <span class="fs-precio__antes">{{ antes() | precio }}</span>
      }
      @if (descuento()) {
        <fs-badge variante="acento">{{ descuento() }}</fs-badge>
      }
    </span>
  `,
})
export class FsPrecio {
  readonly monto = input.required<number>();
  readonly antes = input<number | null>();
  readonly descuento = input('');
}
