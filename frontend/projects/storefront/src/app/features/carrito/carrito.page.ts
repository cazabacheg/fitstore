import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { PrecioPipe, orderApi } from 'shared';
import { FsBoton } from '../../ui/boton/boton';
import { DialogoConfirmacion } from '../../ui/dialogo/dialogo-confirmacion';
import { EstadoCargando, EstadoError, EstadoVacio } from '../../ui/estados/estados';
import { CarritoService } from './carrito.service';

/** Espera tras tocar "+" o "−" antes de llamar a la API (ficha `LineaCarrito`). */
export const ESPERA_CANTIDAD_MS = 400;

/** Carrito (P-4-01): líneas con su descuento, aviso de tramo, totales sin envío y estados de error (CA-4-01 a CA-4-08). */
@Component({
  selector: 'fs-carrito-page',
  imports: [
    RouterLink,
    FsBoton,
    PrecioPipe,
    DialogoConfirmacion,
    EstadoCargando,
    EstadoError,
    EstadoVacio,
  ],
  template: `
    <h1>Tu carrito</h1>
    @switch (carritos.estado()) {
      @case ('cargando') {
        <fs-estado-cargando />
      }
      @case ('error') {
        <fs-estado-error titulo="No pudimos cargar tu carrito" (reintentar)="carritos.cargar()" />
      }
      @default {
        @if (carritos.vacio()) {
          <fs-estado-vacio
            titulo="Tu carrito está vacío"
            texto="Cuando agregues prendas las verás aquí."
            accion="Ir a la tienda"
            (accionar)="irALaTienda()"
          />
        } @else {
          @let c = carritos.carrito()!;
          @if (c.preciosPorConfirmar) {
            <p class="fs-banner-aviso" role="status">
              Precios por confirmar: no pudimos actualizar los precios, te mostramos los últimos que vimos.
            </p>
          }
          <div class="fs-carrito" style="display: grid; gap: var(--espacio-5)">
            <ul style="list-style: none; padding: 0; margin: 0; display: grid; gap: var(--espacio-4)">
              @for (l of c.lineas; track l.varianteId) {
                <li
                  class="fs-linea"
                  [class.fs-linea--no-disponible]="l.estado === 'NO_DISPONIBLE'"
                  [style.opacity]="l.estado === 'NO_DISPONIBLE' ? 0.5 : 1"
                  style="display: flex; gap: var(--espacio-4); align-items: center; flex-wrap: wrap"
                >
                  @if (l.imagenUrl) {
                    <img [src]="l.imagenUrl" [alt]="l.producto" width="72" height="72" />
                  }
                  <div style="flex: 1; min-width: 12rem">
                    <strong>{{ l.producto }}</strong>
                    <div class="fs-suave">{{ l.talla }} · {{ l.color }}</div>
                    @if (l.estado === 'NO_DISPONIBLE') {
                      <div class="fs-linea__aviso">
                        Ya no está disponible ·
                        <button type="button" fsBoton variante="texto" (click)="carritos.quitar(l.varianteId)">
                          Quitar
                        </button>
                      </div>
                    } @else {
                      <div>{{ l.precioUnitario | precio }} c/u</div>
                      @if ((l.descuentoMonto ?? 0) > 0) {
                        <div class="fs-linea__descuento">
                          Ahorras {{ l.descuentoMonto | precio }} ({{ l.descuentoPorcentaje }} %)
                        </div>
                      }
                    }
                    @if (carritos.errores()[l.varianteId]; as motivo) {
                      <div class="fs-linea__error" role="alert">{{ motivo }}</div>
                    }
                  </div>
                  @if (l.estado !== 'NO_DISPONIBLE') {
                    <div class="fs-linea__cantidad" style="display: flex; align-items: center; gap: var(--espacio-2)">
                      <button
                        type="button"
                        fsBoton
                        variante="contorno"
                        tamano="sm"
                        aria-label="Quitar una unidad"
                        [disabled]="cantidadMostrada(l) <= 1"
                        (click)="cambiar(l, -1)"
                      >
                        −
                      </button>
                      <span class="fs-linea__unidades" aria-live="polite">{{ cantidadMostrada(l) }}</span>
                      <button
                        type="button"
                        fsBoton
                        variante="contorno"
                        tamano="sm"
                        aria-label="Agregar una unidad"
                        [disabled]="cantidadMostrada(l) >= maximoUnidades"
                        (click)="cambiar(l, 1)"
                      >
                        +
                      </button>
                    </div>
                    <strong class="fs-linea__total">{{ l.total | precio }}</strong>
                    <button
                      type="button"
                      fsBoton
                      variante="texto"
                      [attr.aria-label]="'Quitar ' + l.producto"
                      (click)="carritos.quitar(l.varianteId)"
                    >
                      Quitar
                    </button>
                  }
                </li>
              }
            </ul>

            @for (a of c.avisosTramo; track a.productoId) {
              @if (a.siguienteTramo; as s) {
                <p class="fs-aviso-tramo">
                  {{ a.producto }}: agrega {{ s.faltan }} más y ahorra {{ s.porcentaje }} %
                </p>
              }
            }

            <aside class="fs-resumen" style="display: grid; gap: var(--espacio-2)">
              <div class="fs-resumen__fila">
                <span>Subtotal</span><span>{{ c.subtotal | precio }}</span>
              </div>
              <div class="fs-resumen__fila">
                <span>Descuento</span><span>-{{ c.descuentoTotal | precio }}</span>
              </div>
              <div class="fs-resumen__fila">
                <strong>Total</strong><strong>{{ c.total | precio }}</strong>
              </div>
              <p class="fs-suave">El envío se calcula en el checkout.</p>
              @if (c.puedeIrAlCheckout) {
                <a fsBoton variante="primario" [bloque]="true" routerLink="/checkout">Ir al checkout</a>
              } @else {
                <button type="button" fsBoton variante="primario" [bloque]="true" disabled aria-disabled="true">
                  Ir al checkout
                </button>
              }
              <button type="button" fsBoton variante="texto" (click)="confirmandoVaciar.set(true)">
                Vaciar carrito
              </button>
            </aside>
          </div>
        }
      }
    }
    <fs-dialogo-confirmacion
      titulo="¿Vaciar tu carrito?"
      texto="Se quitarán todas las prendas."
      textoConfirmar="Vaciar carrito"
      [peligro]="true"
      [abierto]="confirmandoVaciar()"
      (cancelado)="confirmandoVaciar.set(false)"
      (confirmado)="vaciar()"
    />
  `,
})
export class CarritoPage {
  protected readonly carritos = inject(CarritoService);
  private readonly router = inject(Router);
  private readonly destruir = inject(DestroyRef);

