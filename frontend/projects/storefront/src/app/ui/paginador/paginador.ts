import { Component, computed, input, output } from '@angular/core';

type Elemento = number | 'elipsis-ini' | 'elipsis-fin';

/** Paginador numérico de máximo 7 botones; `pagina` cuenta desde 0 como la API (sistema de diseño: Paginador). */
@Component({
  selector: 'fs-paginador',
  template: `
    @if (totalPaginas() > 1) {
      <nav class="fs-paginador" aria-label="Paginación">
        <button
          type="button"
          aria-label="Página anterior"
          [disabled]="pagina() === 0"
          (click)="ir(pagina() - 1)"
        >
          <span class="fs-icono" aria-hidden="true">chevron_left</span>
        </button>
        @for (e of elementos(); track e) {
          @if (e === 'elipsis-ini' || e === 'elipsis-fin') {
            <span class="fs-paginador__elipsis" aria-hidden="true">…</span>
          } @else {
            <button
              type="button"
              [attr.data-pagina]="e"
              [attr.aria-label]="'Página ' + (e + 1)"
              [attr.aria-current]="e === pagina() ? 'page' : null"
              (click)="ir(e)"
            >
              {{ e + 1 }}
            </button>
          }
        }
        <button
          type="button"
          aria-label="Página siguiente"
          [disabled]="pagina() >= totalPaginas() - 1"
          (click)="ir(pagina() + 1)"
        >
          <span class="fs-icono" aria-hidden="true">chevron_right</span>
        </button>
      </nav>
    }
  `,
})
export class FsPaginador {
  readonly pagina = input.required<number>();
  readonly totalPaginas = input.required<number>();
  readonly cambio = output<number>();

  protected readonly elementos = computed<Elemento[]>(() => {
    const total = this.totalPaginas();
    const p = this.pagina();
    const ultima = total - 1;
    if (total <= 7) return Array.from({ length: total }, (_, i) => i);
    if (p <= 3) return [0, 1, 2, 3, 4, 'elipsis-fin', ultima];
    if (p >= total - 4)
      return [0, 'elipsis-ini', ...Array.from({ length: 5 }, (_, i) => total - 5 + i)];
    return [0, 'elipsis-ini', p - 1, p, p + 1, 'elipsis-fin', ultima];
  });

  protected ir(pagina: number): void {
    if (pagina >= 0 && pagina < this.totalPaginas() && pagina !== this.pagina()) {
      this.cambio.emit(pagina);
    }
  }
}
