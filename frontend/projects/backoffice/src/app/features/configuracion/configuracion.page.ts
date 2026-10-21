import { HttpContext, HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import type { Observable } from 'rxjs';
import { SIN_TOAST, ToastService, camposConError, comoApiError, settingsApi } from 'shared';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { EstadoCargando } from '../../ui/estados/estado-cargando';
import { EstadoError } from '../../ui/estados/estado-error';
import { ContenidoTab, type DatosContenido } from './contenido-tab';
import { EditorTema } from './editor-tema';
import { IdentidadTab, type DatosIdentidad, type TipoLogo } from './identidad-tab';
import { ParametrosTab, type DatosParametros } from './parametros-tab';
import type { ConCambiosSinGuardar } from './sin-cambios.guard';

type Pestana = 'identidad' | 'tema' | 'contenido' | 'parametros';
type Respuesta = settingsApi.RespuestaConfiguracionAdmin;

const PESTANAS: { id: Pestana; etiqueta: string }[] = [
  { id: 'identidad', etiqueta: 'Identidad' },
  { id: 'tema', etiqueta: 'Tema' },
  { id: 'contenido', etiqueta: 'Contenido' },
  { id: 'parametros', etiqueta: 'Parámetros' },
];

/** Campo del contrato (K5) → campo de la pantalla, para mostrar el error del servidor donde corresponde. */
function campoDePantalla(campo: string): string {
  if (campo === 'tiendaFisica.ubigeo') return 'tiendaUbigeo';
  if (campo.startsWith('tiendaFisica')) return 'tiendaDireccion';
  if (campo.startsWith('beneficios')) return 'beneficios';
  return campo;
}

const igual = <T>(a: T, b: T) => JSON.stringify(a) === JSON.stringify(b);

/**
 * Configuración de tienda (P-3-04, `/configuracion`, ADMIN): pestañas Identidad, Tema, Contenido y Parámetros;
 * cada una guarda su sección con la versión que abrió (409 si otro admin guardó antes), y Políticas lleva a P-3-05.
 * Avisa al salir con cambios sin guardar.
 */
@Component({
  selector: 'fs-configuracion',
  imports: [
    RouterLink,
    EncabezadoPagina,
    EstadoCargando,
    EstadoError,
    IdentidadTab,
    EditorTema,
    ContenidoTab,
    ParametrosTab,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fs-encabezado-pagina titulo="Configuración de tienda" />
    @if (cargando()) {
      <fs-estado-cargando [lineas]="6" />
    } @else if (errorCarga()) {
      <fs-estado-error
        texto="No pudimos cargar la configuración de la tienda."
        (reintentar)="cargar()"
      />
    } @else if (config(); as c) {
      @if (conflicto()) {
        <div class="fs-aviso fs-aviso--error" role="alert" data-testid="conflicto">
          <span>Otro administrador modificó la configuración; recarga la página</span>
          <button
            type="button"
            class="fs-btn fs-btn--contorno fs-btn--sm"
            data-testid="recargar"
            (click)="cargar()"
          >
            Recargar
          </button>
        </div>
      }
      <div class="pestanas" role="tablist" aria-label="Secciones de la configuración">
        @for (p of pestanas; track p.id) {
          <button
            type="button"
            role="tab"
            class="pestanas__pestana"
            [attr.data-testid]="'pestana-' + p.id"
            [attr.aria-selected]="pestana() === p.id"
            [class.pestanas__pestana--activa]="pestana() === p.id"
            (click)="pestana.set(p.id)"
          >
            {{ p.etiqueta }}
            @if (sinGuardar()[p.id]) {
              <span
                class="pestanas__punto"
                title="Cambios sin guardar"
                aria-label="Cambios sin guardar"
                >●</span
              >
            }
          </button>
        }
        <a
          class="pestanas__pestana"
          role="tab"
          data-testid="pestana-politicas"
          routerLink="/configuracion/politicas"
          >Políticas</a
        >
      </div>

      <section data-testid="panel-identidad" role="tabpanel" [hidden]="pestana() !== 'identidad'">
        <fs-identidad-tab
          [identidad]="identidad()!"
          [guardando]="guardando() === 'identidad'"
          [erroresServidor]="errores().identidad ?? {}"
          (guardar)="guardarIdentidad($event)"
          (subirImagen)="subirLogo($event)"
          (sinGuardar)="marcar('identidad', $event)"
        />
      </section>
      <section data-testid="panel-tema" role="tabpanel" [hidden]="pestana() !== 'tema'">
        <fs-editor-tema
          [tema]="tema()!"
          [nombreTienda]="c.identidad.nombre"
          [guardando]="guardando() === 'tema'"
          [advertenciasServidor]="advertenciasTema()"
          (guardar)="guardarTema($event)"
          (sinGuardar)="marcar('tema', $event)"
        />
      </section>
      <section data-testid="panel-contenido" role="tabpanel" [hidden]="pestana() !== 'contenido'">
        <fs-contenido-tab
          [contenido]="c.contenido"
          [guardando]="guardando() === 'contenido'"
          [erroresServidor]="errores().contenido ?? {}"
          (guardar)="guardarContenido($event)"
          (sinGuardar)="marcar('contenido', $event)"
        />
      </section>
      <section data-testid="panel-parametros" role="tabpanel" [hidden]="pestana() !== 'parametros'">
        <fs-parametros-tab
          [parametros]="parametros()!"
          [correosAlertas]="c.correosAlertas"
          [guardando]="guardando() === 'parametros'"
          [erroresServidor]="errores().parametros ?? {}"
          (guardar)="guardarParametros($event)"
          (sinGuardar)="marcar('parametros', $event)"
        />
      </section>
    }
  `,
  styles: `
    .pestanas {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      margin-block: 16px 24px;
      border-bottom: 1px solid var(--fs-color-linea);
    }
    .pestanas__pestana {
      padding: 10px 16px;
      border: 0;
      border-bottom: 3px solid transparent;
      background: none;
      color: var(--fs-color-texto);
      font: inherit;
      cursor: pointer;
      text-decoration: none;
    }
    .pestanas__pestana--activa {
      border-bottom-color: var(--fs-color-primario-texto);
      font-weight: 600;
    }
    .pestanas__punto {
      color: var(--fs-color-primario-texto);
    }
    [hidden] {
      display: none !important;
    }
  `,
})
export class ConfiguracionPage implements ConCambiosSinGuardar {
  private readonly api = inject(settingsApi.ConfiguracionAdminService);
  private readonly avisos = inject(ToastService);
  private readonly sinAviso = new HttpContext().set(SIN_TOAST, true);

  protected readonly pestanas = PESTANAS;
  protected readonly pestana = signal<Pestana>('identidad');
  protected readonly config = signal<settingsApi.ConfiguracionAdmin | null>(null);
  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal(false);
  protected readonly conflicto = signal(false);
  protected readonly guardando = signal<Pestana | null>(null);
  protected readonly errores = signal<Partial<Record<Pestana, Record<string, string>>>>({});
  protected readonly advertenciasTema = signal<string[]>([]);
  protected readonly sinGuardar = signal<Partial<Record<Pestana, boolean>>>({});

  // Cada pestaña recibe solo su parte, y solo se actualiza si esa parte cambió
  protected readonly identidad = computed(() => this.config()?.identidad, { equal: igual });
  protected readonly tema = computed(() => this.config()?.tema, { equal: igual });
  protected readonly parametros = computed(() => this.config()?.parametros, { equal: igual });

  constructor() {
    this.cargar();
  }

  haySinGuardar(): boolean {
    return Object.values(this.sinGuardar()).some(Boolean);
  }

  @HostListener('window:beforeunload', ['$event'])
  protected avisarAlCerrar(evento: BeforeUnloadEvent): void {
    if (this.haySinGuardar()) evento.preventDefault();
  }

  protected marcar(pestana: Pestana, valor: boolean): void {
    if (this.sinGuardar()[pestana] !== valor)
      this.sinGuardar.update((s) => ({ ...s, [pestana]: valor }));
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(false);
    this.conflicto.set(false);
    this.api.obtenerConfiguracionAdmin('body', false, { context: this.sinAviso }).subscribe({
      next: (r) => {
        this.config.set(r.data!);
        this.errores.set({});
        this.advertenciasTema.set([]);
        this.cargando.set(false);
      },
      error: () => {
        this.errorCarga.set(true);
        this.cargando.set(false);
      },
    });
  }

  protected guardarIdentidad(datos: DatosIdentidad): void {
    this.guardarSeccion('identidad', 'Guardamos la identidad', (version) =>
      this.api.guardarIdentidad({ ...datos, version }, 'body', false, { context: this.sinAviso }),
    );
  }

  protected guardarTema(tema: settingsApi.Tema): void {
    this.guardarSeccion('tema', 'Guardamos el tema', (version) =>
      this.api.guardarTema({ ...tema, version }, 'body', false, { context: this.sinAviso }),
    );
  }

  protected guardarContenido(datos: DatosContenido): void {
    this.guardarSeccion('contenido', 'Guardamos el contenido', (version) =>
      this.api.guardarContenido({ ...datos, version }, 'body', false, { context: this.sinAviso }),
    );
  }

  protected guardarParametros(datos: DatosParametros): void {
    this.guardarSeccion('parametros', 'Guardamos los parámetros', (version) =>
      this.api.guardarParametros({ ...datos, version }, 'body', false, { context: this.sinAviso }),
    );
  }

  protected subirLogo(evento: { tipo: TipoLogo; archivo: File }): void {
    this.api
      .subirImagenTienda(evento.archivo, evento.tipo, undefined, undefined, 'body', false, {
        context: this.sinAviso,
      })
      .subscribe({
        next: (r) => {
          const url = r.data!.url;
          this.config.update((c) =>
            c
              ? {
                  ...c,
                  identidad: {
                    ...c.identidad,
                    ...(evento.tipo === 'LOGO_CLARO' ? { logoClaroUrl: url } : {}),
                    ...(evento.tipo === 'LOGO_OSCURO' ? { logoOscuroUrl: url } : {}),
                    ...(evento.tipo === 'FAVICON' ? { faviconUrl: url } : {}),
                  },
                }
              : c,
          );
          this.avisos.mostrar('exito', 'Guardamos la imagen');
        },
        error: (e: unknown) =>
          this.avisos.mostrar(
            'error',
            comoApiError(e)?.mensaje ?? 'No pudimos subir la imagen. Inténtalo de nuevo.',
          ),
      });
  }

  private guardarSeccion(
    pestana: Pestana,
    mensaje: string,
    llamada: (version: number) => Observable<Respuesta>,
  ): void {
    const actual = this.config();
    if (!actual || this.guardando()) return;
    this.guardando.set(pestana);
    this.conflicto.set(false);
    this.errores.update((e) => ({ ...e, [pestana]: {} }));
    llamada(actual.version).subscribe({
      next: (r) => {
        this.guardando.set(null);
        this.config.set(r.data!);
        if (pestana === 'tema') this.advertenciasTema.set(r.data!.advertencias ?? []);
        this.avisos.mostrar('exito', mensaje);
      },
      error: (e: unknown) => {
        this.guardando.set(null);
        if (e instanceof HttpErrorResponse && e.status === 409) {
          this.conflicto.set(true);
        } else if (e instanceof HttpErrorResponse && e.status === 400) {
          const porCampo: Record<string, string> = {};
          for (const { campo, mensaje: texto } of camposConError(e))
            porCampo[campoDePantalla(campo)] = texto;
          this.errores.update((x) => ({ ...x, [pestana]: porCampo }));
        } else {
          this.avisos.mostrar(
            'error',
            comoApiError(e)?.mensaje ?? 'No pudimos guardar. Inténtalo de nuevo.',
          );
        }
      },
    });
  }
}
