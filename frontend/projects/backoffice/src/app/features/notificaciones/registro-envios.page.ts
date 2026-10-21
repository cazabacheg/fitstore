import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { map, tap } from 'rxjs';
import { FechaPipe, ToastService, type ConsultaPagina, type Pagina, type notificationApi } from 'shared';
import { RegistroService } from 'shared/api/notification-service';
import { DialogoService } from '../../ui/dialogo/dialogo.service';
import { EstadoError } from '../../ui/estados/estado-error';
import { BarraFiltros, type ChipFiltro, type SelectFiltro } from '../../ui/filtros/barra-filtros';
import { FsCelda } from '../../ui/tabla/fs-celda';
import { FsTabla, type AccionFila, type ColumnaTabla } from '../../ui/tabla/fs-tabla';
import {
  CODIGOS_PLANTILLA,
  ESTADOS_ENVIO,
  enlaceReferencia,
  estadoEnvio,
  nombrePlantilla,
  sePuedeReenviar,
} from './etiquetas';
import { PanelDetalleEnvio } from './panel-detalle-envio';

type Fila = notificationApi.EnvioResumen;

const fecha = new FechaPipe();

const PLANTILLAS: SelectFiltro = {
  clave: 'plantilla',
  etiqueta: 'Plantilla',
  opciones: CODIGOS_PLANTILLA.map((c) => ({ valor: c, etiqueta: nombrePlantilla(c) })),
};
const CHIPS: ChipFiltro[] = Object.entries(ESTADOS_ENVIO).map(([valor, { etiqueta }]) => ({ valor, etiqueta }));

/**
 * Registro de envíos (P-7-01, solo ADMIN): los correos del más reciente al más antiguo, con filtros en la URL, el panel
 * lateral de detalle (N1) y el reenvío de los FALLIDO y ENVIADO con confirmación (CA-7-19 a CA-7-21).
 */
