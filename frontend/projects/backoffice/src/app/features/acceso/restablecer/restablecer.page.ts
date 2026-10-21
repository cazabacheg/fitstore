import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { SIN_TOAST, camposConError, comoApiError } from 'shared';
import { AutenticacionService } from 'shared/api/auth-service';
import { CampoFormulario } from '../campo-formulario';
import { errorPassword } from '../validaciones';

/**
 * Restablecer o crear contraseña del staff (P-1-04): `/restablecer?token=…`. Si el enlace es de invitación
 * (`&invitacion=1`, A3) el título es "Bienvenido al equipo" (CA-1-26); si no, es una recuperación (CA-1-13 y CA-1-14).
 */
@Component({
  selector: 'fs-restablecer-page',
  imports: [CampoFormulario, RouterLink],
  template: `
    <main class="acceso">
      <section class="acceso__tarjeta">
        <p class="acceso__marca">FitStore</p>
        @switch (estado()) {
          @case ('listo') {
            <h1 style="margin: 0 0 var(--espacio-4)">Tu contraseña está lista</h1>
            <p class="fs-suave">Ya puedes ingresar al backoffice con tu nueva contraseña.</p>
            <a class="fs-btn fs-btn--primario" routerLink="/ingresar">Ingresar</a>
          }
          @case ('invalido') {
            <h1 style="margin: 0 0 var(--espacio-4)">Enlace inválido o expirado</h1>
            <p class="fs-suave">El enlace ya se usó, venció o fue reemplazado por uno más nuevo.</p>
            <a class="fs-btn fs-btn--primario" routerLink="/recuperar">Pedir otro enlace</a>
          }
          @default {
            <h1 style="margin: 0 0 var(--espacio-5)">
              {{ invitacion ? 'Bienvenido al equipo' : 'Crea tu nueva contraseña' }}
            </h1>
            @if (invitacion) {
              <p class="fs-suave" style="margin-top: 0">
                Crea tu contraseña para activar tu cuenta.
              </p>
            }
            <form novalidate (submit)="enviar($event)" style="display: grid; gap: var(--espacio-4)">
              @if (errorGeneral()) {
                <div class="fs-aviso fs-aviso--error" role="alert">
                  <span class="fs-aviso__texto">{{ errorGeneral() }}</span>
                </div>
              }
              <fs-campo-formulario
                etiqueta="Nueva contraseña"
                tipo="password"
                [(valor)]="password"
                ayuda="Mínimo 8 caracteres, con al menos una letra y un número"
                [error]="errores().password"
              />
              <fs-campo-formulario
                etiqueta="Repite la contraseña"
                tipo="password"
                [(valor)]="repetida"
                [error]="errores().repetida"
              />
              <button
                type="submit"
                class="fs-btn fs-btn--primario fs-btn--bloque"
                [disabled]="enviando()"
              >
                Guardar contraseña
              </button>
            </form>
          }
        }
      </section>
    </main>
  `,
  styles: `
    .acceso {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: var(--espacio-4);
      background: var(--color-fondo);
    }
    .acceso__tarjeta {
      width: 100%;
      max-width: 420px;
      padding: var(--espacio-6);
      border: 1px solid var(--color-linea);
      border-radius: var(--radius-lg);
      background: var(--color-superficie);
    }
    .acceso__marca {
      margin: 0 0 var(--espacio-2);
      font-weight: 700;
      color: var(--color-primario-texto);
    }
  `,
})
export class RestablecerPage {
  private readonly api = inject(AutenticacionService);
  private readonly parametros = inject(ActivatedRoute).snapshot.queryParamMap;
  private readonly token = this.parametros.get('token');

  protected readonly invitacion = this.parametros.get('invitacion') === '1';
  protected readonly estado = signal<'formulario' | 'listo' | 'invalido'>(
    this.token ? 'formulario' : 'invalido',
  );
  protected readonly password = signal('');
  protected readonly repetida = signal('');
  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal('');
  private readonly intentado = signal(false);
  private readonly delServidor = signal('');

  protected readonly errores = computed(() => ({
    password: this.delServidor() || (this.intentado() ? errorPassword(this.password()) : ''),
    repetida:
      this.intentado() && this.repetida() !== this.password() ? 'Las contraseñas no coinciden' : '',
  }));

  protected enviar(evento: Event): void {
    evento.preventDefault();
    this.intentado.set(true);
    this.errorGeneral.set('');
    this.delServidor.set('');
    const { password, repetida } = this.errores();
    if (!this.token || password || repetida || this.enviando()) return;

    this.enviando.set(true);
    this.api
      .restablecerPassword({ token: this.token, nuevaPassword: this.password() }, 'body', false, {
        context: new HttpContext().set(SIN_TOAST, true),
      })
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.estado.set('listo');
        },
        error: (error: unknown) => {
          this.enviando.set(false);
          if (comoApiError(error)?.codigo === 'TOKEN_INVALIDO') {
            this.estado.set('invalido');
            return;
          }
          const campos = camposConError(error);
          if (campos.length > 0) this.delServidor.set(campos[0].mensaje);
          else this.errorGeneral.set('No pudimos guardar tu contraseña. Inténtalo en un momento.');
        },
      });
  }
}
