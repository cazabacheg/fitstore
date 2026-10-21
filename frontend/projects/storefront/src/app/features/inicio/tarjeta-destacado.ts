import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import type { catalogApi } from 'shared';
import { FsBadge } from '../../ui/badge/badge';
import { FsPrecio } from '../../ui/precio/precio';

/**
 * Tarjeta de un destacado de la landing: misma estructura visual que la del listado (SPEC-2, `fs-tarjeta-producto`)
 * pero como componente propio de `inicio/`, para no depender de `features/catalogo`.
 */
@Component({
  selector: 'fs-tarjeta-destacado',
  imports: [RouterLink, FsBadge, FsPrecio],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a
      class="fs-tarjeta-producto"
      data-testid="destacado"
      [class.fs-tarjeta-producto--agotado]="producto().agotado"
      [routerLink]="['/producto', producto().slug]"
    >
      <div class="fs-tarjeta-producto__imagen">
        @if (producto().imagenUrl; as url) {
          <img
            [src]="url"
            [alt]="producto().nombre"
            style="width: 100%; height: 100%; object-fit: cover; border-radius: inherit"
          />
        } @else {
          <span class="fs-icono" aria-hidden="true">image</span>
        }
        @if (producto().agotado) {
          <span class="fs-tarjeta-producto__insignia"><fs-badge>Agotado</fs-badge></span>
        } @else if (producto().etiquetaDescuento) {
          <span class="fs-tarjeta-producto__insignia">
            <fs-badge variante="acento">{{ producto().etiquetaDescuento }}</fs-badge>
          </span>
        }
      </div>
      <span class="fs-tarjeta-producto__nombre">{{ producto().nombre }}</span>
      <fs-precio [monto]="producto().precioDesde" />
      @if (producto().colores.length) {
        <span class="fs-muestras">
          @for (c of producto().colores; track c.id) {
            <span
              class="fs-muestra"
              [title]="c.nombre"
              [style.background]="c.hex"
              role="img"
              [attr.aria-label]="c.nombre"
            ></span>
          }
        </span>
      }
    </a>
  `,
})
export class TarjetaDestacado {
  readonly producto = input.required<catalogApi.ProductoTarjeta>();
}
