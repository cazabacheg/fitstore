import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { FechaPipe, type notificationApi } from 'shared';
import { RegistroService } from 'shared/api/notification-service';
import { EstadoError } from '../../ui/estados/estado-error';
import { estadoEnvio, nombrePlantilla, sePuedeReenviar } from './etiquetas';

const fecha = new FechaPipe();

/**
 * Panel lateral con el detalle de un envío (SPEC-7 N1): asunto, destinatario, referencia, cada intento con su fecha
 * y el último error (CA-7-20). El registro no tiene página de detalle, así que el asunto de la fila abre este panel.
 */
@Component({
  selector: 'fs-panel-detalle-envio',
  imports: [EstadoError],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <aside class="panel-envio" role="complementary" aria-label="Detalle del envío">
      <header class="panel-envio__cabecera">
        <h2 class="fs-titulo-seccion">Detalle del envío</h2>
        <button type="button" class="fs-btn fs-btn--texto fs-btn--sm" aria-label="Cerrar el detalle" (click)="cerrar.emit()">
          Cerrar
        </button>
      </header>
      @if (detalle.error()) {
        <fs-estado-error
          titulo="No pudimos cargar el envío"
          texto="Inténtalo de nuevo."
          (reintentar)="detalle.reload()"
        />
      } @else if (envio(); as e) {
        <dl class="panel-envio__datos">
          <dt>Asunto</dt>
          <dd>{{ e.asunto }}</dd>
          <dt>Destinatario</dt>
          <dd>{{ e.destinatario }}</dd>
          <dt>Plantilla</dt>
          <dd>{{ nombrePlantilla(e.plantilla) }}</dd>
          @if (e.referencia) {
            <dt>Referencia</dt>
            <dd>{{ e.referencia }}</dd>
          }
          <dt>Estado</dt>
          <dd>
            <span [class]="'fs-insignia fs-insignia--' + estado(e.estado).variante">{{ estado(e.estado).etiqueta }}</span>
          </dd>
          <dt>Boleta adjunta</dt>
          <dd>{{ e.conAdjunto ? 'Sí' : 'No' }}</dd>
          <dt>Creado</dt>
          <dd>{{ fecha(e.creadoEn) }}</dd>
          @if (e.proximoIntentoEn) {
            <dt>Próximo intento</dt>
            <dd>{{ fecha(e.proximoIntentoEn) }}</dd>
          }
        </dl>
        @if (e.ultimoError) {
          <p class="panel-envio__error" role="alert">Último error: {{ e.ultimoError }}</p>
        }
        <h3 class="fs-titulo-seccion">Intentos</h3>
        @if (e.historialIntentos.length === 0) {
          <p class="fs-suave">Todavía no se intentó enviar.</p>
        } @else {
          <ol class="panel-envio__intentos">
            @for (i of e.historialIntentos; track i.numero) {
              <li>
                <strong>Intento {{ i.numero }}</strong> · {{ fecha(i.fecha) }} ·
                {{ i.error ? 'Falló: ' + i.error : 'Enviado' }}
              </li>
            }
          </ol>
        }
        @if (puedeReenviar(e.estado)) {
          <button type="button" class="fs-btn fs-btn--primario" (click)="reenviar.emit(e)">Reenviar</button>
        }
      } @else {
        <p class="fs-suave">Cargando…</p>
      }
    </aside>
  `,
  styles: `
    .panel-envio {
      display: grid;
      gap: var(--espacio-3);
      padding: var(--espacio-4);
      border: 1px solid var(--fs-color-linea);
      border-radius: var(--radio-md);
      background: var(--fs-color-superficie);
    }
    .panel-envio__cabecera {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .panel-envio__datos {
      display: grid;
      grid-template-columns: max-content 1fr;
      gap: var(--espacio-1) var(--espacio-3);
      margin: 0;
    }
    .panel-envio__datos dt {
      font-weight: 600;
    }
    .panel-envio__datos dd {
      margin: 0;
      overflow-wrap: anywhere;
    }
    .panel-envio__error {
      color: var(--fs-color-error, #b3261e);
      margin: 0;
    }
    .panel-envio__intentos {
      margin: 0;
      padding-left: var(--espacio-4);
      display: grid;
      gap: var(--espacio-1);
    }
  `,
})
export class PanelDetalleEnvio {
  private readonly api = inject(RegistroService);

  readonly id = input.required<string>();
  /** Cuenta para recargar el detalle después de un reenvío. */
  readonly version = input(0);
  readonly cerrar = output<void>();
  readonly reenviar = output<notificationApi.Envio>();

  protected readonly detalle = rxResource({
    params: () => ({ id: this.id(), version: this.version() }),
    stream: ({ params }) => this.api.obtenerEnvio(params.id).pipe(map((r) => r.data as notificationApi.Envio)),
  });
  protected readonly envio = computed(() => this.detalle.value() ?? null);

  protected readonly estado = estadoEnvio;
  protected readonly nombrePlantilla = nombrePlantilla;
  protected readonly puedeReenviar = sePuedeReenviar;
  protected readonly fecha = (valor: string) => fecha.transform(valor, 'fechaHora');
}
