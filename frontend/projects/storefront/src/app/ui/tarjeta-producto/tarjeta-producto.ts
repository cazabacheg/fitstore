import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import type { catalogApi } from 'shared';
import { FsBadge } from '../badge/badge';
import { FsPrecio } from '../precio/precio';

/**
 * Tarjeta de producto (sistema de diseño: TarjetaProducto). Sin `producto` es el esqueleto vacío.
 * Toda la tarjeta es un enlace a `/producto/:slug`; no lleva botón de agregar al carrito.
 */
@Component({
  selector: 'fs-tarjeta-producto',
  imports: [RouterLink, FsBadge, FsPrecio],
  template: `
    @if (producto(); as p) {
      <a
        class="fs-tarjeta-producto"
        [class.fs-tarjeta-producto--agotado]="p.agotado"
        [routerLink]="['/producto', p.slug]"
      >
        <div class="fs-tarjeta-producto__imagen">
          @if (p.imagenUrl) {
            <img
              [src]="p.imagenUrl"
              [alt]="p.nombre"
              style="width: 100%; height: 100%; object-fit: cover; border-radius: inherit"
            />
          } @else {
            <span class="fs-icono" aria-hidden="true">image</span>
          }
          @if (p.agotado) {
            <span class="fs-tarjeta-producto__insignia"><fs-badge>Agotado</fs-badge></span>
          } @else if (p.etiquetaDescuento) {
            <span class="fs-tarjeta-producto__insignia">
              <fs-badge variante="acento">{{ p.etiquetaDescuento }}</fs-badge>
            </span>
          }
        </div>
        <span class="fs-tarjeta-producto__nombre">{{ p.nombre }}</span>
        <fs-precio [monto]="p.precioDesde" />
        @if (p.colores.length) {
          <span class="fs-muestras">
            @for (c of p.colores; track c.id) {
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
    } @else {
      <div class="fs-tarjeta-producto" aria-hidden="true">
        <div class="fs-tarjeta-producto__imagen fs-esqueleto"></div>
        <div class="fs-esqueleto" style="height: 16px; width: 75%"></div>
        <div class="fs-esqueleto" style="height: 16px; width: 40%"></div>
      </div>
    }
  `,
})
export class FsTarjetaProducto {
  readonly producto = input<catalogApi.ProductoTarjeta>();
}
