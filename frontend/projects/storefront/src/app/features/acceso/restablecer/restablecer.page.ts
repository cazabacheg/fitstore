import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { SIN_TOAST, authApi, camposConError, comoApiError } from 'shared';
import { FsBoton } from '../../../ui/boton/boton';
import { FsCampo } from '../../../ui/campo/campo';
import { errorPassword } from '../validaciones';

/** Restablecer contraseña (P-1-04): `/restablecer?token=…` con el token del correo (CA-1-13 y CA-1-14). */
@Component({
  selector: 'fs-restablecer-page',
  imports: [FsCampo, FsBoton, RouterLink],
  template: `
    <section class="fs-pagina" style="padding-block: 48px; max-width: 440px">
      @switch (estado()) {
        @case ('listo') {
          <h1 style="margin: 0 0 var(--espacio-4)">Tu contraseña se actualizó</h1>
          <p class="fs-suave">Ya puedes ingresar con tu nueva contraseña.</p>
          <a fsBoton variante="primario" routerLink="/ingresar">Ingresar</a>
        }
        @case ('invalido') {
          <h1 style="margin: 0 0 var(--espacio-4)">Enlace inválido o expirado</h1>
          <p class="fs-suave">El enlace ya se usó, venció o fue reemplazado por uno más nuevo.</p>
          <a fsBoton variante="primario" routerLink="/recuperar">Pedir otro enlace</a>
        }
        @default {
          <h1 style="margin: 0 0 var(--espacio-5)">Crea tu nueva contraseña</h1>
          <form novalidate (submit)="enviar($event)" style="display: grid; gap: var(--espacio-4)">
            @if (errorGeneral()) {
              <div class="fs-aviso fs-aviso--error" role="alert">
                <span class="fs-aviso__texto">{{ errorGeneral() }}</span>
              </div>
            }
            <fs-campo
              etiqueta="Nueva contraseña"
              tipo="password"
              [(valor)]="password"
              ayuda="Mínimo 8 caracteres, con al menos una letra y un número"
              [error]="errores().password"
            />
            <fs-campo
              etiqueta="Repite la contraseña"
              tipo="password"
              [(valor)]="repetida"
              [error]="errores().repetida"
            />
            <button
              type="submit"
              fsBoton
              variante="primario"
              [bloque]="true"
              [disabled]="enviando()"
            >
              Guardar contraseña
            </button>
          </form>
        }
      }
    </section>
  `,
})
export class RestablecerPage {
  private readonly api = inject(authApi.AutenticacionService);
  private readonly token = inject(ActivatedRoute).snapshot.queryParamMap.get('token');

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
