import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FsBoton } from '../../ui/boton/boton';
import { ESTILOS_PANTALLA } from '../estilos-pantalla';

export type TipoResultado = 'aprobado' | 'rechazado' | 'confirmando' | 'sin-confirmar';

/** Estado final de la página de pago (P-5-02): aprobado, rechazado, confirmando o sin confirmar todavía. */
@Component({
  selector: 'fs-resultado-pago',
  imports: [RouterLink, FsBoton],
  template: `
    <section
      style="display: grid; justify-items: center; gap: 16px; text-align: center; max-width: 520px; margin-inline: auto"
      aria-live="polite"
    >
      @switch (tipo()) {
        @case ('aprobado') {
          <div class="fs-resultado__icono fs-resultado__icono--exito">
            <span class="fs-icono" style="font-size: 40px" aria-hidden="true">check</span>
          </div>
          <h1 class="fs-titulo-pagina">¡Pago aprobado!</h1>
          <p class="fs-suave" style="margin: 0">
            Tu pedido <span class="fs-codigo">{{ codigo() }}</span> está confirmado. Te enviamos la
            confirmación a tu correo.
          </p>
          <div style="display: flex; gap: 12px; flex-wrap: wrap; justify-content: center">
            <a fsBoton tamano="lg" [routerLink]="['/cuenta/pedidos', codigo()]">Ver mi pedido</a>
            @if (boletaDisponible()) {
              <button type="button" fsBoton variante="contorno" tamano="lg" [disabled]="descargando()" (click)="descargarBoleta.emit()">
                <span class="fs-icono" aria-hidden="true">receipt_long</span>Descargar boleta
              </button>
            }
          </div>
        }
        @case ('rechazado') {
          <div class="fs-resultado__icono fs-resultado__icono--error">
            <span class="fs-icono" style="font-size: 40px" aria-hidden="true">close</span>
          </div>
          <h1 class="fs-titulo-seccion">No pudimos cobrar tu pago</h1>
          <p class="fs-suave" style="margin: 0">
            {{ motivo() }}. Tus prendas siguen apartadas mientras no venza el plazo.
          </p>
          <button type="button" fsBoton (click)="reintentar.emit()">Intentar de nuevo</button>
        }
        @case ('confirmando') {
          <div class="fs-resultado__icono fs-resultado__icono--info">
            <span class="fs-icono" style="font-size: 40px" aria-hidden="true">hourglass_top</span>
          </div>
          <h1 class="fs-titulo-seccion">Estamos confirmando tu pago</h1>
          <p class="fs-suave" style="margin: 0">
            Puede tardar hasta 2 minutos; no cierres ni vuelvas a pagar.
          </p>
          <div class="fs-esqueleto" style="width: 160px; height: 6px"></div>
        }
        @case ('sin-confirmar') {
          <div class="fs-resultado__icono fs-resultado__icono--info">
            <span class="fs-icono" style="font-size: 40px" aria-hidden="true">mark_email_unread</span>
          </div>
          <h1 class="fs-titulo-seccion">Todavía no tenemos la confirmación</h1>
          <p class="fs-suave" style="margin: 0">
            Te avisaremos por correo cuando se confirme. No vuelvas a pagar este pedido.
          </p>
          <a fsBoton variante="contorno" [routerLink]="['/cuenta/pedidos', codigo()]">Ver mi pedido</a>
        }
      }
    </section>
  `,
  styles: [
    ESTILOS_PANTALLA,
    `
    .fs-resultado__icono {
      width: 72px;
      height: 72px;
      border-radius: 50%;
      display: grid;
      place-items: center;
    }
    .fs-resultado__icono--exito {
      background: var(--fs-color-exito-suave);
      color: var(--fs-color-exito);
    }
    .fs-resultado__icono--error {
      background: var(--fs-color-error-suave);
      color: var(--fs-color-error);
    }
    .fs-resultado__icono--info {
      background: var(--fs-color-info-suave);
      color: var(--fs-color-info);
    }
  `,
  ],
})
export class ResultadoPago {
  readonly tipo = input.required<TipoResultado>();
  readonly codigo = input.required<string>();
  readonly motivo = input('El pago no se pudo completar');
  readonly boletaDisponible = input(false);
  readonly descargando = input(false);

  readonly reintentar = output<void>();
  readonly descargarBoleta = output<void>();
}
