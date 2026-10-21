import { Component, computed, input } from '@angular/core';
import { nivelFortaleza } from '../validaciones';

const ETIQUETAS = ['Muy débil', 'Débil', 'Regular', 'Buena', 'Fuerte'];

/** Barra de fortaleza de la contraseña (P-1-02): solo orienta, la política la valida el formulario. */
@Component({
  selector: 'fs-indicador-fortaleza',
  template: `
    <div [attr.data-fortaleza]="nivel()" style="display: flex; align-items: center; gap: 4px">
      @for (n of barras; track n) {
        <span
          aria-hidden="true"
          style="flex: 1; height: 6px; border-radius: 3px"
          [style.background]="n < nivel() ? 'var(--color-primario)' : 'var(--color-linea)'"
        ></span>
      }
      <span class="fs-suave" style="margin-left: 8px; font-size: 0.8125rem">{{ etiqueta() }}</span>
    </div>
  `,
})
export class IndicadorFortaleza {
  readonly password = input('');
  protected readonly barras = [0, 1, 2, 3];
  protected readonly nivel = computed(() => nivelFortaleza(this.password()));
  protected readonly etiqueta = computed(() => ETIQUETAS[this.nivel()]);
}
