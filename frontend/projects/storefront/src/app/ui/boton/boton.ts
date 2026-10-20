import { Directive, computed, input } from '@angular/core';

export type VarianteBoton = 'primario' | 'contorno' | 'acento' | 'secundario' | 'texto' | 'peligro';
export type TamanoBoton = 'sm' | 'md' | 'lg';

/** Botón de acción (sistema de diseño: Boton). Una sola acción `primario` por vista. */
@Directive({
  selector: 'button[fsBoton], a[fsBoton]',
  host: { '[class]': 'clases()' },
})
export class FsBoton {
  readonly variante = input<VarianteBoton>('primario');
  readonly tamano = input<TamanoBoton>('md');
  readonly bloque = input(false);

  protected readonly clases = computed(() =>
    [
      'fs-btn',
      `fs-btn--${this.variante()}`,
      this.tamano() === 'md' ? '' : `fs-btn--${this.tamano()}`,
      this.bloque() ? 'fs-btn--bloque' : '',
    ]
      .filter(Boolean)
      .join(' '),
  );
}
