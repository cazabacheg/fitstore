import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SIN_TOAST } from 'shared';
import { AutenticacionService } from 'shared/api/auth-service';
import { FsBoton } from '../../../ui/boton/boton';
import { FsCampo } from '../../../ui/campo/campo';
import { errorCorreo } from '../validaciones';
import { ESTILOS_PANTALLA } from '../../estilos-pantalla';

const MENSAJE = 'Si el correo está registrado, te enviaremos un enlace';

/** Recuperar contraseña (P-1-03): `POST /auth/password/olvido`, que responde siempre lo mismo (CA-1-12). */
@Component({
  selector: 'fs-recuperar-page',
  imports: [FsCampo, FsBoton, RouterLink],
  styles: [
    ESTILOS_PANTALLA,
    `
      .acceso {
        padding-block: 48px 80px;
        max-width: 440px;
      }
      .acceso__tarjeta h1.fs-titulo-pagina {
        margin-bottom: 20px;
      }
      @media (max-width: 720px) {
        .acceso {
          padding-block: 24px 48px;
        }
      }
    `,
  ],
  template: `
    <section class="fs-pagina acceso">
      <div class="fs-tarjeta acceso__tarjeta">
      <h1 class="fs-titulo-pagina">Recupera tu contraseña</h1>
      @if (enviado()) {
        <div class="fs-aviso fs-aviso--exito" role="status">
          <span class="fs-aviso__texto">{{ mensaje }}</span>
        </div>
        <p style="margin: var(--espacio-4) 0 0"><a routerLink="/ingresar">Volver a ingresar</a></p>
      } @else {
        <p class="fs-suave" style="margin-top: 0">
          Escribe el correo de tu cuenta y te enviaremos un enlace para crear una nueva contraseña.
        </p>
        <form novalidate (submit)="enviar($event)" style="display: grid; gap: var(--espacio-4)">
          @if (errorGeneral()) {
            <div class="fs-aviso fs-aviso--error" role="alert">
              <span class="fs-aviso__texto">{{ errorGeneral() }}</span>
            </div>
          }
          <fs-campo etiqueta="Correo" tipo="email" [(valor)]="email" [error]="error()" />
          <button type="submit" fsBoton variante="primario" [bloque]="true" [disabled]="enviando()">
            Enviar enlace
          </button>
        </form>
        <p style="margin: var(--espacio-4) 0 0"><a routerLink="/ingresar">Volver a ingresar</a></p>
      }
      </div>
    </section>
  `,
})
export class RecuperarPage {
  private readonly api = inject(AutenticacionService);

  protected readonly mensaje = MENSAJE;
  protected readonly email = signal('');
  protected readonly enviando = signal(false);
  protected readonly enviado = signal(false);
  protected readonly errorGeneral = signal('');
  private readonly intentado = signal(false);
  protected readonly error = computed(() => (this.intentado() ? errorCorreo(this.email()) : ''));

  protected enviar(evento: Event): void {
    evento.preventDefault();
    this.intentado.set(true);
    this.errorGeneral.set('');
    if (errorCorreo(this.email()) || this.enviando()) return;

    this.enviando.set(true);
    this.api
      .olvidoPasswordCliente({ email: this.email().trim() }, 'body', false, {
        context: new HttpContext().set(SIN_TOAST, true),
      })
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.enviado.set(true);
        },
        error: () => {
          this.enviando.set(false);
          this.errorGeneral.set('No pudimos enviar el enlace. Inténtalo en un momento.');
        },
      });
  }
}
