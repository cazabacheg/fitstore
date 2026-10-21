import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { SIN_TOAST, ToastService, authApi, camposConError } from 'shared';
import { AutenticacionService, ClienteService } from 'shared/api/auth-service';
import { FsBoton } from '../../../ui/boton/boton';
import { FsCampo } from '../../../ui/campo/campo';
import { EstadoCargando, EstadoError } from '../../../ui/estados/estados';
import { errorCelular, errorNombre } from '../../acceso/validaciones';
import { CambioPassword } from './cambio-password';
import { ESTILOS_PANTALLA } from '../../estilos-pantalla';

/** Mi perfil (P-1-05): edita nombres, apellidos y celular; el correo y el DNI se ven pero no se editan (CA-1-15). */
@Component({
  selector: 'fs-perfil-page',
  imports: [FsCampo, FsBoton, EstadoCargando, EstadoError, CambioPassword],
  styles: [ESTILOS_PANTALLA],
  template: `
    <h1 class="fs-titulo-pagina" style="margin-bottom: var(--espacio-6)">Mi perfil</h1>
    @switch (estado()) {
      @case ('cargando') {
        <fs-estado-cargando [filas]="4" />
      }
      @case ('error') {
        <fs-estado-error titulo="No pudimos cargar tu perfil" (reintentar)="cargar()" />
      }
      @default {
        <form
          novalidate
          (submit)="guardar($event)"
          style="display: grid; gap: var(--espacio-4); max-width: 520px"
        >
          @if (errorGeneral()) {
            <div class="fs-aviso fs-aviso--error" role="alert">
              <span class="fs-aviso__texto">{{ errorGeneral() }}</span>
            </div>
          }
          <fs-campo etiqueta="Nombres" [(valor)]="nombres" [error]="errores().nombres" />
          <fs-campo etiqueta="Apellidos" [(valor)]="apellidos" [error]="errores().apellidos" />
          <fs-campo
            etiqueta="Celular"
            tipo="tel"
            [(valor)]="telefono"
            [error]="errores().telefono"
          />
          <fs-campo etiqueta="Correo" [valor]="correo()" [deshabilitado]="true" />
          <fs-campo etiqueta="DNI" [valor]="dni()" [deshabilitado]="true" />
          <div>
            <button type="submit" fsBoton variante="primario" [disabled]="enviando()">
              Guardar cambios
            </button>
          </div>
        </form>
        <h2 class="fs-titulo-seccion" style="margin: var(--espacio-8) 0 var(--espacio-4)">Cambiar contraseña</h2>
        <fs-cambio-password />
      }
    }
  `,
})
export class PerfilPage {
  private readonly api = inject(AutenticacionService);
  private readonly clientes = inject(ClienteService);
  private readonly toasts = inject(ToastService);

  protected readonly estado = signal<'cargando' | 'listo' | 'error'>('cargando');
  protected readonly nombres = signal('');
  protected readonly apellidos = signal('');
  protected readonly telefono = signal('');
  protected readonly correo = signal('');
  protected readonly dni = signal('');
  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal('');
  private readonly intentado = signal(false);
  private readonly delServidor = signal<Record<string, string>>({});

  protected readonly errores = computed(() => {
    const servidor = this.delServidor();
    const cliente = this.intentado()
      ? {
          nombres: errorNombre(this.nombres(), 'nombres'),
          apellidos: errorNombre(this.apellidos(), 'apellidos'),
          telefono: errorCelular(this.telefono()),
        }
      : { nombres: '', apellidos: '', telefono: '' };
    return {
      nombres: cliente.nombres || servidor['nombres'] || '',
      apellidos: cliente.apellidos || servidor['apellidos'] || '',
      telefono: cliente.telefono || servidor['telefono'] || '',
    };
  });

  constructor() {
    void this.api;
    this.cargar();
  }

  protected cargar(): void {
    this.estado.set('cargando');
    this.clientes
      .obtenerMiPerfilCliente('body', false, { context: new HttpContext().set(SIN_TOAST, true) })
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
    this.clientes
      .actualizarMiPerfilCliente(
        {
          nombres: this.nombres().trim(),
          apellidos: this.apellidos().trim(),
          telefono: this.telefono().trim(),
        },
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

  private mostrar(perfil: authApi.PerfilCliente): void {
    this.nombres.set(perfil.nombres);
    this.apellidos.set(perfil.apellidos);
    this.telefono.set(perfil.telefono);
    this.correo.set(perfil.email);
    this.dni.set(perfil.dni);
  }
}
