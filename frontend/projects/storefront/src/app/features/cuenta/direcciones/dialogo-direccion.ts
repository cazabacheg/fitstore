import { HttpContext } from '@angular/common/http';
import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { SIN_TOAST, authApi, camposConError, comoApiError } from 'shared';
import { ClienteService } from 'shared/api/auth-service';
import { FsBoton } from '../../../ui/boton/boton';
import { FsCampo } from '../../../ui/campo/campo';
import { FsDialogo } from '../../../ui/dialogo/dialogo';
import { FsSelectorUbigeo } from '../../../ui/selector-ubigeo/selector-ubigeo';
import { errorCelular } from '../../acceso/validaciones';

/**
 * Diálogo para agregar o editar una dirección (P-1-06). Es reutilizable (A6): el checkout lo abre con
 * `DialogoDireccionService.abrir()`. Él mismo llama a `POST` o `PUT /clientes/me/direcciones` y respeta el límite de 10.
 */
@Component({
  selector: 'fs-dialogo-direccion',
  imports: [FsDialogo, FsCampo, FsBoton, FsSelectorUbigeo],
  template: `
    <fs-dialogo
      [titulo]="direccion() ? 'Editar dirección' : 'Agregar dirección'"
      [abierto]="true"
      (cerrado)="cancelada.emit()"
    >
      <form
        id="form-direccion"
        novalidate
        (submit)="guardar($event)"
        style="display: grid; gap: var(--espacio-4)"
      >
        @if (errorGeneral()) {
          <div class="fs-aviso fs-aviso--error" role="alert">
            <span class="fs-aviso__texto">{{ errorGeneral() }}</span>
          </div>
        }
        <fs-campo
          etiqueta="Alias"
          placeholder="Casa, Trabajo…"
          [(valor)]="alias"
          [error]="errores().alias"
        />
        <div>
          <fs-selector-ubigeo [valor]="ubigeoInicial()" (cambio)="ubigeo.set($event ?? '')" />
          @if (errores().ubigeo) {
            <span class="fs-campo__error">{{ errores().ubigeo }}</span>
          }
        </div>
        <fs-campo etiqueta="Dirección" [(valor)]="texto" [error]="errores().direccion" />
        <fs-campo etiqueta="Referencia" [(valor)]="referencia" [error]="errores().referencia" />
        <fs-campo
          etiqueta="Celular de contacto"
          tipo="tel"
          ayuda="Si lo dejas vacío usaremos tu celular"
          [(valor)]="telefonoContacto"
          [error]="errores().telefonoContacto"
        />
      </form>
      <button fsAcciones type="button" fsBoton variante="contorno" (click)="cancelada.emit()">
        Cancelar
      </button>
      <button
        fsAcciones
        type="submit"
        form="form-direccion"
        fsBoton
        variante="primario"
        [disabled]="enviando()"
      >
        Guardar
      </button>
    </fs-dialogo>
  `,
})
export class DialogoDireccion implements OnInit {
  private readonly clientes = inject(ClienteService);

  /** Dirección que se edita; sin ella, se crea una nueva. */
  readonly direccion = input<authApi.Direccion | null>(null);
  /** La dirección que devolvió la API tras guardar. */
  readonly guardada = output<authApi.Direccion>();
  readonly cancelada = output<void>();

  protected readonly alias = signal('');
  protected readonly ubigeo = signal('');
  protected readonly ubigeoInicial = signal<string | null>(null);
  protected readonly texto = signal('');
  protected readonly referencia = signal('');
  protected readonly telefonoContacto = signal('');
  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal('');
  private readonly intentado = signal(false);
  private readonly delServidor = signal<Record<string, string>>({});

  protected readonly errores = computed(() => {
    const servidor = this.delServidor();
    const intentado = this.intentado();
    const celular = this.telefonoContacto().trim();
    const cliente = intentado
      ? {
          alias: this.alias().trim() ? '' : 'Ponle un nombre a la dirección',
          ubigeo: this.ubigeo() ? '' : 'Elige el distrito',
          direccion: this.texto().trim().length >= 5 ? '' : 'Escribe al menos 5 caracteres',
          referencia: '',
          telefonoContacto: celular ? errorCelular(celular) : '',
        }
      : { alias: '', ubigeo: '', direccion: '', referencia: '', telefonoContacto: '' };
    return Object.fromEntries(
      Object.entries(cliente).map(([campo, mensaje]) => [campo, mensaje || servidor[campo] || '']),
    ) as typeof cliente;
  });

  ngOnInit(): void {
    const d = this.direccion();
    if (!d) return;
    this.alias.set(d.alias);
    this.ubigeo.set(d.ubigeo);
    this.ubigeoInicial.set(d.ubigeo);
    this.texto.set(d.direccion);
    this.referencia.set(d.referencia ?? '');
    this.telefonoContacto.set(d.telefonoContacto ?? '');
  }

  protected guardar(evento: Event): void {
    evento.preventDefault();
    this.intentado.set(true);
    this.errorGeneral.set('');
    this.delServidor.set({});
    if (Object.values(this.errores()).some(Boolean) || this.enviando()) return;

    const cuerpo: authApi.DireccionRequest = {
      alias: this.alias().trim(),
      ubigeo: this.ubigeo(),
      direccion: this.texto().trim(),
      referencia: this.referencia().trim() || null,
      telefonoContacto: this.telefonoContacto().trim() || null,
    };
    const contexto = { context: new HttpContext().set(SIN_TOAST, true) };
    const existente = this.direccion();
    const llamada = existente
      ? this.clientes.actualizarDireccion(existente.id, cuerpo, 'body', false, contexto)
      : this.clientes.crearDireccion(cuerpo, 'body', false, contexto);

    this.enviando.set(true);
    llamada.subscribe({
      next: (respuesta) => {
        this.enviando.set(false);
        if (respuesta.data) this.guardada.emit(respuesta.data);
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        this.mostrarError(error);
      },
    });
  }

  private mostrarError(error: unknown): void {
    const campos = camposConError(error);
    if (comoApiError(error)?.codigo === 'LIMITE_DIRECCIONES') {
      this.errorGeneral.set('Llegaste al máximo de 10 direcciones. Elimina una para agregar otra.');
    } else if (campos.length > 0) {
      this.delServidor.set(Object.fromEntries(campos.map((c) => [c.campo, c.mensaje])));
    } else {
      this.errorGeneral.set('No pudimos guardar la dirección. Inténtalo en un momento.');
    }
  }
}
