import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { SIN_TOAST, SesionStore, camposConError, comoApiError, settingsApi } from 'shared';
import { AutenticacionService } from 'shared/api/auth-service';
import { ConfiguracionPublicaService } from 'shared/api/settings-service';
import { FsBoton } from '../../../ui/boton/boton';
import { FsCampo } from '../../../ui/campo/campo';
import { EstadoError } from '../../../ui/estados/estados';
import {
  errorCelular,
  errorCorreo,
  errorDni,
  errorNombre,
  errorPassword,
  rutaDeRetorno,
} from '../validaciones';
import { CarritoService } from '../../carrito/carrito.service';
import { IndicadorFortaleza } from './indicador-fortaleza';

type Politicas = Pick<settingsApi.VersionesPoliticas, 'terminos' | 'privacidad'>;

/**
 * Formulario de registro (P-1-02). Valida en el cliente lo mismo que auth (CA-1-03), envía las versiones de las
 * políticas que mostró `/settings/public` (CA-1-01, no se puede guardar un consentimiento sin versión) y muestra los
 * duplicados junto a su campo (CA-1-02).
 */
@Component({
  selector: 'fs-formulario-registro',
  imports: [FsCampo, FsBoton, EstadoError, IndicadorFortaleza],
  template: `
    @if (estadoPoliticas() === 'error') {
      <fs-estado-error
        titulo="No pudimos cargar las políticas"
        texto="Necesitamos las versiones vigentes para registrar tu aceptación."
        (reintentar)="cargarPoliticas()"
      />
    }
    <form novalidate (submit)="enviar($event)" style="display: grid; gap: var(--espacio-4)">
      @if (errorGeneral()) {
        <div class="fs-aviso fs-aviso--error" role="alert">
          <span class="fs-aviso__texto">{{ errorGeneral() }}</span>
        </div>
      }
      <div class="fila-doble">
        <fs-campo
          etiqueta="Nombres"
          placeholder="Ana"
          [(valor)]="nombres"
          [error]="errores().nombres"
        />
        <fs-campo
          etiqueta="Apellidos"
          placeholder="Pérez Ríos"
          [(valor)]="apellidos"
          [error]="errores().apellidos"
        />
      </div>
      <div class="fila-doble">
        <fs-campo etiqueta="DNI" placeholder="12345678" [(valor)]="dni" [error]="errores().dni" />
        <fs-campo
          etiqueta="Celular"
          tipo="tel"
          placeholder="987 654 321"
          [(valor)]="telefono"
          [error]="errores().telefono"
        />
      </div>
      <fs-campo
        etiqueta="Correo"
        tipo="email"
        placeholder="ana@correo.com"
        [(valor)]="email"
        [error]="errores().email"
      />
      <div>
        <fs-campo
          etiqueta="Contraseña"
          tipo="password"
          [(valor)]="password"
          ayuda="Mínimo 8 caracteres, con al menos una letra y un número"
          [error]="errores().password"
        />
        <fs-indicador-fortaleza [password]="password()" />
      </div>
      <div style="display: grid; gap: var(--espacio-2)">
        <label class="fs-check">
          <input
            type="checkbox"
            [checked]="aceptaTerminos()"
            (change)="aceptaTerminos.set(marcada($event))"
          />
          <span>
            Acepto los
            <a href="/politicas/terminos" target="_blank" rel="noopener">Términos y condiciones</a>
            @if (politicas(); as p) {
              <span class="fs-suave">(v{{ p.terminos }})</span>
            }
          </span>
        </label>
        @if (errores().aceptaTerminos) {
          <span class="fs-campo__error">{{ errores().aceptaTerminos }}</span>
        }
        <label class="fs-check">
          <input
            type="checkbox"
            [checked]="aceptaPrivacidad()"
            (change)="aceptaPrivacidad.set(marcada($event))"
          />
          <span>
            Acepto la
            <a href="/politicas/privacidad" target="_blank" rel="noopener"
              >Política de privacidad</a
            >
            y el tratamiento de mis datos personales (Ley 29733)
            @if (politicas(); as p) {
              <span class="fs-suave">(v{{ p.privacidad }})</span>
            }
          </span>
        </label>
        @if (errores().aceptaPrivacidad) {
          <span class="fs-campo__error">{{ errores().aceptaPrivacidad }}</span>
        }
      </div>
      <button
        type="submit"
        fsBoton
        variante="primario"
        [bloque]="true"
        [disabled]="enviando() || estadoPoliticas() !== 'listo'"
      >
        Crear cuenta
      </button>
    </form>
  `,
  styles: `
    .fila-doble {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: var(--espacio-4);
    }
  `,
})
export class FormularioRegistro {
  private readonly auth = inject(AutenticacionService);
  private readonly publica = inject(ConfiguracionPublicaService);
  private readonly sesion = inject(SesionStore);
  private readonly router = inject(Router);
  private readonly carritos = inject(CarritoService);

