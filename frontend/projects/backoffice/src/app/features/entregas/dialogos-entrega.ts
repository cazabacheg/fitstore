import { HttpContext } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import type { Observable } from 'rxjs';
import { SIN_TOAST, camposConError, comoApiError, deliveryApi } from 'shared';
import { CampoFormulario } from '../acceso/campo-formulario';
import { hoyEnLima } from './acciones-entrega';

/** Entrega que se está avanzando; lleva la versión que vio el operador (E6). */
export interface DatosDialogoEntrega {
  entrega: deliveryApi.Entrega;
}

const SIN_AVISO = { context: new HttpContext().set(SIN_TOAST, true) };

/** Lo que tienen en común los diálogos de avance: enviar, mostrar el error del servidor y cerrar con la entrega nueva. */
abstract class DialogoAvance {
  protected readonly ref =
    inject<MatDialogRef<unknown, deliveryApi.Entrega | undefined>>(MatDialogRef);
  protected readonly entrega = inject<DatosDialogoEntrega>(MAT_DIALOG_DATA).entrega;
  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal('');
  protected readonly errores = signal<Record<string, string>>({});
  protected readonly intentado = signal(false);

  protected enviar(llamada: () => Observable<deliveryApi.RespuestaEntrega>): void {
    this.enviando.set(true);
    this.errorGeneral.set('');
    this.errores.set({});
    llamada().subscribe({
      next: (r) => {
        this.enviando.set(false);
        if (r.data) this.ref.close(r.data);
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        const campos = camposConError(error);
        if (campos.length > 0) {
          this.errores.set(Object.fromEntries(campos.map((c) => [c.campo, c.mensaje])));
        } else {
          this.errorGeneral.set(
            comoApiError(error)?.mensaje ?? 'No pudimos guardar. Inténtalo en un momento.',
          );
        }
      },
    });
  }

  protected elegido(evento: Event): string {
    return (evento.target as HTMLSelectElement).value;
  }
}

