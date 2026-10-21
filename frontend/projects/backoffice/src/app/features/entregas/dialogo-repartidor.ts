import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { SIN_TOAST, camposConError, comoApiError, deliveryApi } from 'shared';
import { CampoFormulario } from '../acceso/campo-formulario';

export interface DatosDialogoRepartidor {
  /** Repartidor que se edita; sin él, se registra uno nuevo. */
  repartidor?: deliveryApi.Repartidor;
}

/** Registrar o editar un repartidor (P-6-04). Celular de 9 dígitos que empieza con 9; placa obligatoria en moto y auto (CA-6-16). */
@Component({
  selector: 'fs-dialogo-repartidor',
  imports: [MatDialogModule, CampoFormulario],
  template: `
    <h2 mat-dialog-title>{{ repartidor ? 'Editar repartidor' : 'Registrar repartidor' }}</h2>
    <mat-dialog-content>
      <form
        id="form-repartidor"
        novalidate
        (submit)="guardar($event)"
        style="display: grid; gap: var(--espacio-4); padding-top: var(--espacio-2)"
      >
        @if (errorGeneral()) {
          <div class="fs-aviso fs-aviso--error" role="alert">
            <span class="fs-aviso__texto">{{ errorGeneral() }}</span>
          </div>
        }
        <fs-campo-formulario etiqueta="Nombres" [(valor)]="nombres" [error]="errores().nombres" />
        <fs-campo-formulario
          etiqueta="Celular"
          tipo="tel"
          placeholder="987654321"
          [(valor)]="telefono"
          [error]="errores().telefono"
        />
        <div class="fs-campo">
          <label class="fs-campo__etiqueta" for="vehiculo-repartidor">Vehículo</label>
          <select
            id="vehiculo-repartidor"
            class="fs-select"
            (change)="vehiculo.set(elegido($event))"
          >
            <option value="MOTO" [selected]="vehiculo() === 'MOTO'">Moto</option>
            <option value="AUTO" [selected]="vehiculo() === 'AUTO'">Auto</option>
            <option value="BICICLETA" [selected]="vehiculo() === 'BICICLETA'">Bicicleta</option>
          </select>
        </div>
        @if (vehiculo() !== 'BICICLETA') {
          <fs-campo-formulario
            etiqueta="Placa"
            placeholder="ABC-123"
            [(valor)]="placa"
            [error]="errores().placa"
          />
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions>
      <button type="button" class="fs-btn fs-btn--contorno" [mat-dialog-close]="undefined">
        Cancelar
      </button>
      <button
        type="submit"
        form="form-repartidor"
        class="fs-btn fs-btn--primario"
        [disabled]="enviando()"
      >
        {{ repartidor ? 'Guardar' : 'Registrar' }}
      </button>
    </mat-dialog-actions>
  `,
})
export class DialogoRepartidor {
  private readonly api = inject(deliveryApi.RepartidoresService);
  private readonly ref =
    inject<MatDialogRef<DialogoRepartidor, deliveryApi.Repartidor | undefined>>(MatDialogRef);
  protected readonly repartidor = inject<DatosDialogoRepartidor>(MAT_DIALOG_DATA).repartidor;

  protected readonly nombres = signal(this.repartidor?.nombres ?? '');
  protected readonly telefono = signal(this.repartidor?.telefono ?? '');
  protected readonly vehiculo = signal<string>(this.repartidor?.vehiculo ?? 'MOTO');
  protected readonly placa = signal(this.repartidor?.placa ?? '');
  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal('');
  private readonly intentado = signal(false);
  private readonly delServidor = signal<Record<string, string>>({});

  protected readonly errores = computed(() => {
    const servidor = this.delServidor();
    const cliente = this.intentado()
      ? {
          nombres: this.nombres().trim().length < 3 ? 'Ingresa el nombre (mínimo 3 letras)' : '',
          telefono: /^9\d{8}$/.test(this.telefono().trim())
            ? ''
            : 'Ingresa un celular de 9 dígitos que empiece con 9',
          placa:
            this.vehiculo() !== 'BICICLETA' && !this.placa().trim()
              ? 'La placa es obligatoria para moto y auto'
              : this.placa().trim() && !/^[A-Z0-9]{3}-?[A-Z0-9]{3}$/.test(this.placa().trim())
                ? 'Ingresa una placa válida, por ejemplo ABC-123'
                : '',
        }
      : { nombres: '', telefono: '', placa: '' };
    return Object.fromEntries(
      Object.entries(cliente).map(([campo, mensaje]) => [campo, mensaje || servidor[campo] || '']),
    ) as typeof cliente;
  });

  protected elegido(evento: Event): string {
    return (evento.target as HTMLSelectElement).value;
  }

  protected guardar(evento: Event): void {
    evento.preventDefault();
    this.intentado.set(true);
    this.errorGeneral.set('');
    this.delServidor.set({});
    if (Object.values(this.errores()).some(Boolean) || this.enviando()) return;

    const pedido: deliveryApi.RepartidorRequest = {
      nombres: this.nombres().trim(),
      telefono: this.telefono().trim(),
      vehiculo: this.vehiculo() as deliveryApi.Vehiculo,
      placa: this.vehiculo() === 'BICICLETA' ? null : this.placa().trim(),
    };
    const contexto = { context: new HttpContext().set(SIN_TOAST, true) };
    const llamada = this.repartidor
      ? this.api.editarRepartidor(this.repartidor.id, pedido, 'body', false, contexto)
      : this.api.crearRepartidor(pedido, 'body', false, contexto);
    this.enviando.set(true);
    llamada.subscribe({
      next: (r) => {
        this.enviando.set(false);
        if (r.data) this.ref.close(r.data);
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        const campos = camposConError(error);
        if (campos.length > 0) {
          this.delServidor.set(Object.fromEntries(campos.map((c) => [c.campo, c.mensaje])));
        } else {
          this.errorGeneral.set(
            comoApiError(error)?.mensaje ?? 'No pudimos guardar. Inténtalo en un momento.',
          );
        }
      },
    });
  }
}
