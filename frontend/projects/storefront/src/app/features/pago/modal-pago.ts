import { A11yModule } from '@angular/cdk/a11y';
import { Component, ElementRef, OnDestroy, afterRenderEffect, input, output, viewChild } from '@angular/core';
import { ESTILOS_PANTALLA } from '../estilos-pantalla';
import { ResultadoPago, type TipoResultado } from './resultado-pago';

export type EstadoModalPago = 'procesando' | TipoResultado;

/**
 * Modal de la página de pago (P-5-01 y P-5-02): muestra el spinner mientras se procesa el cobro y, en el mismo
 * modal, el resultado (aprobado, rechazado, confirmando o sin confirmar). La persona no sale de la pantalla de pago.
 */
@Component({
  selector: 'fs-modal-pago',
  imports: [A11yModule, ResultadoPago],
  template: `
    <div class="fs-dialogo-fondo">
      <div
        #dialogo
        class="fs-dialogo modal-pago"
        role="dialog"
        aria-modal="true"
        aria-label="Estado de tu pago"
        tabindex="-1"
        cdkTrapFocus
        (keydown.escape)="alEscape()"
      >
        <div aria-live="polite" class="modal-pago__estado">
          @if (estado() === 'procesando') {
            <div class="modal-pago__procesando" role="status">
              <span class="fs-spinner" aria-hidden="true"></span>
              <h2 class="fs-titulo-seccion">Procesando tu pago</h2>
              <p class="fs-suave" style="margin: 0">No cierres ni recargues esta página.</p>
            </div>
          } @else {
            <fs-resultado-pago
              [tipo]="tipoResultado()"
              [codigo]="codigo()"
              [motivo]="motivo()"
              [boletaDisponible]="boletaDisponible()"
              [descargando]="descargando()"
              (reintentar)="reintentar.emit()"
              (descargarBoleta)="descargarBoleta.emit()"
            />
          }
        </div>
      </div>
    </div>
  `,
  styles: [
    ESTILOS_PANTALLA,
    `
      .modal-pago {
        width: min(520px, 100%);
        outline: none;
      }
      .modal-pago__estado {
        display: grid;
      }
      .modal-pago__procesando {
        display: grid;
        justify-items: center;
        gap: 16px;
        padding-block: 16px;
        text-align: center;
      }
      .fs-spinner {
        width: 48px;
        height: 48px;
        border-radius: 50%;
        border: 4px solid var(--fs-color-linea);
        border-top-color: var(--fs-color-texto);
        animation: fs-giro 0.9s linear infinite;
      }
      @keyframes fs-giro {
        to {
          transform: rotate(360deg);
        }
      }
      @media (prefers-reduced-motion: reduce) {
        .fs-spinner {
          animation-duration: 3s;
        }
      }
    `,
  ],
})
export class ModalPago implements OnDestroy {
  readonly estado = input.required<EstadoModalPago>();
  readonly codigo = input.required<string>();
  readonly motivo = input('El pago no se pudo completar');
  readonly boletaDisponible = input(false);
  readonly descargando = input(false);

  readonly reintentar = output<void>();
  readonly descargarBoleta = output<void>();

  private readonly dialogo = viewChild.required<ElementRef<HTMLElement>>('dialogo');
  private readonly previo = document.activeElement as HTMLElement | null;

  constructor() {
    // cada cambio de estado deja el foco dentro: en el primer botón o enlace, o en el propio modal si no hay
    afterRenderEffect(() => {
      this.estado();
      const raiz = this.dialogo().nativeElement;
      if (raiz.contains(document.activeElement) && document.activeElement !== raiz) return;
      const accion = raiz.querySelector<HTMLElement>('button:not([disabled]), a[href]');
      (accion ?? raiz).focus();
    });
  }

  ngOnDestroy(): void {
    this.previo?.focus?.();
  }

  protected tipoResultado(): TipoResultado {
    const e = this.estado();
    return e === 'procesando' ? 'confirmando' : e;
  }

  /** Solo el rechazo se puede cerrar: mientras se procesa o se confirma, la persona espera. */
  protected alEscape(): void {
    if (this.estado() === 'rechazado') this.reintentar.emit();
  }
}