/** Asignar un repartidor y una fecha, o reprogramar (CA-6-20 y CA-6-22). */
@Component({
  selector: 'fs-dialogo-asignar',
  imports: [MatDialogModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>
      {{ entrega.estado === 'PENDIENTE_ASIGNACION' ? 'Asignar entrega' : 'Reprogramar entrega' }}
      {{ entrega.codigoPedido }}
    </h2>
    <mat-dialog-content>
      <form
        id="form-asignar"
        novalidate
        (submit)="guardar($event)"
        style="display: grid; gap: var(--espacio-4); padding-top: var(--espacio-2)"
      >
        @if (errorGeneral()) {
          <div class="fs-aviso fs-aviso--error" role="alert">
            <span class="fs-aviso__texto">{{ errorGeneral() }}</span>
          </div>
        }
        <div class="fs-campo" [class.fs-campo--error]="!!error('repartidorId')">
          <label class="fs-campo__etiqueta" for="repartidor-asignar">Repartidor</label>
          <select
            id="repartidor-asignar"
            class="fs-select"
            (change)="repartidorId.set(elegido($event))"
          >
            <option value="" [selected]="!repartidorId()">Elige un repartidor</option>
            @for (r of repartidores(); track r.id) {
              <option [value]="r.id" [selected]="'' + r.id === repartidorId()">
                {{ r.nombres }} · {{ r.vehiculo }}
              </option>
            }
          </select>
          @if (error('repartidorId')) {
            <span class="fs-campo__error">{{ error('repartidorId') }}</span>
          }
        </div>
        <div class="fs-campo" [class.fs-campo--error]="!!error('fechaProgramada')">
          <label class="fs-campo__etiqueta" for="fecha-asignar">Fecha</label>
          <input
            id="fecha-asignar"
            type="date"
            class="fs-input"
            [min]="hoy"
            [value]="fecha()"
            (input)="fecha.set(elegido($event))"
          />
          @if (error('fechaProgramada')) {
            <span class="fs-campo__error">{{ error('fechaProgramada') }}</span>
          }
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions>
      <button type="button" class="fs-btn fs-btn--contorno" [mat-dialog-close]="undefined">
        Cancelar
      </button>
      <button
        type="submit"
        form="form-asignar"
        class="fs-btn fs-btn--primario"
        [disabled]="enviando()"
      >
        Guardar
      </button>
    </mat-dialog-actions>
  `,
})
export class DialogoAsignar extends DialogoAvance {
  private readonly entregas = inject(deliveryApi.EntregasService);
  private readonly api = inject(deliveryApi.RepartidoresService);

  protected readonly hoy = hoyEnLima();
  protected readonly repartidores = signal<deliveryApi.Repartidor[]>([]);
  protected readonly repartidorId = signal(
    this.entrega.repartidor?.id ? '' + this.entrega.repartidor.id : '',
  );
  protected readonly fecha = signal(
    this.entrega.fechaProgramada && this.entrega.fechaProgramada >= this.hoy
      ? this.entrega.fechaProgramada
      : this.hoy,
  );

  constructor() {
    super();
    this.api.listarRepartidores(true, 'body', false, SIN_AVISO).subscribe({
      next: (r) => this.repartidores.set(r.data ?? []),
      error: () => this.errorGeneral.set('No pudimos cargar los repartidores.'),
    });
  }

  protected error(campo: string): string {
    return this.errores()[campo] ?? '';
  }

  protected guardar(evento: Event): void {
    evento.preventDefault();
    this.intentado.set(true);
    if (!this.repartidorId()) {
      this.errores.set({ repartidorId: 'Elige un repartidor' });
      return;
    }
    if (!this.fecha() || this.fecha() < this.hoy) {
      this.errores.set({ fechaProgramada: 'La fecha debe ser hoy o posterior' });
      return;
    }
    this.enviar(() =>
      this.entregas.asignarEntrega(
        this.entrega.id,
        {
          repartidorId: Number(this.repartidorId()),
          fechaProgramada: this.fecha(),
          version: this.entrega.version,
        },
        'body',
        false,
        SIN_AVISO,
      ),
    );
  }
}

/** "No se pudo entregar": la observación es obligatoria (CA-6-22). */
@Component({
  selector: 'fs-dialogo-fallida',
  imports: [MatDialogModule, CampoFormulario],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>No se pudo entregar {{ entrega.codigoPedido }}</h2>
    <mat-dialog-content>
      <form
        id="form-fallida"
        novalidate
        (submit)="guardar($event)"
        style="padding-top: var(--espacio-2)"
      >
        @if (errorGeneral()) {
          <div class="fs-aviso fs-aviso--error" role="alert">
            <span class="fs-aviso__texto">{{ errorGeneral() }}</span>
          </div>
        }
        <fs-campo-formulario
          etiqueta="Observación"
          placeholder="No había nadie, dirección incorrecta…"
          [(valor)]="observacion"
          [error]="mensaje()"
        />
      </form>
    </mat-dialog-content>
    <mat-dialog-actions>
      <button type="button" class="fs-btn fs-btn--contorno" [mat-dialog-close]="undefined">
        Cancelar
      </button>
      <button
        type="submit"
        form="form-fallida"
        class="fs-btn fs-btn--primario"
        [disabled]="enviando()"
      >
        Marcar como fallida
      </button>
    </mat-dialog-actions>
  `,
})
export class DialogoFallida extends DialogoAvance {
  private readonly entregas = inject(deliveryApi.EntregasService);
  protected readonly observacion = signal('');

  protected mensaje(): string {
    if (this.errores()['observacion']) return this.errores()['observacion'];
    return this.intentado() && this.observacion().trim().length < 3
      ? 'Cuéntanos por qué no se pudo entregar'
      : '';
  }

  protected guardar(evento: Event): void {
    evento.preventDefault();
    this.intentado.set(true);
    if (this.observacion().trim().length < 3) return;
    this.enviar(() =>
      this.entregas.marcarFallida(
        this.entrega.id,
        { observacion: this.observacion().trim(), version: this.entrega.version },
        'body',
        false,
        SIN_AVISO,
      ),
    );
  }
}

/** Registrar el envío por agencia: código obligatorio, clave opcional y, si hace falta, otra agencia (CA-6-26). */
@Component({
  selector: 'fs-dialogo-envio-agencia',
  imports: [MatDialogModule, CampoFormulario],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Registrar envío {{ entrega.codigoPedido }}</h2>
    <mat-dialog-content>
      <form
        id="form-envio"
        novalidate
        (submit)="guardar($event)"
        style="display: grid; gap: var(--espacio-4); padding-top: var(--espacio-2)"
      >
        @if (errorGeneral()) {
          <div class="fs-aviso fs-aviso--error" role="alert">
            <span class="fs-aviso__texto">{{ errorGeneral() }}</span>
          </div>
        }
        <div class="fs-campo">
          <label class="fs-campo__etiqueta" for="agencia-envio">Agencia</label>
          <select id="agencia-envio" class="fs-select" (change)="agenciaId.set(elegido($event))">
            @for (a of agencias(); track a.id) {
              <option [value]="a.id" [selected]="'' + a.id === agenciaId()">{{ a.nombre }}</option>
            }
          </select>
        </div>
        <fs-campo-formulario
          etiqueta="Código de seguimiento"
          [(valor)]="codigo"
          [error]="mensajeCodigo()"
        />
        <fs-campo-formulario
          etiqueta="Clave de recojo (opcional)"
          [(valor)]="clave"
          [error]="errores()['claveRecojo'] ?? ''"
        />
      </form>
    </mat-dialog-content>
    <mat-dialog-actions>
      <button type="button" class="fs-btn fs-btn--contorno" [mat-dialog-close]="undefined">
        Cancelar
      </button>
      <button
        type="submit"
        form="form-envio"
        class="fs-btn fs-btn--primario"
        [disabled]="enviando()"
      >
        Registrar envío
      </button>
    </mat-dialog-actions>
  `,
})
export class DialogoEnvioAgencia extends DialogoAvance {
  private readonly entregas = inject(deliveryApi.EntregasService);
  private readonly envios = inject(deliveryApi.EnviosService);

  protected readonly agencias = signal<deliveryApi.Agencia[]>([]);
  protected readonly agenciaId = signal(this.entrega.agenciaId ? '' + this.entrega.agenciaId : '');
  protected readonly codigo = signal(this.entrega.codigoSeguimiento ?? '');
  protected readonly clave = signal(this.entrega.claveRecojo ?? '');

  constructor() {
    super();
    this.envios.listarAgenciasActivas('body', false, SIN_AVISO).subscribe({
      next: (r) => this.agencias.set(r.data ?? []),
      error: () => this.errorGeneral.set('No pudimos cargar las agencias.'),
    });
  }

  protected mensajeCodigo(): string {
    if (this.errores()['codigoSeguimiento']) return this.errores()['codigoSeguimiento'];
    return this.intentado() && this.codigo().trim().length < 3
      ? 'Ingresa el código de seguimiento'
      : '';
  }

  protected guardar(evento: Event): void {
    evento.preventDefault();
    this.intentado.set(true);
    if (this.codigo().trim().length < 3) return;
    const otra = Number(this.agenciaId());
    this.enviar(() =>
      this.entregas.registrarEnvioAgencia(
        this.entrega.id,
        {
          // Solo se manda la agencia si el operador la cambió respecto de la que eligió el cliente
          agenciaId: otra && otra !== this.entrega.agenciaId ? otra : null,
          codigoSeguimiento: this.codigo().trim(),
          claveRecojo: this.clave().trim() || null,
          version: this.entrega.version,
        },
        'body',
        false,
        SIN_AVISO,
      ),
    );
  }
}