  protected readonly maximoUnidades = 100;
  protected readonly confirmandoVaciar = signal(false);
  /** Cantidad que la persona ve mientras espera los 400 ms; vuelve a la del servidor al llegar su respuesta. */
  private readonly pendientes = signal<Record<number, number>>({});
  private readonly temporizadores = new Map<number, ReturnType<typeof setTimeout>>();

  constructor() {
    this.carritos.cargar();
    // Cada respuesta del servidor (éxito o rechazo) deja la línea con la cantidad que el servidor tiene
    effect(() => {
      this.carritos.carrito();
      this.pendientes.set({});
    });
    this.destruir.onDestroy(() => this.temporizadores.forEach((t) => clearTimeout(t)));
  }

  protected cantidadMostrada(linea: orderApi.LineaCarrito): number {
    return this.pendientes()[linea.varianteId] ?? linea.cantidad;
  }

  /** Espera 400 ms desde el último toque antes de llamar a la API (CA-4-03). */
  protected cambiar(linea: orderApi.LineaCarrito, delta: number): void {
    const nueva = Math.min(this.maximoUnidades, Math.max(1, this.cantidadMostrada(linea) + delta));
    this.pendientes.update((p) => ({ ...p, [linea.varianteId]: nueva }));
    clearTimeout(this.temporizadores.get(linea.varianteId));
    this.temporizadores.set(
      linea.varianteId,
      setTimeout(() => {
        this.temporizadores.delete(linea.varianteId);
        this.carritos.cambiarCantidad(linea.varianteId, nueva);
      }, ESPERA_CANTIDAD_MS),
    );
  }

  protected vaciar(): void {
    this.confirmandoVaciar.set(false);
    this.carritos.vaciar();
  }

  protected irALaTienda(): void {
    void this.router.navigateByUrl('/productos');
  }
}
