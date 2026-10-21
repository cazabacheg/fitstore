import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { SIN_TOAST, camposConError, comoApiError, deliveryApi } from 'shared';
import { CampoFormulario } from '../acceso/campo-formulario';

export interface DatosDialogoAgencia {
  /** Agencia que se edita; sin ella, se crea una nueva. */
  agencia?: deliveryApi.Agencia;
}

const PLANTILLA = /^https:\/\/.*\{codigo\}.*$/;
const CODIGO_EJEMPLO = 'ABC123';

/** Crear o editar una agencia (P-6-06) con vista previa del enlace de rastreo con un código de ejemplo (CA-6-13). */
@Component({
  selector: 'fs-dialogo-agencia',
  imports: [MatDialogModule, CampoFormulario],
  template: `
    <h2 mat-dialog-title>{{ agencia ? 'Editar agencia' : 'Nueva agencia' }}</h2>
    <mat-dialog-content>
      <form
        id="form-agencia"
        novalidate
        (submit)="guardar($event)"
        style="display: grid; gap: var(--espacio-4); padding-top: var(--espacio-2)"
      >
        @if (errorGeneral()) {
          <div class="fs-aviso fs-aviso--error" role="alert">
            <span class="fs-aviso__texto">{{ errorGeneral() }}</span>
          </div>
        }
        <fs-campo-formulario etiqueta="Nombre" [(valor)]="nombre" [error]="errores().nombre" />
        <fs-campo-formulario
          etiqueta="Enlace de rastreo (opcional)"
          placeholder="https://rastreo.agencia.pe/?codigo={codigo}"
          ayuda="Debe ser https e incluir {codigo}, que se reemplaza por el código de seguimiento."
          [(valor)]="url"
          [error]="errores().urlSeguimiento"
        />
        @if (vistaPrevia(); as enlace) {
          <p class="fs-suave" data-testid="vista-previa">
            Vista previa con el código {{ ejemplo }}:
            <a [href]="enlace" target="_blank" rel="noopener noreferrer">{{ enlace }}</a>
          </p>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions>
      <button type="button" class="fs-btn fs-btn--contorno" [mat-dialog-close]="undefined">
        Cancelar
      </button>
      <button
        type="submit"
        form="form-agencia"
        class="fs-btn fs-btn--primario"
        [disabled]="enviando()"
      >
        Guardar
      </button>
    </mat-dialog-actions>
  `,
})
export class DialogoAgencia {
  private readonly api = inject(deliveryApi.AgenciasAdminService);
  private readonly ref =
    inject<MatDialogRef<DialogoAgencia, deliveryApi.Agencia | undefined>>(MatDialogRef);
  protected readonly agencia = inject<DatosDialogoAgencia>(MAT_DIALOG_DATA).agencia;
  protected readonly ejemplo = CODIGO_EJEMPLO;

  protected readonly nombre = signal(this.agencia?.nombre ?? '');
  protected readonly url = signal(this.agencia?.urlSeguimiento ?? '');
  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal('');
  private readonly intentado = signal(false);
  private readonly delServidor = signal<Record<string, string>>({});

  protected readonly vistaPrevia = computed(() => {
    const plantilla = this.url().trim();
    return PLANTILLA.test(plantilla) ? plantilla.replaceAll('{codigo}', CODIGO_EJEMPLO) : '';
  });

  protected readonly errores = computed(() => {
    const servidor = this.delServidor();
    const url = this.url().trim();
    const cliente = this.intentado()
      ? {
          nombre: this.nombre().trim().length < 2 ? 'Ingresa el nombre de la agencia' : '',
          urlSeguimiento:
            url && !PLANTILLA.test(url) ? 'La URL debe ser https e incluir {codigo}' : '',
        }
      : { nombre: '', urlSeguimiento: '' };
    return Object.fromEntries(
      Object.entries(cliente).map(([campo, mensaje]) => [campo, mensaje || servidor[campo] || '']),
    ) as typeof cliente;
  });

  protected guardar(evento: Event): void {
    evento.preventDefault();
    this.intentado.set(true);
    this.errorGeneral.set('');
    this.delServidor.set({});
    if (Object.values(this.errores()).some(Boolean) || this.enviando()) return;

    const pedido: deliveryApi.AgenciaRequest = {
      nombre: this.nombre().trim(),
      urlSeguimiento: this.url().trim() || null,
    };
    const contexto = { context: new HttpContext().set(SIN_TOAST, true) };
    const llamada = this.agencia
      ? this.api.editarAgencia(this.agencia.id, pedido, 'body', false, contexto)
      : this.api.crearAgencia(pedido, 'body', false, contexto);
    this.enviando.set(true);
    llamada.subscribe({
      next: (r) => {
        this.enviando.set(false);
        if (r.data) this.ref.close(r.data);
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        const cuerpo = comoApiError(error);
        const campos = camposConError(error);
        if (campos.length > 0) {
          this.delServidor.set(Object.fromEntries(campos.map((c) => [c.campo, c.mensaje])));
        } else if (cuerpo?.codigo === 'AGENCIA_DUPLICADA') {
          this.delServidor.set({ nombre: cuerpo.mensaje });
        } else {
          this.errorGeneral.set(cuerpo?.mensaje ?? 'No pudimos guardar. Inténtalo en un momento.');
        }
      },
    });
  }
}
