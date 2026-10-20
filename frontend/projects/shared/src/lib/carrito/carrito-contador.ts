import { Injectable, signal } from '@angular/core';

/**
 * Cantidad de prendas del carrito que muestra la cabecera de la tienda (R8). La spec del carrito (4) la actualiza;
 * así 0b no depende de la lógica del carrito.
 */
@Injectable({ providedIn: 'root' })
export class CarritoContador {
  private readonly total = signal(0);

  readonly cantidad = this.total.asReadonly();

  establecer(cantidad: number): void {
    this.total.set(Number.isFinite(cantidad) ? Math.max(0, Math.trunc(cantidad)) : 0);
  }
}
