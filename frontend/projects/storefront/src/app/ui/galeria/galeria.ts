import { Component, input, linkedSignal } from '@angular/core';

export interface ImagenGaleria {
  url: string;
  alt: string;
}

/** Galería del detalle: miniaturas y foto principal 4:5 (sistema de diseño: Galeria). */
@Component({
  selector: 'fs-galeria',
  template: `
    <div
      class="fs-galeria"
      tabindex="0"
      role="group"
      aria-label="Fotos del producto"
      (keydown)="alTeclear($event)"
    >
      <div class="fs-galeria__miniaturas">
        @for (img of imagenes(); track img.url; let i = $index) {
          <button
            type="button"
            class="fs-galeria__miniatura"
            [attr.aria-current]="i === indice() ? 'true' : null"
            [attr.aria-label]="'Ver foto ' + (i + 1)"
            (click)="indice.set(i)"
          >
            <img [src]="img.url" alt="" />
          </button>
        }
      </div>
      <div class="fs-galeria__principal">
        @if (imagenes()[indice()]; as actual) {
          <img [src]="actual.url" [alt]="actual.alt" />
        } @else {
          <span class="fs-icono" aria-hidden="true" style="font-size: 48px">image</span>
        }
      </div>
    </div>
  `,
})
export class FsGaleria {
  readonly imagenes = input<ImagenGaleria[]>([]);

  /** Al cambiar de color llegan otras fotos: se reinicia en la principal. */
  protected readonly indice = linkedSignal<ImagenGaleria[], number>({
    source: this.imagenes,
    computation: () => 0,
  });

  protected alTeclear(evento: KeyboardEvent): void {
    const total = this.imagenes().length;
    if (!total) return;
    if (evento.key === 'ArrowRight') this.indice.update((i) => Math.min(i + 1, total - 1));
    else if (evento.key === 'ArrowLeft') this.indice.update((i) => Math.max(i - 1, 0));
  }
}
