import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { SIN_TOAST, ToastService, camposConError } from 'shared';
import { ClienteService } from 'shared/api/auth-service';
import { FsBoton } from '../../../ui/boton/boton';
import { FsCampo } from '../../../ui/campo/campo';
import { errorPassword } from '../../acceso/validaciones';

/** Cambio de contraseña (P-1-05): pide la actual; las otras sesiones se cierran (CA-1-16). */
@Component({
  selector: 'fs-cambio-password',
  imports: [FsCampo, FsBoton],
  template: `
    <form
      novalidate
      (submit)="enviar($event)"
      style="display: grid; gap: var(--espacio-4); max-width: 520px"
    >
      @if (errorGeneral()) {
        <div class="fs-aviso fs-aviso--error" role="alert">
          <span class="fs-aviso__texto">{{ errorGeneral() }}</span>
        </div>
      }
      <fs-campo
        etiqueta="Contraseña actual"
        tipo="password"
        [(valor)]="actual"
        [error]="errores().actual"
      />
      <fs-campo
        etiqueta="Nueva contraseña"
        tipo="password"
        [(valor)]="nueva"
        ayuda="Mínimo 8 caracteres, con al menos una letra y un número"
        [error]="errores().nueva"
      />
      <fs-campo
        etiqueta="Repite la nueva contraseña"
        tipo="password"
        [(valor)]="repetida"
        [error]="errores().repetida"
      />
      <div>
        <button type="submit" fsBoton variante="contorno" [disabled]="enviando()">
          Cambiar contraseña
        </button>
      </div>
    </form>
  `,
})
export class CambioPassword {
  private readonly clientes = inject(ClienteService);
  private readonly toasts = inject(ToastService);

  protected readonly actual = signal('');
  protected readonly nueva = signal('');
  protected readonly repetida = signal('');
  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal('');
  private readonly intentado = signal(false);
  private readonly delServidor = signal<Record<string, string>>({});

  protected readonly errores = computed(() => {
    const servidor = this.delServidor();
    const intentado = this.intentado();
    return {
      actual:
        (intentado && !this.actual() ? 'Ingresa tu contraseña actual' : '') ||
        servidor['passwordActual'] ||
        '',
      nueva: (intentado ? errorPassword(this.nueva()) : '') || servidor['nuevaPassword'] || '',
      repetida: intentado && this.repetida() !== this.nueva() ? 'Las contraseñas no coinciden' : '',
    };
  });

  protected enviar(evento: Event): void {
    evento.preventDefault();
    this.intentado.set(true);
    this.errorGeneral.set('');
    this.delServidor.set({});
    if (Object.values(this.errores()).some(Boolean) || this.enviando()) return;

    this.enviando.set(true);
    this.clientes
      .cambiarMiPasswordCliente(
        { passwordActual: this.actual(), nuevaPassword: this.nueva() },
        'body',
        false,
        { context: new HttpContext().set(SIN_TOAST, true) },
      )
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.actual.set('');
          this.nueva.set('');
          this.repetida.set('');
          this.intentado.set(false);
          this.toasts.mostrar('exito', 'Contraseña actualizada');
        },
        error: (error: unknown) => {
          this.enviando.set(false);
          const campos = camposConError(error);
          if (campos.length > 0) {
            this.delServidor.set(Object.fromEntries(campos.map((c) => [c.campo, c.mensaje])));
          } else {
            this.errorGeneral.set('No pudimos cambiar tu contraseña. Inténtalo en un momento.');
          }
        },
      });
  }
}
