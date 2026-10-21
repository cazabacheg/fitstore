import { HttpContext } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SIN_TOAST, comoApiError, settingsApi } from 'shared';
import { PoliticasService } from 'shared/api/settings-service';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { EstadoCargando } from '../../ui/estados/estado-cargando';
import { EstadoError } from '../../ui/estados/estado-error';
import { renderizarMarkdownSeguro } from './markdown-seguro';

type TipoPolitica = 'terminos' | 'privacidad' | 'cambios-devoluciones' | 'envios';

const TIPOS: { tipo: TipoPolitica; etiqueta: string }[] = [
  { tipo: 'terminos', etiqueta: 'Términos y condiciones' },
  { tipo: 'privacidad', etiqueta: 'Privacidad' },
  { tipo: 'cambios-devoluciones', etiqueta: 'Cambios y devoluciones' },
  { tipo: 'envios', etiqueta: 'Envíos' },
];
const MINIMO = 50;
const MAXIMO = 50_000;
const FECHA = new Intl.DateTimeFormat('es-PE', {
  timeZone: 'America/Lima',
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
});

/**
 * Dirección de la tienda a partir de la del backoffice: `admin.x` → `tienda.x` en la nube y el puerto 4300 → 4200
 * en desarrollo. Con otro esquema (un mismo dominio), usa el mismo origen.
 */
export function urlTienda(origen: string = window.location.origin): string {
  return origen.replace('://admin.', '://tienda.').replace(/:4300$/, ':4200');
}

/**
 * Políticas (P-3-05, `/configuracion/politicas`, ADMIN): editor Markdown con vista previa sanitizada, publicación de
 * una versión nueva (las anteriores no se editan) e historial con enlace "ver" a la tienda.
 */