@Component({
  selector: 'fs-registro-envios-page',
  imports: [FsTabla, FsCelda, BarraFiltros, EstadoError, PanelDetalleEnvio, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="fs-pagina" style="display: grid; gap: var(--espacio-4)">
      <header>
        <h1 class="fs-titulo-pagina">Notificaciones</h1>
        <p class="fs-suave">Los correos que envía la tienda, con su estado. Aquí puedes reenviar los que fallaron.</p>
      </header>

      <fs-barra-filtros
        placeholder="Buscar por destinatario"
        [selects]="selects"
        [chips]="chips"
        [contadores]="contadores()"
        (cambio)="alCambiarFiltros()"
      />
      <div class="envios-extra">
        <label>
          Referencia
          <input
            type="search"
            class="fs-input"
            aria-label="Referencia"
            placeholder="FS-000124 o RC-2026-000001"
            [value]="referencia()"
            (change)="cambiarParametro('referencia', $any($event.target).value.trim())"
          />
        </label>
        <label>
          Desde
          <input
            type="date"
            class="fs-input"
            aria-label="Desde"
            [value]="desde()"
            (change)="cambiarParametro('desde', $any($event.target).value)"
          />
        </label>
        <label>
          Hasta
          <input
            type="date"
            class="fs-input"
            aria-label="Hasta"
            [value]="hasta()"
            (change)="cambiarParametro('hasta', $any($event.target).value)"
          />
        </label>
      </div>

      <div class="envios-cuerpo" [class.envios-cuerpo--con-panel]="seleccionado() !== null">
        @if (datos.error()) {
          <fs-estado-error
            titulo="No pudimos cargar los envíos"
            texto="Revisa tu conexión e inténtalo de nuevo."
            (reintentar)="datos.reload()"
          />
        } @else {
          <fs-tabla
            columnaId="asunto"
            tituloVacio="Sin resultados"
            textoVacio="No hay correos con esos filtros."
            [columnas]="columnas"
            [datos]="datos.value() ?? null"
            [cargando]="datos.isLoading()"
            [abribleEnDialogo]="true"
            [acciones]="acciones"
            (alAbrir)="abrirPanel($event)"
            (consulta)="consulta.set($event)"
          >
            <ng-template fsCelda="estado" let-fila>
              <span [class]="'fs-insignia fs-insignia--' + estadoDe(fila.estado).variante">{{
                estadoDe(fila.estado).etiqueta
              }}</span>
            </ng-template>
            <ng-template fsCelda="referencia" let-fila>
              @if (fila.referencia; as ref) {
                @if (enlaceDe(ref); as enlace) {
                  <a [routerLink]="enlace.ruta" [queryParams]="enlace.params">{{ ref }}</a>
                } @else {
                  {{ ref }}
                }
              } @else {
                —
              }
            </ng-template>
          </fs-tabla>
        }
        @if (seleccionado(); as id) {
          <fs-panel-detalle-envio
            [id]="id"
            [version]="version()"
            (cerrar)="seleccionado.set(null)"
            (reenviar)="pedirReenvio($event)"
          />
        }
      </div>
    </section>
  `,
  styles: `
    .envios-extra {
      display: flex;
      gap: var(--espacio-3);
      flex-wrap: wrap;
    }
    .envios-extra label {
      display: grid;
      gap: var(--espacio-1);
      font-size: 13px;
    }
    .envios-cuerpo {
      display: grid;
      gap: var(--espacio-4);
      align-items: start;
    }
    @media (min-width: 1100px) {
      .envios-cuerpo--con-panel {
        grid-template-columns: minmax(0, 1fr) 380px;
      }
    }
  `,
})
export class RegistroEnviosPage {
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(RegistroService);
  private readonly confirmacion = inject(DialogoService);
  private readonly toasts = inject(ToastService);

  protected readonly selects: SelectFiltro[] = [PLANTILLAS];
  protected readonly chips = CHIPS;
  protected readonly columnas: ColumnaTabla<Fila>[] = [
    { clave: 'creadoEn', titulo: 'Fecha', valor: (f) => fecha.transform(f.creadoEn, 'fechaHora') },
    { clave: 'asunto', titulo: 'Asunto', valor: (f) => f.asunto },
    { clave: 'plantilla', titulo: 'Plantilla', valor: (f) => nombrePlantilla(f.plantilla) },
    { clave: 'destinatario', titulo: 'Destinatario', valor: (f) => f.destinatario },
    { clave: 'referencia', titulo: 'Referencia' },
    { clave: 'estado', titulo: 'Estado' },
    { clave: 'intentos', titulo: 'Intentos', valor: (f) => f.intentos },
  ];
  protected readonly estadoDe = estadoEnvio;
  protected readonly enlaceDe = enlaceReferencia;

  private readonly parametros = toSignal(this.ruta.queryParamMap, {
    initialValue: this.ruta.snapshot.queryParamMap,
  });
  protected readonly referencia = computed(() => this.parametros().get('referencia') ?? '');
  protected readonly desde = computed(() => this.parametros().get('desde') ?? '');
  protected readonly hasta = computed(() => this.parametros().get('hasta') ?? '');
  protected readonly consulta = signal<ConsultaPagina>({ page: 0, size: 20 });
  protected readonly contadores = signal<Record<string, number> | null>(null);
  protected readonly seleccionado = signal<string | null>(null);
  protected readonly version = signal(0);

  private readonly peticion = computed(() => {
    const p = this.parametros();
    return {
      estado: p.get('estado'),
      plantilla: p.get('plantilla'),
      destinatario: p.get('q'),
      referencia: p.get('referencia'),
      desde: p.get('desde'),
      hasta: p.get('hasta'),
      ...this.consulta(),
    };
  });

  protected readonly datos = rxResource({
    params: () => this.peticion(),
    stream: ({ params: q }) =>
      this.api
        .listarEnvios(
          (q.estado as notificationApi.EstadoEnvio | null) ?? undefined,
          (q.plantilla as notificationApi.CodigoPlantilla | null) ?? undefined,
          q.destinatario ?? undefined,
          q.referencia ?? undefined,
          q.desde ?? undefined,
          q.hasta ?? undefined,
          q.page,
          q.size,
        )
        .pipe(
          tap((r) => this.contadores.set(r.data?.contadores ?? null)),
          map((r) => r.data as unknown as Pagina<Fila>),
        ),
  });

  /** "Reenviar" solo en los FALLIDO y ENVIADO (R11: acciones de la fila). */
  protected readonly acciones = (fila: Fila): AccionFila<Fila>[] =>
    sePuedeReenviar(fila.estado)
      ? [{ etiqueta: 'Reenviar', alEjecutar: (f) => this.pedirReenvio(f) }]
      : [];

  protected abrirPanel(fila: Fila): void {
    this.seleccionado.set(fila.id);
  }

  /** Al cambiar un filtro se vuelve a la primera página. */
  protected alCambiarFiltros(): void {
    if (this.consulta().page !== 0) this.consulta.update((c) => ({ ...c, page: 0 }));
  }

  protected cambiarParametro(clave: 'referencia' | 'desde' | 'hasta', valor: string): void {
    this.alCambiarFiltros();
    void this.router.navigate([], {
      relativeTo: this.ruta,
      queryParams: { [clave]: valor || null },
      queryParamsHandling: 'merge',
    });
  }

  protected pedirReenvio(envio: Pick<Fila, 'id' | 'asunto' | 'destinatario'>): void {
    this.confirmacion
      .confirmar({
        titulo: '¿Reenviar este correo?',
        mensaje: `"${envio.asunto}" volverá a la cola y saldrá a ${envio.destinatario} con la marca y los textos actuales.`,
        accion: 'Reenviar',
      })
      .subscribe((confirmado) => {
        if (!confirmado) return;
        this.api.reenviarEnvio(envio.id).subscribe({
          next: () => {
            this.toasts.mostrar('exito', 'El correo volvió a la cola de envíos');
            this.version.update((v) => v + 1);
            this.datos.reload();
          },
          error: () => undefined, // el interceptor ya mostró el motivo
        });
      });
  }
}
