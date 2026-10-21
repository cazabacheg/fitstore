import { HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { CarritoContador, SIN_TOAST, comoApiError, orderApi } from 'shared';
import { CarritoService as CarritoApi } from 'shared/api/order-service';
import { Observable, tap } from 'rxjs';

/** Estado del carrito de la tienda con señales; cada cambio actualiza el contador de la cabecera (O7, R8). */
@Injectable({ providedIn: 'root' })
export class CarritoService {
  private readonly api = inject(CarritoApi);
  private readonly contador = inject(CarritoContador);

  readonly carrito = signal<orderApi.Carrito | null>(null);
  readonly estado = signal<'cargando' | 'listo' | 'error'>('cargando');
  /** Mensaje de la línea que el servidor rechazó (stock o límite), por variante. */
  readonly errores = signal<Record<number, string>>({});

  readonly vacio = computed(() => (this.carrito()?.lineas.length ?? 0) === 0);

  private readonly sinToast = () => ({ context: new HttpContext().set(SIN_TOAST, true) });

  cargar(): void {
    this.estado.set('cargando');
    this.api.obtenerCarrito(undefined, 'body', false, this.sinToast()).subscribe({
      next: (r) => this.aplicar(r.data),
      error: () => this.estado.set('error'),
    });
  }

  /** CA-4-01: suma a la línea si ya existía; el contador de la cabecera muestra el total de unidades. */
  agregar(varianteId: number, cantidad: number): Observable<orderApi.RespuestaCarrito> {
    return this.api
      .agregarAlCarrito({ varianteId, cantidad }, undefined, 'body', false, this.sinToast())
      .pipe(tap((r) => this.aplicar(r.data)));
  }

  /** CA-4-03: recalcula con una nueva cotización. Si el servidor lo rechaza la línea conserva su cantidad (CA-4-04). */
  cambiarCantidad(varianteId: number, cantidad: number): void {
    this.api
      .cambiarCantidadCarrito(varianteId, { cantidad }, undefined, 'body', false, this.sinToast())
      .subscribe({
        next: (r) => this.aplicar(r.data),
        error: (e: unknown) => {
          const motivo = comoApiError(e)?.mensaje ?? 'No pudimos actualizar la cantidad';
          this.errores.update((m) => ({ ...m, [varianteId]: motivo }));
          this.carrito.update((c) => (c ? { ...c } : c));
        },
      });
  }

  quitar(varianteId: number): void {
    this.api.quitarDelCarrito(varianteId, undefined, 'body', false, this.sinToast()).subscribe({
      next: (r) => this.aplicar(r.data),
      error: () => this.errores.update((m) => ({ ...m, [varianteId]: 'No pudimos quitar el producto' })),
    });
  }

  vaciar(): void {
    this.api.vaciarCarrito(undefined, 'body', false, this.sinToast()).subscribe({
      next: (r) => this.aplicar(r.data),
      error: () => this.estado.set('error'),
    });
  }

  private aplicar(carrito: orderApi.Carrito | undefined): void {
    if (!carrito) return;
    this.carrito.set(carrito);
    this.errores.set({});
    this.contador.establecer(carrito.cantidadTotal);
    this.estado.set('listo');
  }
}
