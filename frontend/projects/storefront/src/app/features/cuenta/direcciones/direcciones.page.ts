import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { SIN_TOAST, ToastService, authApi } from 'shared';
import { FsBoton } from '../../../ui/boton/boton';
import { DialogoConfirmacion } from '../../../ui/dialogo/dialogo-confirmacion';
import { EstadoCargando, EstadoError, EstadoVacio } from '../../../ui/estados/estados';
import { DialogoDireccionService } from './dialogo-direccion.service';
import { TarjetaDireccion } from './tarjeta-direccion';

/** Máximo de direcciones por cliente (invariante 4 de auth). */
export const MAXIMO_DIRECCIONES = 10;

/** Mis direcciones (P-1-06): lista, alta y edición con `DialogoDireccion`, predeterminada y borrado (CA-1-17 a CA-1-20). */
@Component({
  selector: 'fs-direcciones-page',
  imports: [
    FsBoton,
    TarjetaDireccion,
    DialogoConfirmacion,
    EstadoCargando,
    EstadoError,
    EstadoVacio,
  ],
  template: `
    <header
      style="display: flex; align-items: center; justify-content: space-between; gap: var(--espacio-4); flex-wrap: wrap"
    >
      <h1 style="margin: 0">Mis direcciones</h1>
      <button
        type="button"
        fsBoton
        variante="primario"
        [disabled]="limiteAlcanzado()"
        (click)="agregar()"
      >
        Agregar dirección
      </button>
    </header>
    @if (limiteAlcanzado()) {
      <p class="fs-suave">
        Llegaste al máximo de {{ maximo }} direcciones. Elimina una para poder agregar otra.
      </p>
    }
    <div style="margin-top: var(--espacio-5)">
      @switch (estado()) {
        @case ('cargando') {
          <fs-estado-cargando />
        }
        @case ('error') {
          <fs-estado-error titulo="No pudimos cargar tus direcciones" (reintentar)="cargar()" />
        }
        @default {
          @if (direcciones().length === 0) {
            <fs-estado-vacio
              titulo="Aún no tienes direcciones"
              texto="Guarda una para elegirla rápido al pagar."
            />
          } @else {
            <div style="display: grid; gap: var(--espacio-4)">
              @for (d of direcciones(); track d.id) {
                <fs-tarjeta-direccion
                  [direccion]="d"
                  (editar)="editar(d)"
                  (eliminar)="pendiente.set(d)"
                  (predeterminar)="predeterminar(d)"
                />
              }
            </div>
          }
        }
      }
    </div>
    <fs-dialogo-confirmacion
      [titulo]="'¿Eliminar la dirección ' + (pendiente()?.alias ?? '') + '?'"
      texto="Esta acción no se puede deshacer."
      textoConfirmar="Eliminar dirección"
      [peligro]="true"
      [abierto]="pendiente() !== null"
      (cancelado)="pendiente.set(null)"
      (confirmado)="eliminar()"
    />
  `,
})
export class DireccionesPage {
  private readonly api = inject(authApi.ClienteService);
  private readonly dialogo = inject(DialogoDireccionService);
  private readonly toasts = inject(ToastService);
  private readonly sinToast = () => ({ context: new HttpContext().set(SIN_TOAST, true) });

  protected readonly maximo = MAXIMO_DIRECCIONES;
  protected readonly estado = signal<'cargando' | 'listo' | 'error'>('cargando');
  protected readonly direcciones = signal<authApi.Direccion[]>([]);
  protected readonly pendiente = signal<authApi.Direccion | null>(null);
  protected readonly limiteAlcanzado = computed(
    () => this.direcciones().length >= MAXIMO_DIRECCIONES,
  );

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.estado.set('cargando');
    this.api.listarMisDirecciones('body', false, this.sinToast()).subscribe({
      next: (respuesta) => {
        this.direcciones.set(respuesta.data ?? []);
        this.estado.set('listo');
      },
      error: () => this.estado.set('error'),
    });
  }

  protected async agregar(): Promise<void> {
    if (await this.dialogo.abrir(undefined)) this.cargar();
  }

  protected async editar(direccion: authApi.Direccion): Promise<void> {
    if (await this.dialogo.abrir(direccion)) this.cargar();
  }

  protected predeterminar(direccion: authApi.Direccion): void {
    this.api.marcarDireccionPredeterminada(direccion.id, 'body', false, this.sinToast()).subscribe({
      next: (respuesta) => this.direcciones.set(respuesta.data ?? []),
      error: () => this.toasts.mostrar('error', 'No pudimos cambiar la dirección predeterminada'),
    });
  }

  protected eliminar(): void {
    const direccion = this.pendiente();
    this.pendiente.set(null);
    if (!direccion) return;
    this.api.eliminarDireccion(direccion.id, 'body', false, this.sinToast()).subscribe({
      next: () => this.cargar(),
      error: () => this.toasts.mostrar('error', 'No pudimos eliminar la dirección'),
    });
  }
}
