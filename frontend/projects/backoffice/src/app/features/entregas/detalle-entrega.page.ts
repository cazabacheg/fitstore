import { HttpContext } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FechaPipe, SIN_TOAST, ToastService, deliveryApi } from 'shared';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { EstadoCargando } from '../../ui/estados/estado-cargando';
import { InsigniaEstado } from '../../ui/insignia/insignia-estado';
import { etiquetaAccion } from './acciones-entrega';
import { AvanceEntrega } from './avance-entrega';

/**
 * Detalle de una entrega (P-6-02, `/entregas/:id`): destinatario, dirección, ítems, las acciones que permite su estado
 * (`accionesPermitidas`, E5) y el seguimiento con fecha, actor, observación e intento. DNI y teléfono del
 * destinatario son solo para staff.
 */
@Component({
  selector: 'fs-detalle-entrega',
  imports: [RouterLink, EncabezadoPagina, EstadoCargando, InsigniaEstado, FechaPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
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
    .detalle__acciones {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
    .seguimiento {
      margin: 0;
      padding: 0;
      list-style: none;
      display: grid;
      gap: 12px;
    }
  `,
  template: `
    <fs-encabezado-pagina [titulo]="entrega()?.codigoPedido ?? 'Entrega'">
      <a accion class="fs-btn fs-btn--contorno" routerLink="/entregas">Volver a entregas</a>
    </fs-encabezado-pagina>

    @if (cargando()) {
      <fs-estado-cargando [lineas]="6" />
    } @else if (noEncontrada()) {
      <section class="fs-estado" data-testid="no-encontrada">
        <h2 class="fs-estado__titulo">No encontramos esta entrega</h2>
        <a class="fs-btn fs-btn--primario" routerLink="/entregas">Ir a entregas</a>
      </section>
    } @else if (entrega(); as e) {
      <div class="detalle">
        <section class="detalle__bloque" aria-label="Resumen">
          <dl>
            <dt>Estado</dt>
            <dd data-testid="estado"><fs-insignia-estado tipo="entrega" [estado]="e.estado" /></dd>
            <dt>Modalidad</dt>
            <dd>{{ e.modalidad === 'AGENCIA' ? 'Agencia' : 'Delivery local' }}</dd>
            <dt>Pedido</dt>
            <dd>
              <a [routerLink]="['/pedidos', e.pedidoId]">{{ e.codigoPedido }}</a>
            </dd>
            @if (e.fechaProgramada) {
              <dt>Fecha programada</dt>
              <dd>{{ e.fechaProgramada | fecha }}</dd>
            }
            @if (e.repartidor) {
              <dt>Repartidor</dt>
              <dd>{{ e.repartidor.nombres }}</dd>
            }
            @if (e.intento > 0) {
              <dt>Intento</dt>
              <dd>{{ e.intento }}</dd>
            }
            @if (e.agenciaNombre) {
              <dt>Agencia</dt>
              <dd>{{ e.agenciaNombre }}</dd>
            }
            @if (e.codigoSeguimiento) {
              <dt>Código de seguimiento</dt>
              <dd>{{ e.codigoSeguimiento }}</dd>
            }
            @if (e.claveRecojo) {
              <dt>Clave de recojo</dt>
              <dd>{{ e.claveRecojo }}</dd>
            }
            @if (e.urlSeguimiento) {
              <dt>Rastreo</dt>
              <dd>
                <a [href]="e.urlSeguimiento" target="_blank" rel="noopener noreferrer"
                  >Seguir el envío</a
                >
              </dd>
            }
          </dl>
        </section>

        <section class="detalle__bloque" aria-label="Destinatario">
          <h2>Destinatario</h2>
          <dl>
            <dt>Nombre</dt>
            <dd>{{ e.destinatario.nombres }} {{ e.destinatario.apellidos }}</dd>
            <dt>DNI</dt>
            <dd>{{ e.destinatario.dni }}</dd>
            <dt>Teléfono</dt>
            <dd>{{ e.destinatario.telefono }}</dd>
          </dl>
        </section>

        @if (e.direccion; as d) {
          <section class="detalle__bloque" aria-label="Dirección">
            <h2>Dirección</h2>
            <dl>
              <dt>Dirección</dt>
              <dd>{{ d.direccion }}</dd>
              <dt>Distrito</dt>
              <dd>{{ d.distrito }}, {{ d.provincia }}, {{ d.departamento }}</dd>
              @if (d.referencia) {
                <dt>Referencia</dt>
                <dd>{{ d.referencia }}</dd>
              }
            </dl>
          </section>
        }

        <section class="detalle__bloque" aria-label="Ítems">
          <h2>Ítems</h2>
          <p>{{ e.resumenItems }}</p>
          <p class="fs-suave">{{ e.bultos }} {{ e.bultos === 1 ? 'bulto' : 'bultos' }}</p>
        </section>

        @if (e.accionesPermitidas.length > 0) {
          <section class="detalle__bloque" aria-label="Acciones">
            <h2>Acciones</h2>
            <div class="detalle__acciones">
              @for (accion of e.accionesPermitidas; track accion) {
                <button
                  type="button"
                  class="fs-btn"
                  [class.fs-btn--primario]="accion === e.siguienteAccion"
                  [class.fs-btn--contorno]="accion !== e.siguienteAccion"
                  [disabled]="ocupada()"
                  (click)="ejecutar(accion)"
                >
                  {{ etiqueta(accion, e.modalidad) }}
                </button>
              }
            </div>
          </section>
        }

        <section class="detalle__bloque" aria-label="Seguimiento">
          <h2>Seguimiento</h2>
          <ol class="seguimiento">
            @for (ev of e.eventos; track $index) {
              <li data-testid="evento">
                <fs-insignia-estado tipo="entrega" [estado]="ev.estado" />
                {{ ev.fecha | fecha: 'fechaHora' }} · {{ ev.actorNombre ?? 'Sistema' }}
                @if (ev.intento > 0) {
                  · intento {{ ev.intento }}
                }
                @if (ev.observacion) {
                  <div class="fs-suave">{{ ev.observacion }}</div>
                }
              </li>
            }
          </ol>
        </section>
      </div>
    }
  `,
})
export class DetalleEntregaPage {
  private readonly api = inject(deliveryApi.EntregasService);
  private readonly avance = inject(AvanceEntrega);
  private readonly toasts = inject(ToastService);
  private readonly id = inject(ActivatedRoute).snapshot.paramMap.get('id') ?? '';

  protected readonly entrega = signal<deliveryApi.Entrega | null>(null);
  protected readonly cargando = signal(true);
  protected readonly noEncontrada = signal(false);
  protected readonly ocupada = signal(false);
  protected readonly etiqueta = etiquetaAccion;

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.api
      .obtenerEntrega(this.id, 'body', false, { context: new HttpContext().set(SIN_TOAST, true) })
      .subscribe({
        next: (r) => {
          this.entrega.set((r.data as deliveryApi.Entrega | undefined) ?? null);
          this.noEncontrada.set(!r.data);
          this.cargando.set(false);
        },
        error: () => {
          this.noEncontrada.set(true);
          this.cargando.set(false);
        },
      });
  }

  protected ejecutar(accion: string): void {
    const actual = this.entrega();
    if (!actual) return;
    this.ocupada.set(true);
    this.avance.ejecutar(actual, accion).subscribe({
      next: (nueva) => {
        this.ocupada.set(false);
        if (!nueva) return;
        this.entrega.set(nueva);
        this.toasts.mostrar('exito', 'Entrega actualizada');
      },
      // Un 409 (otro operador la cambió) ya se avisó por el interceptor: se vuelve a leer la entrega
      error: () => {
        this.ocupada.set(false);
        this.cargar();
      },
    });
  }
}
