import { HttpContext } from '@angular/common/http';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { SIN_TOAST, ToastService, deliveryApi } from 'shared';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { DialogoService } from '../../ui/dialogo/dialogo.service';
import { EstadoCargando } from '../../ui/estados/estado-cargando';
import { EstadoError } from '../../ui/estados/estado-error';
import { EstadoVacio } from '../../ui/estados/estado-vacio';
import { DialogoAgencia } from './dialogo-agencia';

type Agencia = deliveryApi.Agencia;

/**
 * Agencias (P-6-06, `/agencias`, ADMIN): las filas se arrastran para fijar el orden en que el checkout las muestra
 * (CA-6-14). El nombre abre el diálogo de edición; las acciones son Desactivar o Activar y Eliminar (R11, CA-6-13 a CA-6-15).
 */
@Component({
  selector: 'fs-agencias',
  imports: [DragDropModule, EncabezadoPagina, EstadoCargando, EstadoError, EstadoVacio],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .agencia--arrastrando {
      background: var(--fs-color-superficie-alta, #f4f4f4);
    }
    .agencia__asa {
      cursor: grab;
      color: var(--fs-color-texto-suave);
      user-select: none;
    }
    .cdk-drag-preview {
      display: table;
      box-shadow: 0 2px 8px rgb(0 0 0 / 25%);
      background: var(--fs-color-superficie, #fff);
    }
    .cdk-drag-placeholder {
      opacity: 0.3;
    }
  `,
  template: `
    <fs-encabezado-pagina titulo="Agencias">
      <button accion type="button" class="fs-btn fs-btn--primario" (click)="nueva()">
        Nueva agencia
      </button>
    </fs-encabezado-pagina>
    @if (cargando()) {
      <fs-estado-cargando />
    } @else if (error()) {
      <fs-estado-error texto="No pudimos cargar las agencias." (reintentar)="cargar()" />
    } @else if (agencias().length === 0) {
      <fs-estado-vacio titulo="Sin agencias" texto="Crea la primera agencia de transporte." />
    } @else {
      <p class="fs-suave">
        Arrastra las filas para cambiar el orden en que el cliente ve las agencias.
      </p>
      <div class="fs-tabla-contenedor">
        <table class="fs-tabla">
          <thead>
            <tr>
              <th scope="col"><span class="fs-sr-only">Orden</span></th>
              <th scope="col">Nombre</th>
              <th scope="col">Enlace de rastreo</th>
              <th scope="col">Estado</th>
              <th scope="col" class="fs-acciones">Acciones</th>
            </tr>
          </thead>
          <tbody cdkDropList (cdkDropListDropped)="soltar($event)">
            @for (a of agencias(); track a.id; let i = $index) {
              <tr cdkDrag [cdkDragData]="a" data-testid="fila-agencia">
                <td class="agencia__asa" cdkDragHandle aria-hidden="true">⠿</td>
                <td>
                  <button type="button" class="fs-enlace fs-codigo" (click)="editar(a)">
                    {{ a.nombre }}
                  </button>
                </td>
                <td>{{ a.urlSeguimiento ?? '—' }}</td>
                <td>
                  <span
                    class="fs-insignia"
                    [class]="'fs-insignia fs-insignia--' + (a.activa ? 'exito' : 'neutra')"
                    >{{ a.activa ? 'Activa' : 'Inactiva' }}</span
                  >
                </td>
                <td class="fs-acciones">
                  <button
                    type="button"
                    class="fs-btn fs-btn--texto fs-btn--sm"
                    [attr.aria-label]="'Subir ' + a.nombre"
                    [disabled]="i === 0"
                    (click)="mover(i, i - 1)"
                  >
                    ▲
                  </button>
                  <button
                    type="button"
                    class="fs-btn fs-btn--texto fs-btn--sm"
                    [attr.aria-label]="'Bajar ' + a.nombre"
                    [disabled]="i === agencias().length - 1"
                    (click)="mover(i, i + 1)"
                  >
                    ▼
                  </button>
                  <button
                    type="button"
                    class="fs-btn fs-btn--texto fs-btn--sm"
                    (click)="cambiarActiva(a)"
                  >
                    {{ a.activa ? 'Desactivar' : 'Activar' }}
                  </button>
                  <button
                    type="button"
                    class="fs-btn fs-btn--texto fs-btn--sm"
                    (click)="eliminar(a)"
                  >
                    Eliminar
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
})
export class AgenciasPage {
  private readonly api = inject(deliveryApi.AgenciasAdminService);
  private readonly dialogos = inject(MatDialog);
  private readonly confirmacion = inject(DialogoService);
  private readonly toasts = inject(ToastService);

  protected readonly agencias = signal<Agencia[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.api
      .listarAgenciasAdmin('body', false, { context: new HttpContext().set(SIN_TOAST, true) })
      .subscribe({
        next: (r) => {
          this.agencias.set(r.data ?? []);
          this.cargando.set(false);
        },
        error: () => {
          this.cargando.set(false);
          this.error.set(true);
        },
      });
  }

  protected nueva(): void {
    this.abrirDialogo(undefined, 'Agencia creada');
  }

  protected editar(agencia: Agencia): void {
    this.abrirDialogo(agencia, 'Guardado');
  }

  private abrirDialogo(agencia: Agencia | undefined, aviso: string): void {
    this.dialogos
      .open<DialogoAgencia, { agencia?: Agencia }, Agencia | undefined>(DialogoAgencia, {
        data: { agencia },
        width: '520px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((guardada) => {
        if (!guardada) return;
        this.toasts.mostrar('exito', aviso);
        this.cargar();
      });
  }

  protected soltar(evento: CdkDragDrop<Agencia[]>): void {
    this.mover(evento.previousIndex, evento.currentIndex);
  }

  /** Mueve la agencia en la lista y le dice al servidor su nueva posición (la primera es 0). */
  protected mover(desde: number, hacia: number): void {
    if (desde === hacia || hacia < 0 || hacia >= this.agencias().length) return;
    const lista = [...this.agencias()];
    const movida = lista[desde];
    moveItemInArray(lista, desde, hacia);
    this.agencias.set(lista);
    this.api.ordenarAgencia(movida.id, { orden: hacia }).subscribe({
      next: (r) => {
        this.agencias.set(r.data ?? lista);
        this.toasts.mostrar('exito', 'Orden guardado');
      },
      error: () => this.cargar(),
    });
  }

  protected cambiarActiva(agencia: Agencia): void {
    const llamada = agencia.activa
      ? this.api.desactivarAgencia(agencia.id)
      : this.api.activarAgencia(agencia.id);
    llamada.subscribe({
      next: () => {
        this.toasts.mostrar('exito', agencia.activa ? 'Agencia desactivada' : 'Agencia activada');
        this.cargar();
      },
      error: () => this.cargar(),
    });
  }

  protected eliminar(agencia: Agencia): void {
    this.confirmacion
      .confirmar({
        titulo: `¿Eliminar ${agencia.nombre}?`,
        mensaje:
          'Si ya tiene entregas no se puede eliminar: desactívala para que el checkout no la muestre.',
        accion: 'Eliminar',
        peligro: true,
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.api.eliminarAgencia(agencia.id).subscribe({
          next: () => {
            this.toasts.mostrar('exito', 'Agencia eliminada');
            this.cargar();
          },
          // 409 con "desactívala": el interceptor muestra el motivo
          error: () => this.cargar(),
        });
      });
  }
}
