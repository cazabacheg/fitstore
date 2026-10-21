import { Component, computed, model } from '@angular/core';

/** Dígitos de la tarjeta de prueba, sin espacios. */
export function digitosDeTarjeta(valor: string): string {
  return valor.replace(/\D/g, '');
}

/** Token del proveedor simulado (P2): `sim_` y los últimos 4 dígitos de la tarjeta de prueba; vacío si no es válida. */
export function tokenSimulado(tarjeta: string): string {
  const digitos = digitosDeTarjeta(tarjeta);
  return digitos.length >= 13 && digitos.length <= 19 ? `sim_${digitos.slice(-4)}` : '';
}

/** Formulario del "Pago de prueba": pide un número de tarjeta de prueba y deja claro que no es un cobro real. */
@Component({
  selector: 'fs-formulario-simulado',
  template: `
    <div class="fs-campo">
      <label class="fs-campo__etiqueta" for="tarjeta-simulada">Número de tarjeta de prueba</label>
      <input
        id="tarjeta-simulada"
        name="tarjeta-simulada"
        class="fs-input"
        inputmode="numeric"
        autocomplete="off"
        placeholder="4111 1111 1111 1111"
        [value]="tarjeta()"
        (input)="tarjeta.set($any($event.target).value)"
      />
      <span class="fs-campo__ayuda">
        4111 1111 1111 1111 aprueba · 4000 0000 0000 0002 rechaza · 4000 0000 0000 0119 queda en confirmación
      </span>
    </div>
    <p class="fs-suave" style="display: flex; gap: 6px; align-items: center" role="note">
      <span class="fs-icono" style="font-size: 16px" aria-hidden="true">science</span>
      Esto es una demostración: no es un cobro real y no se usa ninguna tarjeta verdadera.
    </p>
  `,
})
export class FormularioSimulado {
  readonly tarjeta = model('');
  readonly token = computed(() => tokenSimulado(this.tarjeta()));
}
