import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SIN_TOAST, SesionStore, authApi, comoApiError } from 'shared';
import { CampoFormulario } from '../campo-formulario';
import { errorCorreo, rutaDeRetorno } from '../validaciones';

/** Ingresar al backoffice (P-1-07), fuera del shell: `POST /auth/staff/login` y vuelta a `returnUrl` (CA-1-22 a CA-1-24). */
@Component({
  selector: 'fs-ingresar-staff-page',
  imports: [CampoFormulario, RouterLink],
  template: `
    <main class="acceso">
      <section class="acceso__tarjeta">
        <p class="acceso__marca">FitStore</p>
        <h1 style="margin: 0 0 var(--espacio-5)">Ingresa al backoffice</h1>
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
            [error]="errores().email"
          />
          <fs-campo-formulario
            etiqueta="Contraseña"
            tipo="password"
            [(valor)]="password"
            [error]="errores().password"
          />
          <button
            type="submit"
            class="fs-btn fs-btn--primario fs-btn--bloque"
            [disabled]="enviando()"
          >
            Ingresar
          </button>
        </form>
        <p style="margin: var(--espacio-4) 0 0">
          <a routerLink="/recuperar">¿Olvidaste tu contraseña?</a>
        </p>
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
export class IngresarStaffPage {
  private readonly api = inject(authApi.AutenticacionService);
  private readonly sesion = inject(SesionStore);
  private readonly router = inject(Router);
  private readonly returnUrl = inject(ActivatedRoute).snapshot.queryParamMap.get('returnUrl');

  protected readonly email = signal('');
  protected readonly password = signal('');
  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal('');
  private readonly intentado = signal(false);

  protected readonly errores = computed(() =>
    this.intentado()
      ? {
          email: errorCorreo(this.email()),
          password: this.password() ? '' : 'Ingresa tu contraseña',
        }
      : { email: '', password: '' },
  );

  protected enviar(evento: Event): void {
    evento.preventDefault();
    this.intentado.set(true);
    this.errorGeneral.set('');
    const { email, password } = this.errores();
    if (email || password || this.enviando()) return;

    this.enviando.set(true);
    this.api
      .loginStaff({ email: this.email().trim(), password: this.password() }, 'body', false, {
        context: new HttpContext().set(SIN_TOAST, true),
      })
      .subscribe({
        next: (respuesta) => {
          this.enviando.set(false);
          if (!respuesta.data) return;
          this.sesion.iniciarSesion(respuesta.data);
          void this.router.navigateByUrl(rutaDeRetorno(this.returnUrl));
        },
        error: (error: unknown) => {
          this.enviando.set(false);
          this.errorGeneral.set(this.mensajeDe(error));
        },
      });
  }

  private mensajeDe(error: unknown): string {
    switch (comoApiError(error)?.codigo) {
      case 'CREDENCIALES_INVALIDAS':
        return 'Correo o contraseña incorrectos';
      case 'CUENTA_BLOQUEADA':
        return 'Cuenta bloqueada temporalmente. Inténtalo de nuevo en 15 minutos.';
      default:
        return 'No pudimos ingresar. Inténtalo en un momento.';
    }
  }
}
