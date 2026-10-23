import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { SIN_TOAST, SesionStore, comoApiError } from 'shared';
import { AutenticacionService } from 'shared/api/auth-service';
import { FsBoton } from '../../../ui/boton/boton';
import { FsCampo } from '../../../ui/campo/campo';
import { CarritoService } from '../../carrito/carrito.service';
import { errorCorreo, rutaDeRetorno } from '../validaciones';

/** Formulario de ingreso del cliente (P-1-01): `POST /auth/login`, sesión en memoria y vuelta a `returnUrl` (CA-1-04). */
@Component({
  selector: 'fs-formulario-ingreso',
  imports: [FsCampo, FsBoton],
  template: `
    <form novalidate (submit)="enviar($event)" style="display: grid; gap: var(--espacio-4)">
      @if (errorGeneral()) {
        <div class="fs-aviso fs-aviso--error" role="alert">
          <span class="fs-aviso__texto">{{ errorGeneral() }}</span>
        </div>
      }
      <fs-campo etiqueta="Correo" tipo="email" [(valor)]="email" [error]="errores().email" />
      <fs-campo
        etiqueta="Contraseña"
        tipo="password"
        [(valor)]="password"
        [error]="errores().password"
      />
      <button type="submit" fsBoton variante="primario" [bloque]="true" [disabled]="enviando()">
        Ingresar
      </button>
    </form>
  `,
})
export class FormularioIngreso {
  private readonly api = inject(AutenticacionService);
  private readonly sesion = inject(SesionStore);
  private readonly router = inject(Router);
  private readonly carritos = inject(CarritoService);

  /** Ruta de origen que puso el guard (`?returnUrl=`). */
  readonly returnUrl = input<string | null>(null);

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
      .loginCliente({ email: this.email().trim(), password: this.password() }, 'body', false, {
        context: new HttpContext().set(SIN_TOAST, true),
      })
      .subscribe({
        next: (respuesta) => {
          this.enviando.set(false);
          if (!respuesta.data) return;
          this.sesion.iniciarSesion(respuesta.data);
          // CA-4-12: el carrito anónimo se fusiona antes de volver, para que el checkout ya lo vea
          this.carritos.alIniciarSesion().subscribe(() => {
            void this.router.navigateByUrl(rutaDeRetorno(this.returnUrl()));
          });
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