@Component({
  selector: 'fs-politicas',
  imports: [RouterLink, EncabezadoPagina, EstadoCargando, EstadoError],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fs-encabezado-pagina titulo="Políticas">
      <a accion class="fs-btn fs-btn--contorno" routerLink="/configuracion"
        >Volver a la configuración</a
      >
    </fs-encabezado-pagina>

    <div class="tipos" role="tablist" aria-label="Política">
      @for (t of tipos; track t.tipo) {
        <button
          type="button"
          role="tab"
          class="fs-chip"
          [attr.data-testid]="'tipo-' + t.tipo"
          [attr.aria-selected]="tipo() === t.tipo"
          [class.tipos__activo]="tipo() === t.tipo"
          (click)="elegir(t.tipo)"
        >
          {{ t.etiqueta }}
        </button>
      }
    </div>

    @if (cargando()) {
      <fs-estado-cargando [lineas]="6" />
    } @else if (errorCarga()) {
      <fs-estado-error texto="No pudimos cargar la política." (reintentar)="cargar()" />
    } @else {
      <p class="fs-suave">
        Al publicar se crea la versión {{ siguiente() }}, con tu usuario y la fecha de hoy, y pasa a
        ser la vigente; las anteriores no se pueden editar. El texto se guarda tal cual y se muestra
        sanitizado en la tienda.
      </p>
      <div class="editor">
        <div class="fs-campo">
          <label class="fs-campo__etiqueta" for="pol-editor"
            >Texto en Markdown ({{ largo() }} caracteres)</label
          >
          <textarea
            id="pol-editor"
            class="fs-input editor__texto"
            data-testid="editor"
            rows="22"
            [value]="texto()"
            (input)="texto.set($any($event.target).value)"
          ></textarea>
          @if (largo() < minimo) {
            <span class="fs-campo__ayuda">Escribe al menos 50 caracteres para publicar.</span>
          } @else if (largo() > maximo) {
            <span class="fs-campo__error">El texto no puede pasar de 50 000 caracteres.</span>
          }
        </div>
        <div>
          <h3 class="editor__titulo">Vista previa</h3>
          <div class="editor__previa" data-testid="vista-previa" [innerHTML]="html()"></div>
        </div>
      </div>

      @if (errorPublicar()) {
        <p class="error" role="alert" data-testid="error-publicar">{{ errorPublicar() }}</p>
      }
      @if (mensaje()) {
        <p class="fs-suave" role="status" data-testid="mensaje-publicada">{{ mensaje() }}</p>
      }
      <button
        type="button"
        class="fs-btn fs-btn--primario"
        data-testid="publicar"
        [disabled]="!puedePublicar()"
        (click)="publicar()"
      >
        {{ publicando() ? 'Publicando…' : 'Publicar nueva versión' }}
      </button>

      <section class="historial" aria-label="Historial de versiones">
        <h3>Historial de versiones</h3>
        <ul>
          @for (v of versiones(); track v.version) {
            <li data-testid="version-fila">
              <span
                >Versión {{ v.version }} · {{ fecha(v.publicadaEn) }}
                @if (v.publicadaPor) {
                  · por {{ v.publicadaPor }}
                }
              </span>
              @if (v.vigente) {
                <span class="fs-insignia fs-insignia--exito">Vigente</span>
              }
              <a
                data-testid="ver-version"
                target="_blank"
                rel="noopener"
                [href]="enlaceVer(v.version)"
                >ver</a
              >
            </li>
          }
        </ul>
      </section>
    }
  `,
  styles: `
    .tipos {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-block: 16px;
    }
    .tipos__activo {
      background: var(--fs-color-primario-suave);
      border-color: var(--fs-color-primario-texto);
    }
    .editor {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 24px;
      margin-block: 16px;
    }
    .editor__texto {
      font-family: monospace;
      width: 100%;
      box-sizing: border-box;
    }
    .editor__titulo {
      margin: 0 0 8px;
    }
    .editor__previa {
      min-height: 200px;
      max-height: 520px;
      overflow: auto;
      padding: 16px;
      border: 1px solid var(--fs-color-linea);
      border-radius: 8px;
      background: var(--fs-color-superficie, #fff);
    }
    .error {
      color: var(--fs-color-error, #b3261e);
    }
    .historial ul {
      display: grid;
      gap: 8px;
      padding: 0;
      list-style: none;
    }
    .historial li {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      align-items: center;
    }
  `,
})
export class PoliticasPage {
  private readonly ajustes = inject(PoliticasService);
  private readonly sinAviso = new HttpContext().set(SIN_TOAST, true);

  protected readonly tipos = TIPOS;
  protected readonly minimo = MINIMO;
  protected readonly maximo = MAXIMO;
  protected readonly tipo = signal<TipoPolitica>('terminos');
  protected readonly texto = signal('');
  protected readonly versiones = signal<settingsApi.VersionPolitica[]>([]);
  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal(false);
  protected readonly publicando = signal(false);
  protected readonly errorPublicar = signal('');
  protected readonly mensaje = signal('');

  protected readonly largo = computed(() => this.texto().length);
  protected readonly html = computed(() => renderizarMarkdownSeguro(this.texto()));
  protected readonly siguiente = computed(() => (this.versiones()[0]?.version ?? 0) + 1);
  protected readonly puedePublicar = computed(
    () => !this.publicando() && this.largo() >= MINIMO && this.largo() <= MAXIMO,
  );

  constructor() {
    this.cargar();
  }

  protected elegir(tipo: TipoPolitica): void {
    this.tipo.set(tipo);
    this.mensaje.set('');
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(false);
    this.errorPublicar.set('');
    const tipo = this.tipo();
    let pendientes = 2;
    let fallo = false;
    const terminar = () => {
      if (--pendientes === 0) {
        this.errorCarga.set(fallo);
        this.cargando.set(false);
      }
    };
    this.ajustes
      .obtenerPolitica(tipo, undefined, 'body', false, { context: this.sinAviso })
      .subscribe({
        next: (r) => {
          this.texto.set(r.data?.contenido ?? '');
          terminar();
        },
        error: () => {
          fallo = true;
          terminar();
        },
      });
    this.cargarVersiones(terminar);
  }

  protected publicar(): void {
    if (!this.puedePublicar()) return;
    this.publicando.set(true);
    this.errorPublicar.set('');
    this.mensaje.set('');
    this.ajustes
      .publicarPolitica(this.tipo(), { contenido: this.texto() }, 'body', false, {
        context: this.sinAviso,
      })
      .subscribe({
        next: (r) => {
          this.publicando.set(false);
          this.mensaje.set(
            `Publicamos la versión ${r.data?.version}. Ya es la vigente en la tienda.`,
          );
          this.cargarVersiones();
        },
        error: (e: unknown) => {
          this.publicando.set(false);
          this.errorPublicar.set(
            comoApiError(e)?.mensaje ?? 'No pudimos publicar la política. Inténtalo de nuevo.',
          );
        },
      });
  }

  protected fecha(valor: string): string {
    return FECHA.format(new Date(valor));
  }

  protected enlaceVer(version: number): string {
    return `${urlTienda()}/politicas/${this.tipo()}?version=${version}`;
  }

  private cargarVersiones(alTerminar?: () => void): void {
    this.ajustes
      .listarVersionesPolitica(this.tipo(), 'body', false, { context: this.sinAviso })
      .subscribe({
        next: (r) => {
          this.versiones.set(r.data ?? []);
          alTerminar?.();
        },
        error: () => alTerminar?.(),
      });
  }
}