  /** Ruta de origen (`?returnUrl=`): tras registrarse se vuelve a ella (CA-1-01). */
  readonly returnUrl = input<string | null>(null);

  protected readonly nombres = signal('');
  protected readonly apellidos = signal('');
  protected readonly dni = signal('');
  protected readonly telefono = signal('');
  protected readonly email = signal('');
  protected readonly password = signal('');
  protected readonly aceptaTerminos = signal(false);
  protected readonly aceptaPrivacidad = signal(false);

  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal('');
  protected readonly politicas = signal<Politicas | null>(null);
  protected readonly estadoPoliticas = signal<'cargando' | 'listo' | 'error'>('cargando');
  private readonly intentado = signal(false);
  /** Errores que devolvió el backend (duplicados o un 400 que llegó igual), por campo. */
  private readonly delServidor = signal<Record<string, string>>({});

  protected readonly errores = computed(() => {
    const servidor = this.delServidor();
    const cliente = this.intentado()
      ? {
          nombres: errorNombre(this.nombres(), 'nombres'),
          apellidos: errorNombre(this.apellidos(), 'apellidos'),
          dni: errorDni(this.dni()),
          telefono: errorCelular(this.telefono()),
          email: errorCorreo(this.email()),
          password: errorPassword(this.password()),
          aceptaTerminos: this.aceptaTerminos() ? '' : 'Debes aceptar los términos y condiciones',
          aceptaPrivacidad: this.aceptaPrivacidad()
            ? ''
            : 'Debes aceptar la política de privacidad',
        }
      : {
          nombres: '',
          apellidos: '',
          dni: '',
          telefono: '',
          email: '',
          password: '',
          aceptaTerminos: '',
          aceptaPrivacidad: '',
        };
    return Object.fromEntries(
      Object.entries(cliente).map(([campo, mensaje]) => [campo, mensaje || servidor[campo] || '']),
    ) as typeof cliente;
  });

  constructor() {
    this.cargarPoliticas();
  }

  /** Las versiones vigentes salen de `/settings/public`; sin ellas no se puede registrar el consentimiento. */
  protected cargarPoliticas(): void {
    this.estadoPoliticas.set('cargando');
    this.publica
      .obtenerConfiguracionPublica('body', false, {
        context: new HttpContext().set(SIN_TOAST, true),
      })
      .subscribe({
        next: (respuesta) => {
          const vigentes = respuesta.data?.politicas;
          if (!vigentes) {
            this.estadoPoliticas.set('error');
            return;
          }
          this.politicas.set({ terminos: vigentes.terminos, privacidad: vigentes.privacidad });
          this.estadoPoliticas.set('listo');
        },
        error: () => this.estadoPoliticas.set('error'),
      });
  }

  protected marcada(evento: Event): boolean {
    return (evento.target as HTMLInputElement).checked;
  }

  protected enviar(evento: Event): void {
    evento.preventDefault();
    this.intentado.set(true);
    this.errorGeneral.set('');
    this.delServidor.set({});
    const vigentes = this.politicas();
    if (!vigentes || this.enviando() || Object.values(this.errores()).some(Boolean)) return;

    this.enviando.set(true);
    this.auth
      .registrarCliente(
        {
          email: this.email().trim(),
          password: this.password(),
          nombres: this.nombres().trim(),
          apellidos: this.apellidos().trim(),
          dni: this.dni().trim(),
          telefono: this.telefono().trim(),
          aceptaTerminos: true,
          aceptaPrivacidad: true,
          versionTerminos: vigentes.terminos,
          versionPrivacidad: vigentes.privacidad,
        },
        'body',
        false,
        { context: new HttpContext().set(SIN_TOAST, true) },
      )
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
          this.mostrarError(error);
        },
      });
  }

  private mostrarError(error: unknown): void {
    const cuerpo = comoApiError(error);
    if (cuerpo?.codigo === 'CORREO_DUPLICADO') {
      this.delServidor.set({ email: cuerpo.mensaje });
    } else if (cuerpo?.codigo === 'DNI_DUPLICADO') {
      this.delServidor.set({ dni: cuerpo.mensaje });
    } else if (camposConError(error).length > 0) {
      this.delServidor.set(
        Object.fromEntries(camposConError(error).map((c) => [c.campo, c.mensaje])),
      );
    } else {
      this.errorGeneral.set('No pudimos crear tu cuenta. Inténtalo en un momento.');
    }
  }
}
