import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
} from '@angular/core';
import { ToastService, type TipoToast } from 'shared';

/** Cuánto dura un aviso de éxito, información o advertencia; los de error se quedan hasta cerrarlos. */
const DURACION_MS = 3000;
const MAXIMO_VISIBLES = 3;
const ICONOS: Record<TipoToast, string> = {
  exito: 'check_circle',
  error: 'error',
  aviso: 'warning',
  info: 'info',
};

/** Avisos flotantes (ficha `Aviso`): dibuja los de `ToastService`, máximo tres a la vez. */
@Component({
  selector: 'fs-toast',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="fs-toasts">
      @for (aviso of visibles(); track aviso.id) {
        <div
          class="fs-aviso"
          [class]="'fs-aviso fs-aviso--' + aviso.tipo"
          [attr.role]="aviso.tipo === 'error' ? 'alert' : 'status'"
        >
          <span class="fs-icono" aria-hidden="true">{{ icono(aviso.tipo) }}</span>
          <p class="fs-aviso__texto">{{ aviso.mensaje }}</p>
          <button
            type="button"
            class="fs-aviso__cerrar"
            aria-label="Cerrar aviso"
            (click)="servicio.descartar(aviso.id)"
          >
            <span class="fs-icono" aria-hidden="true">close</span>
          </button>
        </div>
      }
    </div>
  `,
  styleUrl: './toast.css',
})
export class Toast {
  protected readonly servicio = inject(ToastService);
  protected readonly visibles = computed(() => this.servicio.toasts().slice(-MAXIMO_VISIBLES));
  private readonly temporizadores = new Map<number, ReturnType<typeof setTimeout>>();

  constructor() {
    effect(() => {
      for (const aviso of this.servicio.toasts()) {
        if (aviso.tipo === 'error' || this.temporizadores.has(aviso.id)) continue;
        this.temporizadores.set(
          aviso.id,
          setTimeout(() => {
            this.temporizadores.delete(aviso.id);
            this.servicio.descartar(aviso.id);
          }, DURACION_MS),
        );
      }
    });
    inject(DestroyRef).onDestroy(() => this.temporizadores.forEach((t) => clearTimeout(t)));
  }

  protected icono(tipo: TipoToast): string {
    return ICONOS[tipo];
  }
}
