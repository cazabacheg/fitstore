import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { SIN_TOAST, authApi, camposConError, comoApiError } from 'shared';
import { CampoFormulario } from '../acceso/campo-formulario';
import { errorCorreo, errorNombre } from '../acceso/validaciones';

export interface DatosDialogoStaff {
  /** Miembro que se edita; sin él, se invita a uno nuevo. */
  staff?: authApi.Staff;
}

/** Invitar o editar a un miembro del staff (P-1-08). Se crea sin contraseña y recibe un enlace por correo (D4). */
@Component({
  selector: 'fs-dialogo-staff',
  imports: [MatDialogModule, CampoFormulario],
  template: `
    <h2 mat-dialog-title>{{ staff ? 'Editar miembro' : 'Invitar miembro' }}</h2>
    <mat-dialog-content>
      <form
        id="form-staff"
        novalidate
        (submit)="guardar($event)"
        style="display: grid; gap: var(--espacio-4); padding-top: var(--espacio-2)"
      >
        @if (errorGeneral()) {
          <div class="fs-aviso fs-aviso--error" role="alert">
            <span class="fs-aviso__texto">{{ errorGeneral() }}</span>
          </div>
        }
        <fs-campo-formulario
          etiqueta="Correo"
          tipo="email"
          [(valor)]="email"
          [deshabilitado]="!!staff"
          [error]="errores().email"
        />
        <fs-campo-formulario etiqueta="Nombres" [(valor)]="nombres" [error]="errores().nombres" />
        <fs-campo-formulario
          etiqueta="Apellidos"
          [(valor)]="apellidos"
          [error]="errores().apellidos"
        />
        <div class="fs-campo" [class.fs-campo--error]="!!errores().rol">
          <label class="fs-campo__etiqueta" for="rol-staff">Rol</label>
          <select id="rol-staff" class="fs-select" (change)="rol.set(elegido($event))">
            <option value="" [selected]="!rol()">Elige un rol</option>
            <option value="OPERADOR" [selected]="rol() === 'OPERADOR'">Operador</option>
            <option value="ADMIN" [selected]="rol() === 'ADMIN'">Admin</option>
          </select>
          @if (errores().rol) {
            <span class="fs-campo__error">{{ errores().rol }}</span>
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
        form="form-staff"
        class="fs-btn fs-btn--primario"
        [disabled]="enviando()"
      >
        {{ staff ? 'Guardar' : 'Enviar invitación' }}
      </button>
    </mat-dialog-actions>
  `,
})
export class DialogoStaff {
  private readonly api = inject(authApi.StaffService);
  private readonly ref =
    inject<MatDialogRef<DialogoStaff, authApi.Staff | undefined>>(MatDialogRef);
  protected readonly staff = inject<DatosDialogoStaff>(MAT_DIALOG_DATA).staff;

  protected readonly email = signal(this.staff?.email ?? '');
  protected readonly nombres = signal(this.staff?.nombres ?? '');
  protected readonly apellidos = signal(this.staff?.apellidos ?? '');
  protected readonly rol = signal<string>(this.staff?.rol ?? '');
  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal('');
  private readonly intentado = signal(false);
  private readonly delServidor = signal<Record<string, string>>({});

  protected readonly errores = computed(() => {
    const servidor = this.delServidor();
    const intentado = this.intentado();
    const cliente = intentado
      ? {
          email: this.staff ? '' : errorCorreo(this.email()),
          nombres: errorNombre(this.nombres(), 'el nombre'),
          apellidos: errorNombre(this.apellidos(), 'el apellido'),
          rol: this.rol() ? '' : 'Elige un rol',
        }
      : { email: '', nombres: '', apellidos: '', rol: '' };
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

    const rol = this.rol() as authApi.RolStaff;
    const contexto = { context: new HttpContext().set(SIN_TOAST, true) };
    const llamada = this.staff
      ? this.api.editarStaff(
          this.staff.id,
          {
            nombres: this.nombres().trim(),
            apellidos: this.apellidos().trim(),
            rol,
            version: this.staff.version,
          },
          'body',
          false,
          contexto,
        )
      : this.api.invitarStaff(
          {
            email: this.email().trim(),
            nombres: this.nombres().trim(),
            apellidos: this.apellidos().trim(),
            rol,
          },
          'body',
          false,
          contexto,
        );

    this.enviando.set(true);
    llamada.subscribe({
      next: (respuesta) => {
        this.enviando.set(false);
        if (respuesta.data) this.ref.close(respuesta.data);
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        this.mostrarError(error);
      },
    });
  }

  private mostrarError(error: unknown): void {
    const cuerpo = comoApiError(error);
    const campos = camposConError(error);
    if (cuerpo?.codigo === 'CORREO_DUPLICADO') {
      this.delServidor.set({ email: cuerpo.mensaje });
    } else if (campos.length > 0) {
      this.delServidor.set(Object.fromEntries(campos.map((c) => [c.campo, c.mensaje])));
    } else {
      this.errorGeneral.set(cuerpo?.mensaje ?? 'No pudimos guardar. Inténtalo en un momento.');
    }
  }
}
