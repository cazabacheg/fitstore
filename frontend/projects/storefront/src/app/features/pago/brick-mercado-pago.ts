import { Component, DestroyRef, afterNextRender, inject, input, output, signal } from '@angular/core';
import { idUnico } from '../../ui/id-unico';
import { SDK_MERCADO_PAGO, type BrickCreado } from './sdk-mercado-pago';

/** Lo que el Payment Brick devuelve al enviarse: el token de un solo uso y los datos del medio (nunca la tarjeta). */
export interface DatosBrick {
  token: string;
  payment_method_id?: string;
  installments?: number;
  issuer_id?: string;
  payer?: { email?: string };
}

/** Un envío del Brick: la página cobra y avisa con `terminar` si salió bien, para que el Brick deje de mostrar "procesando". */
export interface EnvioBrick {
  datos: DatosBrick;
  terminar: (ok: boolean) => void;
}

/** Payment Brick embebido de Mercado Pago (P-5-01): el navegador obtiene el token y el backend cobra. */
@Component({
  selector: 'fs-brick-mercado-pago',
  template: `
    <div class="fs-suave" style="display: flex; gap: 6px; align-items: center">
      <span class="fs-icono" style="font-size: 16px" aria-hidden="true">lock</span>
      Mercado Pago procesa tu tarjeta; la tienda nunca ve sus datos.
    </div>
    @if (error()) {
      <p class="fs-campo__error" role="alert">{{ error() }}</p>
    }
    <div [id]="idContenedor" aria-label="Formulario de Mercado Pago"></div>
  `,
})
export class BrickMercadoPago {
  readonly clavePublica = input.required<string>();
  readonly monto = input.required<number>();
  readonly enviado = output<EnvioBrick>();

  protected readonly idContenedor = idUnico('brick-mp');
  protected readonly error = signal('');

  private readonly cargarSdk = inject(SDK_MERCADO_PAGO);
  private brick: BrickCreado | null = null;
  private destruido = false;

  constructor() {
    afterNextRender(() => void this.montar());
    inject(DestroyRef).onDestroy(() => {
      this.destruido = true;
      this.brick?.unmount();
    });
  }

  private async montar(): Promise<void> {
    try {
      const sdk = await this.cargarSdk();
      const brick = await sdk.crear(this.clavePublica()).bricks().create('cardPayment', this.idContenedor, {
        initialization: { amount: this.monto() },
        callbacks: {
          onReady: () => undefined,
          onError: () => this.error.set('No pudimos mostrar el formulario de Mercado Pago. Inténtalo de nuevo.'),
          onSubmit: (datos: DatosBrick) =>
            new Promise<void>((resolver, rechazar) =>
              this.enviado.emit({ datos, terminar: (ok) => (ok ? resolver() : rechazar()) }),
            ),
        },
      });
      if (this.destruido) brick.unmount();
      else this.brick = brick;
    } catch {
      this.error.set('No pudimos cargar Mercado Pago. Revisa tu conexión e inténtalo de nuevo.');
    }
  }
}
