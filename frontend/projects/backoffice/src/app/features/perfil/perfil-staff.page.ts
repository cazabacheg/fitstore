import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { SIN_TOAST, ToastService, authApi, camposConError } from 'shared';
import { StaffPropioService } from 'shared/api/auth-service';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { EstadoCargando } from '../../ui/estados/estado-cargando';
import { EstadoError } from '../../ui/estados/estado-error';
import { CampoFormulario } from '../acceso/campo-formulario';
import { errorNombre, errorPassword } from '../acceso/validaciones';

const ROLES: Record<string, string> = { ADMIN: 'Admin', OPERADOR: 'Operador' };

/** Mi perfil de staff (P-1-10): edito mi nombre y cambio mi contraseña; el correo y el rol no son editables por mí (CA-1-35). */
@Component({
  selector: 'fs-perfil-staff-page',
  imports: [EncabezadoPagina, CampoFormulario, EstadoCargando, EstadoError],
  template: `
    <fs-encabezado-pagina titulo="Mi perfil" />
    @switch (estado()) {
      @case ('cargando') {
        <fs-estado-cargando [lineas]="4" />
      }
      @case ('error') {
        <fs-estado-error titulo="No pudimos cargar tu perfil" (reintentar)="cargar()" />
      }
      @default {
        <form
          novalidate
          (submit)="guardar($event)"
          style="display: grid; gap: var(--espacio-4); max-width: 520px; margin-top: var(--espacio-4)"
        >
          @if (errorGeneral()) {
            <div class="fs-aviso fs-aviso--error" role="alert">
              <span class="fs-aviso__texto">{{ errorGeneral() }}</span>
            </div>
          }
          <fs-campo-formulario etiqueta="Nombres" [(valor)]="nombres" [error]="errores().nombres" />
          <fs-campo-formulario
            etiqueta="Apellidos"
            [(valor)]="apellidos"
            [error]="errores().apellidos"
          />
          <fs-campo-formulario etiqueta="Correo" [valor]="correo()" [deshabilitado]="true" />
          <fs-campo-formulario etiqueta="Rol" [valor]="rol()" [deshabilitado]="true" />
          <div>
            <button type="submit" class="fs-btn fs-btn--primario" [disabled]="enviando()">
              Guardar cambios
            </button>
          </div>
        </form>
        <h2 style="margin: var(--espacio-8) 0 var(--espacio-4)">Cambiar contraseña</h2>
        <form
          novalidate
          (submit)="cambiarPassword($event)"
          style="display: grid; gap: var(--espacio-4); max-width: 520px"
        >
          @if (errorPassword()) {
            <div class="fs-aviso fs-aviso--error" role="alert">
              <span class="fs-aviso__texto">{{ errorPassword() }}</span>
            </div>
          }
          <fs-campo-formulario
            etiqueta="Contraseña actual"
            tipo="password"
            [(valor)]="actual"
            [error]="erroresClave().actual"
          />
          <fs-campo-formulario
            etiqueta="Nueva contraseña"
            tipo="password"
            [(valor)]="nueva"
            ayuda="Mínimo 8 caracteres, con al menos una letra y un número"
            [error]="erroresClave().nueva"
          />
          <fs-campo-formulario
            etiqueta="Repite la nueva contraseña"
            tipo="password"
            [(valor)]="repetida"
            [error]="erroresClave().repetida"
          />
          <div>
            <button type="submit" class="fs-btn fs-btn--contorno" [disabled]="cambiando()">
              Cambiar contraseña
            </button>
          </div>
        </form>
      }
    }
  `,
})
export class PerfilStaffPage {
  private readonly api = inject(StaffPropioService);
  private readonly toasts = inject(ToastService);

  protected readonly estado = signal<'cargando' | 'listo' | 'error'>('cargando');
  protected readonly nombres = signal('');
  protected readonly apellidos = signal('');
  protected readonly correo = signal('');
  protected readonly rol = signal('');
  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal('');
  private readonly intentado = signal(false);
  private readonly delServidor = signal<Record<string, string>>({});

