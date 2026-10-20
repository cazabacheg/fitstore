import { Component, DestroyRef, computed, effect, inject } from '@angular/core';
import { ToastService, type TipoToast } from 'shared';

const DURACION_MS = 5000;
const ICONOS: Record<TipoToast, string> = {
  exito: 'check_circle',
  error: 'error',
  aviso: 'warning',
  info: 'info',
};

/** Avisos flotantes que llegan por `ToastService` (sistema de diseño: Aviso). Máximo dos a la vez. */
@Component({
  selector: 'fs-toast',
  template: `
    <div class="fs-avisos">
      @for (t of visibles(); track t.id) {
        <div
          class="fs-aviso"
          [class]="'fs-aviso fs-aviso--' + t.tipo"
          [attr.role]="t.tipo === 'error' ? 'alert' : 'status'"
        >
          <span class="fs-icono" aria-hidden="true">{{ icono(t.tipo) }}</span>
          <span class="fs-aviso__texto">{{ t.mensaje }}</span>
          <button
            type="button"
            class="fs-icono-boton"
            style="width: 28px; height: 28px"
            aria-label="Cerrar aviso"
            (click)="servicio.descartar(t.id)"
          >
            <span class="fs-icono" aria-hidden="true" style="font-size: 18px">close</span>
          </button>
        </div>
      }
    </div>
  `,
})
export class FsToast {
  protected readonly servicio = inject(ToastService);
  protected readonly visibles = computed(() => this.servicio.toasts().slice(-2));

  private readonly programados = new Set<number>();
  private readonly temporizadores: ReturnType<typeof setTimeout>[] = [];

  constructor() {
    effect(() => {
      for (const t of this.servicio.toasts()) {
        // Los de error se quedan hasta que la persona los cierra.
        if (t.tipo === 'error' || this.programados.has(t.id)) continue;
        this.programados.add(t.id);
        this.temporizadores.push(setTimeout(() => this.servicio.descartar(t.id), DURACION_MS));
      }
    });
    inject(DestroyRef).onDestroy(() => this.temporizadores.forEach(clearTimeout));
  }

  protected icono(tipo: TipoToast): string {
    return ICONOS[tipo];
  }
}
