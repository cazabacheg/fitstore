import { A11yModule } from '@angular/cdk/a11y';
import { Component, effect, input, model, output } from '@angular/core';
import { idUnico } from '../id-unico';

/**
 * Diálogo modal (sistema de diseño: Dialogo): foco atrapado, Esc cierra y el foco vuelve al elemento que lo abrió.
 * Las acciones se proyectan con el atributo `fsAcciones`.
 */
@Component({
  selector: 'fs-dialogo',
  imports: [A11yModule],
  template: `
    @if (abierto()) {
      <div class="fs-dialogo-fondo">
        <div
          class="fs-dialogo"
          role="dialog"
          aria-modal="true"
          [attr.aria-labelledby]="idTitulo"
          cdkTrapFocus
          [cdkTrapFocusAutoCapture]="true"
          (keydown.escape)="cerrar()"
        >
          <h2 class="fs-dialogo__titulo" [id]="idTitulo">{{ titulo() }}</h2>
          <ng-content />
          <div class="fs-dialogo__acciones"><ng-content select="[fsAcciones]" /></div>
        </div>
      </div>
    }
  `,
})
export class FsDialogo {
  protected readonly idTitulo = idUnico('fs-dialogo');

  readonly titulo = input.required<string>();
  readonly abierto = model(false);
  readonly cerrado = output<void>();

  private previo: HTMLElement | null = null;
  private estabaAbierto = false;

  constructor() {
    effect(() => {
      const abierto = this.abierto();
      if (abierto && !this.estabaAbierto) {
        this.previo = document.activeElement as HTMLElement | null;
      } else if (!abierto && this.estabaAbierto) {
        this.previo?.focus();
        this.previo = null;
      }
      this.estabaAbierto = abierto;
    });
  }

  protected cerrar(): void {
    this.abierto.set(false);
    this.cerrado.emit();
  }
}