  protected readonly actual = signal('');
  protected readonly nueva = signal('');
  protected readonly repetida = signal('');
  protected readonly cambiando = signal(false);
  protected readonly errorPassword = signal('');
  private readonly intentadoClave = signal(false);
  private readonly claveDelServidor = signal<Record<string, string>>({});

  protected readonly errores = computed(() => {
    const servidor = this.delServidor();
    const intentado = this.intentado();
    return {
      nombres:
        (intentado ? errorNombre(this.nombres(), 'tus nombres') : '') || servidor['nombres'] || '',
      apellidos:
        (intentado ? errorNombre(this.apellidos(), 'tus apellidos') : '') ||
        servidor['apellidos'] ||
        '',
    };
  });

  protected readonly erroresClave = computed(() => {
    const servidor = this.claveDelServidor();
    const intentado = this.intentadoClave();
    return {
      actual:
        (intentado && !this.actual() ? 'Ingresa tu contraseña actual' : '') ||
        servidor['passwordActual'] ||
        '',
      nueva: (intentado ? errorPassword(this.nueva()) : '') || servidor['nuevaPassword'] || '',
      repetida: intentado && this.repetida() !== this.nueva() ? 'Las contraseñas no coinciden' : '',
    };
  });

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.estado.set('cargando');
    this.api
      .obtenerMiPerfilStaff('body', false, { context: new HttpContext().set(SIN_TOAST, true) })
      .subscribe({
        next: (respuesta) => {
          if (!respuesta.data) {
            this.estado.set('error');
            return;
          }
          this.mostrar(respuesta.data);
          this.estado.set('listo');
        },
        error: () => this.estado.set('error'),
      });
  }

  protected guardar(evento: Event): void {
    evento.preventDefault();
    this.intentado.set(true);
    this.errorGeneral.set('');
    this.delServidor.set({});
    if (Object.values(this.errores()).some(Boolean) || this.enviando()) return;

    this.enviando.set(true);
    this.api
      .actualizarMiPerfilStaff(
        { nombres: this.nombres().trim(), apellidos: this.apellidos().trim() },
        'body',
        false,
        { context: new HttpContext().set(SIN_TOAST, true) },
      )
      .subscribe({
        next: (respuesta) => {
          this.enviando.set(false);
          if (respuesta.data) this.mostrar(respuesta.data);
          this.toasts.mostrar('exito', 'Guardado');
        },
        error: (error: unknown) => {
          this.enviando.set(false);
          const campos = camposConError(error);
          if (campos.length > 0) {
            this.delServidor.set(Object.fromEntries(campos.map((c) => [c.campo, c.mensaje])));
          } else {
            this.errorGeneral.set('No pudimos guardar tus cambios. Inténtalo en un momento.');
          }
        },
      });
  }

  protected cambiarPassword(evento: Event): void {
    evento.preventDefault();
    this.intentadoClave.set(true);
    this.errorPassword.set('');
    this.claveDelServidor.set({});
    if (Object.values(this.erroresClave()).some(Boolean) || this.cambiando()) return;

    this.cambiando.set(true);
    this.api
      .cambiarMiPasswordStaff(
        { passwordActual: this.actual(), nuevaPassword: this.nueva() },
        'body',
        false,
        { context: new HttpContext().set(SIN_TOAST, true) },
      )
      .subscribe({
        next: () => {
          this.cambiando.set(false);
          this.actual.set('');
          this.nueva.set('');
          this.repetida.set('');
          this.intentadoClave.set(false);
          this.toasts.mostrar('exito', 'Contraseña actualizada');
        },
        error: (error: unknown) => {
          this.cambiando.set(false);
          const campos = camposConError(error);
          if (campos.length > 0) {
            this.claveDelServidor.set(Object.fromEntries(campos.map((c) => [c.campo, c.mensaje])));
          } else {
            this.errorPassword.set('No pudimos cambiar tu contraseña. Inténtalo en un momento.');
          }
        },
      });
  }

  private mostrar(perfil: authApi.Staff): void {
    this.nombres.set(perfil.nombres);
    this.apellidos.set(perfil.apellidos);
    this.correo.set(perfil.email);
    this.rol.set(ROLES[perfil.rol] ?? perfil.rol);
  }
}
