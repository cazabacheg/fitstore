import { Component, computed, input } from '@angular/core';

export type VarianteBadge = 'exito' | 'aviso' | 'error' | 'info' | 'marca' | 'acento' | 'neutra';

/** Insignia con palabra en un color semántico (sistema de diseño: Insignia). */
@Component({
  selector: 'fs-badge',
  template: `<span [class]="clase()"><ng-content /></span>`,
})
export class FsBadge {
  readonly variante = input<VarianteBadge>('neutra');

  protected readonly clase = computed(() =>
    this.variante() === 'neutra' ? 'fs-insignia' : `fs-insignia fs-insignia--${this.variante()}`,
  );
}
