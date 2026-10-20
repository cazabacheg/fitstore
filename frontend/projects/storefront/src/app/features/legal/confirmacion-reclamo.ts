import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FsBoton } from '../../ui/boton/boton';
import { fechaLarga } from './formato-fecha';

/** Confirmación del reclamo registrado: número, fecha límite de respuesta y botón para imprimirla. */
@Component({
  selector: 'fs-confirmacion-reclamo',
  imports: [FsBoton, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="confirmacion" data-testid="confirmacion" aria-live="polite">
      <span class="fs-icono confirmacion__icono" aria-hidden="true">check_circle</span>
      <h2 class="confirmacion__titulo">Registramos tu reclamo</h2>
      <p class="confirmacion__numero" data-testid="numero-reclamo">{{ numero() }}</p>
      <p>
        Responderemos como máximo el <strong>{{ fecha(fechaLimite()) }}</strong> (15 días hábiles).
      </p>
      <p class="fs-suave">
        Te enviaremos una copia de tu hoja a <strong>{{ email() }}</strong
        >. Guarda este número para consultar tu caso.
      </p>
      <div class="confirmacion__acciones no-imprimir">
        <button
          type="button"
          fsBoton
          variante="contorno"
          data-testid="imprimir"
          (click)="imprimir()"
        >
          Imprimir constancia
        </button>
        <a fsBoton variante="texto" routerLink="/">Volver al inicio</a>
      </div>
    </section>
  `,
  styles: `
    .confirmacion {
      display: grid;
      justify-items: center;
      gap: 8px;
      padding: 48px 24px;
      text-align: center;
      border: 1px solid var(--fs-color-linea);
      border-radius: var(--fs-radio-lg, 12px);
    }
    .confirmacion__icono {
      font-size: 56px;
      color: var(--fs-color-primario-texto);
    }
    .confirmacion__titulo {
      margin: 0;
    }
    .confirmacion__numero {
      margin: 0;
      font-size: 28px;
      font-weight: 700;
      letter-spacing: 0.04em;
    }
    .confirmacion__acciones {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 12px;
      margin-top: 16px;
    }
    @media print {
      .no-imprimir {
        display: none;
      }
      .confirmacion {
        border: 0;
      }
    }
  `,
})
export class ConfirmacionReclamo {
  readonly numero = input.required<string>();
  readonly fechaLimite = input.required<string>();
  readonly email = input.required<string>();

  protected fecha(valor: string): string {
    return fechaLarga(valor);
  }

  protected imprimir(): void {
    window.print();
  }
}
