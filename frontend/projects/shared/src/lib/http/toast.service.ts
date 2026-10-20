import { Injectable, signal } from '@angular/core';

export type TipoToast = 'exito' | 'error' | 'aviso' | 'info';

export interface Toast {
  id: number;
  tipo: TipoToast;
  mensaje: string;
}

/** Avisos temporales. El componente `Toast` de `ui/` de cada app los dibuja y los descarta. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly lista = signal<Toast[]>([]);
  private siguiente = 1;

  readonly toasts = this.lista.asReadonly();

  mostrar(tipo: TipoToast, mensaje: string): void {
    this.lista.update((actual) => [...actual, { id: this.siguiente++, tipo, mensaje }]);
  }

  descartar(id: number): void {
    this.lista.update((actual) => actual.filter((t) => t.id !== id));
  }
}
