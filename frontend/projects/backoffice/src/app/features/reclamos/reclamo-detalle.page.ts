import { HttpContext, HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FechaPipe, PrecioPipe, SIN_TOAST, SesionStore, ToastService, comoApiError, settingsApi } from 'shared';
import { ReclamosService } from 'shared/api/settings-service';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { DialogoService } from '../../ui/dialogo/dialogo.service';
import { EstadoCargando } from '../../ui/estados/estado-cargando';
import { ESTADOS_RECLAMO, TIPOS_RECLAMO, claseInsignia } from './estados-reclamo';

const MINIMO = 10;
const MAXIMO = 3000;

/**
 * Detalle de un reclamo (P-3-06, `/reclamos/:id`): la Hoja de Reclamación completa, su plazo y la atención.
 * "Tomar" (paso opcional) y "Responder" solo los tiene el ADMIN; el OPERADOR consulta. Respondido es inmutable.
 */
@Component({
  selector: 'fs-reclamo-detalle',
  imports: [RouterLink, EncabezadoPagina, EstadoCargando, FechaPipe, PrecioPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fs-encabezado-pagina [titulo]="reclamo()?.numero ?? 'Reclamo'">
      <a accion class="fs-btn fs-btn--contorno" routerLink="/reclamos">Volver a la bandeja</a>
    </fs-encabezado-pagina>

    @if (cargando()) {
      <fs-estado-cargando [lineas]="6" />
    } @else if (noEncontrado()) {
      <section class="fs-estado" data-testid="no-encontrado">
        <h2 class="fs-estado__titulo">No encontramos este reclamo</h2>
        <a class="fs-btn fs-btn--primario" routerLink="/reclamos">Ir a la bandeja</a>
      </section>
    } @else if (reclamo(); as r) {
      <div class="detalle">
        <section class="detalle__bloque" aria-label="Resumen">
          <dl>
            <dt>Tipo</dt>
            <dd>{{ tipo() }}</dd>
            <dt>Estado</dt>
            <dd data-testid="estado">
              <span [class]="claseEstado()">{{ etiquetaEstado() }}</span>
            </dd>
            <dt>Registrado</dt>
            <dd>{{ r.creadoEn | fecha: 'fechaHora' }}</dd>
            <dt>Fecha límite</dt>
            <dd data-testid="plazo">
              {{ r.fechaLimite | fecha }}
              @if (r.vencido) {
                <span class="fs-insignia fs-insignia--error">Vencido</span>
              } @else if (r.porVencer) {
                <span class="fs-insignia fs-insignia--aviso">Por vencer</span>
              }
            </dd>
          </dl>
        </section>

        <section class="detalle__bloque" aria-label="Consumidor">
          <h2>Consumidor</h2>
          <dl>
            <dt>Nombre</dt>
            <dd>{{ r.consumidor.nombres }} {{ r.consumidor.apellidos }}</dd>
            <dt>Documento</dt>
            <dd>{{ r.consumidor.tipoDocumento }} {{ r.consumidor.numeroDocumento }}</dd>
            <dt>Domicilio</dt>
            <dd>{{ r.consumidor.domicilio }}</dd>
            <dt>Teléfono</dt>
            <dd>{{ r.consumidor.telefono }}</dd>
            <dt>Correo</dt>
            <dd>{{ r.consumidor.email }}</dd>
            @if (r.consumidor.esMenor) {
              <dt>Menor de edad</dt>
              <dd>Sí · apoderado: {{ r.consumidor.apoderadoNombre }}</dd>
            }
          </dl>
        </section>

        <section class="detalle__bloque" aria-label="Bien contratado">
          <h2>Bien contratado</h2>
          <dl>
            <dt>Tipo</dt>
            <dd>{{ r.bien.tipo === 'PRODUCTO' ? 'Producto' : 'Servicio' }}</dd>
            <dt>Descripción</dt>
            <dd>{{ r.bien.descripcion }}</dd>
            @if (r.bien.montoReclamado !== null && r.bien.montoReclamado !== undefined) {
              <dt>Monto reclamado</dt>
              <dd>{{ r.bien.montoReclamado | precio }}</dd>
            }
            @if (r.bien.pedidoCodigo) {
              <dt>Pedido</dt>
              <dd>{{ r.bien.pedidoCodigo }}</dd>
            }
          </dl>
        </section>

        <section class="detalle__bloque" aria-label="Detalle del consumidor">
          <h2>Lo que cuenta el consumidor</h2>
          <p class="detalle__texto">{{ r.detalle }}</p>
          <h2>Lo que solicita</h2>
          <p class="detalle__texto">{{ r.pedidoConsumidor }}</p>
        </section>

        <section class="detalle__bloque" aria-label="Atención">
          <h2>Atención</h2>
          @if (r.estado === 'RESPONDIDO') {
            <div data-testid="respuesta-registrada">
              <p class="detalle__texto">{{ r.respuesta }}</p>
              <p class="fs-suave">
                Respondido el {{ r.respondidoEn | fecha }}
                @if (r.respondidoPor) {
                  por {{ r.respondidoPor }}
                }
                . Ya no se puede modificar.
              </p>
            </div>
          } @else if (esAdmin()) {
            @if (r.estado === 'REGISTRADO') {
              <button
                type="button"
                class="fs-btn fs-btn--contorno"
                data-testid="tomar"
                [disabled]="trabajando()"
                (click)="tomar()"
              >
                Tomar reclamo
              </button>
              <p class="fs-suave">Tomarlo es opcional: también puedes responder directamente.</p>
            }
            <div class="fs-campo">
              <label class="fs-campo__etiqueta" for="rec-respuesta"
                >Respuesta al consumidor ({{ respuesta().length }}/3000)</label
              >
              <textarea
                id="rec-respuesta"
                class="fs-input"
                rows="6"
                data-testid="respuesta"
                [value]="respuesta()"
                (input)="respuesta.set($any($event.target).value)"
              ></textarea>
              <span class="fs-campo__ayuda"
                >Entre 10 y 3000 caracteres. Una vez enviada, no se puede modificar.</span
              >
            </div>
            <button
              type="button"
              class="fs-btn fs-btn--primario"
              data-testid="responder"
              [disabled]="!puedeResponder()"
              (click)="responder()"
            >
              Responder reclamo
            </button>
          } @else {
            <p class="fs-suave">Solo un administrador puede tomar y responder los reclamos.</p>
          }
          @if (errorAccion()) {
            <p class="detalle__error" role="alert" data-testid="error-accion">
              {{ errorAccion() }}
            </p>
          }
        </section>
      </div>
    }
  `,
  styles: `
    .detalle {
      display: grid;
      gap: 16px;
      max-width: 860px;
      margin-top: 16px;
    }
    .detalle__bloque {
      padding: 16px 20px;
      border: 1px solid var(--fs-color-linea);
      border-radius: 8px;
    }
    .detalle__bloque h2 {
      margin: 0 0 8px;
      font-size: 16px;
    }
    .detalle__bloque dl {
      display: grid;
      grid-template-columns: max-content 1fr;
      gap: 6px 16px;
      margin: 0;
    }
    .detalle__bloque dt {
      color: var(--fs-color-texto-suave);
    }
    .detalle__bloque dd {
      margin: 0;
    }
    .detalle__texto {
      margin: 0 0 12px;
      white-space: pre-wrap;
    }
    .detalle__error {
      color: var(--fs-color-error, #b3261e);
    }
  `,
})
export class ReclamoDetallePage {
  private readonly api = inject(ReclamosService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly dialogo = inject(DialogoService);
  private readonly avisos = inject(ToastService);
  private readonly sesion = inject(SesionStore);
  private readonly sinAviso = new HttpContext().set(SIN_TOAST, true);
  private readonly id = Number(this.ruta.snapshot.paramMap.get('id'));

  protected readonly reclamo = signal<settingsApi.Reclamo | null>(null);
  protected readonly cargando = signal(true);
  protected readonly noEncontrado = signal(false);
  protected readonly trabajando = signal(false);
  protected readonly errorAccion = signal('');
  protected readonly respuesta = signal('');

  protected readonly esAdmin = computed(() => this.sesion.rol() === 'ADMIN');
  protected readonly puedeResponder = computed(
    () =>
      !this.trabajando() &&
      this.respuesta().trim().length >= MINIMO &&
      this.respuesta().length <= MAXIMO,
  );
  protected readonly etiquetaEstado = computed(
    () => ESTADOS_RECLAMO[this.reclamo()?.estado ?? '']?.etiqueta ?? this.reclamo()?.estado ?? '',
  );
  protected readonly claseEstado = computed(() =>
    claseInsignia(ESTADOS_RECLAMO[this.reclamo()?.estado ?? '']?.variante ?? 'neutra'),
  );
  protected readonly tipo = computed(() => TIPOS_RECLAMO[this.reclamo()?.tipo ?? ''] ?? '');

  constructor() {
    this.cargar();
  }

  protected tomar(): void {
    this.errorAccion.set('');
    this.trabajando.set(true);
    this.api.tomarReclamo(this.id, 'body', false, { context: this.sinAviso }).subscribe({
      next: (r) => {
        this.trabajando.set(false);
        this.reclamo.set(r.data!);
        this.avisos.mostrar('exito', 'El reclamo quedó en atención');
      },
      error: (e: unknown) => this.fallar(e),
    });
  }

  protected responder(): void {
    if (!this.puedeResponder()) return;
    this.dialogo
      .confirmar({
        titulo: '¿Enviar la respuesta?',
        mensaje: 'El consumidor la recibirá por correo y el reclamo no se podrá modificar.',
        accion: 'Responder reclamo',
      })
      .subscribe((confirmado) => {
        if (!confirmado) return;
        this.errorAccion.set('');
        this.trabajando.set(true);
        this.api
          .responderReclamo(this.id, { respuesta: this.respuesta().trim() }, 'body', false, {
            context: this.sinAviso,
          })
          .subscribe({
            next: (r) => {
              this.trabajando.set(false);
              this.reclamo.set(r.data!);
              this.avisos.mostrar('exito', 'Registramos la respuesta');
            },
            error: (e: unknown) => this.fallar(e),
          });
      });
  }

  private cargar(): void {
    this.api.obtenerReclamo(this.id, 'body', false, { context: this.sinAviso }).subscribe({
      next: (r) => {
        this.reclamo.set(r.data!);
        this.cargando.set(false);
      },
      error: (e: unknown) => {
        this.noEncontrado.set(e instanceof HttpErrorResponse && e.status === 404);
        this.cargando.set(false);
      },
    });
  }

  /** Un 409 (otra persona lo tomó o lo respondió) avisa y vuelve a leer el reclamo para mostrar su estado real. */
  private fallar(e: unknown): void {
    this.trabajando.set(false);
    this.errorAccion.set(
      comoApiError(e)?.mensaje ?? 'No pudimos completar la acción. Inténtalo de nuevo.',
    );
    if (e instanceof HttpErrorResponse && e.status === 409) this.cargar();
  }
}
