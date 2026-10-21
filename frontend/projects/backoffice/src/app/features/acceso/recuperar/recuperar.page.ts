import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SIN_TOAST, authApi } from 'shared';
import { CampoFormulario } from '../campo-formulario';
import { errorCorreo } from '../validaciones';

const MENSAJE = 'Si el correo está registrado, te enviaremos un enlace';

/** Recuperar contraseña del staff (P-1-03): `POST /auth/staff/password/olvido`, siempre la misma respuesta (CA-1-12). */
@Component({
  selector: 'fs-recuperar-page',
  imports: [CampoFormulario, RouterLink],
  template: `
    <main class="acceso">
      <section class="acceso__tarjeta">
        <p class="acceso__marca">FitStore</p>
        <h1 style="margin: 0 0 var(--espacio-5)">Recupera tu contraseña</h1>
        @if (enviado()) {
          <div class="fs-aviso fs-aviso--exito" role="status">
            <span class="fs-aviso__texto">{{ mensaje }}</span>
          </div>
        } @else {
          <form novalidate (submit)="enviar($event)" style="display: grid; gap: var(--espacio-4)">
            @if (errorGeneral()) {
              <div class="fs-aviso fs-aviso--error" role="alert">
                <span class="fs-aviso__texto">{{ errorGeneral() }}</span>
              </div>
            }
            <fs-campo-formulario
              etiqueta="Correo"
              tipo="email"
              [(valor)]="email"
              [error]="error()"
            />
            <button
              type="submit"
              class="fs-btn fs-btn--primario fs-btn--bloque"
              [disabled]="enviando()"
            >
              Enviar enlace
            </button>
          </form>
        }
        <p style="margin: var(--espacio-4) 0 0"><a routerLink="/ingresar">Volver a ingresar</a></p>
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
export class RecuperarPage {
  private readonly api = inject(authApi.AutenticacionService);

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
      .olvidoPasswordStaff({ email: this.email().trim() }, 'body', false, {
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
