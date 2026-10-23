import { Component, DestroyRef, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { PrecioPipe, orderApi } from 'shared';
import { FsBoton } from '../../ui/boton/boton';
import { DialogoConfirmacion } from '../../ui/dialogo/dialogo-confirmacion';
import { EstadoCargando, EstadoError, EstadoVacio } from '../../ui/estados/estados';
import { ESTILOS_PANTALLA } from '../estilos-pantalla';
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
  styles: [
    ESTILOS_PANTALLA,
    `
      .carrito {
        padding-block: 40px 80px;
        display: grid;
        gap: 28px;
      }
      .carrito__lineas {
        list-style: none;
        margin: 0;
        padding: 0;
        display: grid;
        align-content: start;
      }
      li.fs-linea {
        display: grid;
        grid-template-columns: 96px minmax(0, 1fr) auto;
        gap: 20px;
        padding-block: 24px;
        border-bottom: 1px solid var(--fs-color-linea);
      }
      li.fs-linea .fs-miniatura {
        width: 96px;
      }
      .fs-linea__info {
        display: grid;
        gap: 6px;
        align-content: start;
        min-width: 0;
      }
      .fs-linea__nombre {
        font-weight: 600;
      }
      .fs-linea__descuento {
        font-size: 13px;
        color: var(--fs-color-exito);
      }
      .fs-linea__aviso {
        font-size: 13px;
        color: var(--fs-color-aviso);
      }
      .fs-linea__error {
        font-size: 13px;
        color: var(--fs-color-error);
      }
      .fs-linea__cantidad {
        display: inline-flex;
        align-items: center;
        gap: var(--espacio-2);
        justify-self: start;
        margin-top: 6px;
      }
      .fs-linea__unidades {
        min-width: 24px;
        text-align: center;
        font-weight: 600;
        font-variant-numeric: tabular-nums;
      }
      .fs-linea__derecha {
        display: grid;
        justify-items: end;
        align-content: space-between;
        gap: 8px;
      }
      .fs-linea__total {
        font-variant-numeric: tabular-nums;
      }
      .fs-linea--no-disponible .fs-miniatura {
        filter: grayscale(1);
      }
      .fs-banner-aviso {
        margin: 0;
        padding: 14px 16px;
        border-radius: var(--fs-radio-md);
        background: var(--fs-color-aviso-suave);
        color: var(--fs-color-aviso);
        font-size: 14px;
      }
      .fs-aviso-tramo {
        margin: 0 0 8px;
        padding: 14px 16px;
        border-radius: var(--fs-radio-md);
        background: var(--fs-color-primario-suave);
      }
      hr.fs-separador {
        border: 0;
        border-top: 1px solid var(--fs-color-linea);
        margin: 0;
      }
      .fs-resumen {
        display: grid;
        gap: 16px;
      }
      .fs-resumen__fila {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        font-variant-numeric: tabular-nums;
      }
      .fs-resumen__fila--descuento {
        color: var(--fs-color-exito);
      }
      .fs-resumen__total {
        font-size: 18px;
      }
      .fs-resumen__nota {
        margin: 0;
      }
      @media (max-width: 720px) {
        .carrito {
          padding-block: 24px 48px;
        }
        li.fs-linea {
          grid-template-columns: 80px minmax(0, 1fr) auto;
          gap: 12px;
        }
        li.fs-linea .fs-miniatura {
          width: 80px;
        }
      }
    `,
  ],
  template: `
    <section class="fs-pagina carrito">
      <div class="fs-entre">
        <h1 class="fs-titulo-pagina">Tu carrito</h1>
        @if (carritos.carrito(); as actual) {
          @if (!carritos.vacio()) {
            <span class="fs-suave">{{ prendas(actual) }}</span>
          }
        }
      </div>
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
            <div class="fs-carrito fs-dos-columnas">
              <section style="display: grid; align-content: start">
                @for (m of carritos.avisos(); track m) {
                  <p class="fs-banner-aviso" role="status">{{ m }}</p>
                }
                @for (a of c.avisosTramo; track a.productoId) {
                  @if (a.siguienteTramo; as s) {
                    @if (carritos.tramoAlcanzable(a)) {
                    <p class="fs-aviso-tramo">
                      {{ a.producto }}: agrega {{ s.faltan }} más y ahorra {{ s.porcentaje }} %
                    </p>
                    }
                  }
                }
                <ul class="carrito__lineas">
                  @for (l of c.lineas; track l.varianteId) {
                    <li
                      class="fs-linea"
                      [class.fs-linea--no-disponible]="l.estado === 'NO_DISPONIBLE'"
                      [style.opacity]="l.estado === 'NO_DISPONIBLE' ? 0.5 : 1"
                    >
                      <div class="fs-miniatura">
                        @if (l.imagenUrl) {
                          <img [src]="l.imagenUrl" [alt]="l.producto" width="96" height="120" />
                        } @else {
                          <span class="fs-icono" aria-hidden="true">checkroom</span>
                        }
                      </div>
                      <div class="fs-linea__info">
                        <strong class="fs-linea__nombre">{{ l.producto }}</strong>
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
                        @if (l.estado !== 'NO_DISPONIBLE') {
                          <div class="fs-linea__cantidad">
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
                              [disabled]="cantidadMostrada(l) >= tope(l)"
                              (click)="cambiar(l, 1)"
                            >
                              +
                            </button>
                          </div>
                        }
                      </div>
                      @if (l.estado !== 'NO_DISPONIBLE') {
                        <div class="fs-linea__derecha">
                          <button
                            type="button"
                            fsBoton
                            variante="texto"
                            tamano="sm"
                            title="Quitar"
                            [attr.aria-label]="'Quitar ' + l.producto"
                            (click)="carritos.quitar(l.varianteId)"
                          >
                            <span class="fs-icono" aria-hidden="true">delete</span>
                          </button>
                          <strong class="fs-linea__total">{{ l.total | precio }}</strong>
                        </div>
                      }
                    </li>
                  }
                </ul>
                <div style="padding-top: 16px">
                  <a fsBoton variante="texto" routerLink="/productos">Seguir comprando</a>
                </div>
              </section>

              <aside class="fs-resumen fs-tarjeta fs-lateral">
                <h2 class="fs-titulo-seccion">Resumen</h2>
                <div class="fs-resumen__fila">
                  <span>Subtotal</span><span>{{ c.subtotal | precio }}</span>
                </div>
                @if (c.descuentoTotal > 0) {
                  <div class="fs-resumen__fila fs-resumen__fila--descuento">
                    <span>Descuento</span><span>-{{ c.descuentoTotal | precio }}</span>
                  </div>
                }
                <hr class="fs-separador" />
                <div class="fs-resumen__fila fs-resumen__total">
                  <strong>Total</strong><strong>{{ c.total | precio }}</strong>
                </div>
                <p class="fs-suave fs-resumen__nota">El envío se calcula en el checkout.</p>
                @if (c.puedeIrAlCheckout) {
                  <a fsBoton variante="primario" tamano="lg" [bloque]="true" routerLink="/checkout">Ir al checkout</a>
                } @else {
                  <button
                    type="button"
                    fsBoton
                    variante="primario"
                    tamano="lg"
                    [bloque]="true"
                    disabled
                    aria-disabled="true"
                  >
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
    </section>
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
  private readonly temporizadores = new Map<number, ReturnType<typeof setTimeout>>();

  constructor() {
    this.carritos.cargar();
    this.destruir.onDestroy(() => this.temporizadores.forEach((t) => clearTimeout(t)));
  }

  protected cantidadMostrada(linea: orderApi.LineaCarrito): number {
    return linea.cantidad;
  }

  /** Hasta cuánto se puede subir: el límite por línea o el stock que el servidor informó, lo que sea menor (CA-4-63). */
  protected tope(linea: orderApi.LineaCarrito): number {
    return Math.min(this.maximoUnidades, this.carritos.stockMaximo(linea.varianteId) ?? this.maximoUnidades);
  }

  /** "7 prendas" junto al título, como en el diseño. */
  protected prendas(carrito: orderApi.Carrito): string {
    const total = carrito.lineas.reduce((suma, l) => suma + l.cantidad, 0);
    return total === 1 ? '1 prenda' : `${total} prendas`;
  }

  /** Muestra el cambio al instante y espera 400 ms desde el último toque antes de llamar a la API (CA-4-03, CA-4-66). */
  protected cambiar(linea: orderApi.LineaCarrito, delta: number): void {
    const actual = this.carritos.carrito()?.lineas.find((l) => l.varianteId === linea.varianteId) ?? linea;
    const nueva = Math.min(this.maximoUnidades, Math.max(1, actual.cantidad + delta));
    this.carritos.previsualizar(linea.varianteId, nueva); // CA-4-66: se ve al instante
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
