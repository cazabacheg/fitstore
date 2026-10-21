import { HttpClient, HttpContext } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FechaPipe, PrecioPipe, SIN_TOAST, ToastService, comoApiError, orderApi } from 'shared';
import { PedidosAdminService } from 'shared/api/order-service';
import { EstadoCargando } from '../../ui/estados/estado-cargando';
import { EstadoError } from '../../ui/estados/estado-error';
import { InsigniaEstado } from '../../ui/insignia/insignia-estado';
import {
  DialogoAccionPedidoService,
  type DatosAccionPedido,
  type ResultadoAccionPedido,
} from './dialogo-accion-pedido';

const MAXIMO_COMENTARIO = 300;

/**
 * Detalle de un pedido para el operador (P-4-06): cliente, entrega, productos con SKU, montos, pago, boleta, historial
 * y solo los botones que la API permite según el estado y la modalidad (CA-4-43 a CA-4-50, CA-4-60).
 */
@Component({
  selector: 'fs-detalle-pedido-admin-page',
  imports: [RouterLink, FechaPipe, PrecioPipe, InsigniaEstado, EstadoCargando, EstadoError],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a routerLink="/pedidos" class="fs-suave">‹ Pedidos</a>
    @switch (estado()) {
      @case ('cargando') {
        <fs-estado-cargando />
      }
      @case ('no-encontrado') {
        <section class="fs-estado" role="alert">
          <h2 class="fs-estado__titulo">No encontramos ese pedido</h2>
          <a class="fs-btn fs-btn--contorno" routerLink="/pedidos">Volver a pedidos</a>
        </section>
      }
      @case ('error') {
        <fs-estado-error titulo="No pudimos cargar el pedido" (reintentar)="cargar()" />
      }
      @default {
        @let p = pedido()!;
        <header class="fs-cabecera-pedido">
          <h1>Pedido {{ p.codigo }}</h1>
          <fs-insignia-estado tipo="pedido" [estado]="p.estado" />
          @if (p.estadoReembolso !== 'NINGUNO') {
            <span class="fs-insignia fs-insignia--marca">
              {{ p.estadoReembolso === 'TOTAL' ? 'Reembolsado' : 'Reembolso parcial: ' + (p.montoReembolsado | precio) }}
            </span>
          }
          <span class="fs-suave">Creado el {{ p.creadoEn | fecha: 'fechaHora' }}</span>
        </header>

        @if (conflicto()) {
          <div class="fs-banner fs-banner--aviso" role="alert">
            El pedido cambió; recarga la página.
            <button type="button" class="fs-btn fs-btn--contorno fs-btn--sm" (click)="cargar()">Recargar</button>
          </div>
        }

        <section class="fs-acciones" aria-label="Acciones">
          @for (t of p.transicionesPermitidas; track t.estado) {
            <button type="button" class="fs-btn fs-btn--primario" [disabled]="enCurso()" (click)="avanzar(t)">
              {{ t.etiqueta }}
            </button>
          }
          @if (p.puedeCancelarStaff) {
            <button type="button" class="fs-btn fs-btn--peligro" [disabled]="enCurso()" (click)="cancelar()">
              Cancelar pedido
            </button>
          }
          @if (p.transicionesPermitidas.length === 0 && !p.puedeCancelarStaff) {
            <span class="fs-suave">Pedido final: no tiene acciones.</span>
          }
        </section>

        <div class="fs-grid-detalle">
          <section aria-labelledby="cliente">
            <h2 id="cliente">Cliente y destinatario</h2>
            <p>
              <a [routerLink]="['/pedidos']" [queryParams]="{ clienteId: p.clienteId }">Ver sus pedidos</a>
            </p>
            <p>{{ p.destinatario.nombres }} {{ p.destinatario.apellidos }} · DNI {{ p.destinatario.dni }}</p>
            <p>{{ p.destinatario.telefono }} · {{ p.destinatario.email }}</p>
          </section>

          <section aria-labelledby="entrega">
            <h2 id="entrega">Entrega</h2>
            <p>{{ nombreModalidad() }}</p>
            @if (p.direccion; as d) {
              <p>{{ d.direccion }}, {{ d.distrito }}, {{ d.provincia }}, {{ d.departamento }}</p>
              @if (d.referencia) {
                <p class="fs-suave">{{ d.referencia }}</p>
              }
            }
            @if (p.envio.agenciaNombre) {
              <p>
                Agencia {{ p.envio.agenciaNombre }}
                @if (p.envio.codigoSeguimiento) {
                  · código {{ p.envio.codigoSeguimiento }}
                }
              </p>
            }
            @if (p.notas) {
              <p class="fs-suave">Notas: {{ p.notas }}</p>
            }
          </section>
        </div>

        <section aria-labelledby="productos">
          <h2 id="productos">Productos</h2>
          <table class="fs-tabla fs-tabla--plana">
            <thead>
              <tr><th>SKU</th><th>Producto</th><th>Cant.</th><th>Precio</th><th>Total</th></tr>
            </thead>
            <tbody>
              @for (i of p.items; track i.varianteId) {
                <tr>
                  <td><a [routerLink]="['/inventario', i.varianteId]">{{ i.sku }}</a></td>
                  <td>{{ i.producto }} · {{ i.talla }} · {{ i.color }}</td>
                  <td>{{ i.cantidad }}</td>
                  <td>{{ i.precioUnitario | precio }}</td>
                  <td>{{ i.total | precio }}</td>
                </tr>
              }
            </tbody>
          </table>
        </section>

        <section aria-labelledby="montos">
          <h2 id="montos">Montos y pago</h2>
          <p>Subtotal {{ p.subtotal | precio }} · Descuento -{{ p.descuentoTotal | precio }} · Envío {{ p.costoEnvio | precio }}</p>
          <p><strong>Total {{ p.total | precio }}</strong></p>
          @if (p.pago; as pago) {
            <p class="bloque-pago">Pagado con {{ pago.proveedor }} · {{ pago.medio }}
              @if (pago.pagadoEn) { el {{ pago.pagadoEn | fecha: 'fechaHora' }} }
            </p>
          } @else {
            <p class="fs-suave">Sin pago registrado.</p>
          }
          @if (p.boletaUrl) {
            <button type="button" class="fs-btn fs-btn--contorno" (click)="descargarBoleta(p.boletaUrl)">
              Descargar boleta
            </button>
          }
        </section>

        <section aria-labelledby="historial">
          <h2 id="historial">Historial</h2>
          <ol class="fs-historial">
            @for (h of p.historial; track h.fecha + h.tipo + $index) {
              <li>
                <span class="fs-suave">{{ h.fecha | fecha: 'fechaHora' }}</span>
                @if (h.estadoNuevo) {
                  <fs-insignia-estado tipo="pedido" [estado]="h.estadoNuevo" />
                }
                {{ h.comentario }}
                <span class="fs-suave">· {{ h.actorNombre ?? (h.actorTipo === 'SISTEMA' ? 'Sistema' : 'Cliente') }}</span>
              </li>
            }
          </ol>
        </section>
      }
    }
  `,
})
export class DetallePedidoAdminPage {
  private readonly api = inject(PedidosAdminService);
  private readonly cliente = inject(HttpClient);
  private readonly dialogos = inject(DialogoAccionPedidoService);
  private readonly toasts = inject(ToastService);
  private readonly id = inject(ActivatedRoute).snapshot.paramMap.get('id') ?? '';
  private readonly contexto = () => ({ context: new HttpContext().set(SIN_TOAST, true) });

  protected readonly estado = signal<'cargando' | 'listo' | 'no-encontrado' | 'error'>('cargando');
  protected readonly pedido = signal<orderApi.PedidoAdmin | null>(null);
  protected readonly enCurso = signal(false);
  protected readonly conflicto = signal(false);
  protected readonly nombreModalidad = computed(
    () =>
      ({ DELIVERY_LOCAL: 'Delivery local', AGENCIA: 'Envío por agencia', RECOJO: 'Recojo en tienda' })[
        this.pedido()?.modalidad ?? 'RECOJO'
      ],
  );

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.estado.set('cargando');
    this.conflicto.set(false);
    this.api.obtenerPedidoAdmin(this.id, 'body', false, this.contexto()).subscribe({
      next: (r) => {
        this.pedido.set(r.data ?? null);
        this.estado.set(r.data ? 'listo' : 'error');
      },
      error: (e: { status?: number }) => this.estado.set(e.status === 404 ? 'no-encontrado' : 'error'),
    });
  }

  /** Cada botón abre el diálogo que le toca: comentario opcional, DNI verificado (recojo) o motivo obligatorio (no recogido). */
  protected avanzar(t: orderApi.TransicionPermitida): void {
    const p = this.pedido()!;
    const datos: DatosAccionPedido = t.requiereDni
      ? {
          titulo: `Marcar ${p.codigo} como recogido`,
          mensaje: 'El pedido pasa a ENTREGADO.',
          accion: 'Marcar recogido',
          confirmacion: 'Verifiqué el DNI del destinatario',
        }
      : t.requiereComentario
        ? {
            titulo: `Marcar ${p.codigo} como no recogido`,
            mensaje: 'El pedido pasa a DEVUELTO y queda disponible para reembolsar.',
            accion: 'Marcar no recogido',
            peligro: true,
            comentario: { etiqueta: 'Motivo', obligatorio: true, maximo: MAXIMO_COMENTARIO },
          }
        : {
            titulo: `${t.etiqueta}: ${p.codigo}`,
            accion: t.etiqueta,
            comentario: { etiqueta: 'Comentario', obligatorio: false, maximo: MAXIMO_COMENTARIO },
          };
    this.dialogos.abrir(datos).subscribe((resultado) => {
      if (resultado) this.ejecutar(t, resultado);
    });
  }

  private ejecutar(t: orderApi.TransicionPermitida, resultado: ResultadoAccionPedido): void {
    const p = this.pedido()!;
    this.enCurso.set(true);
    this.api
      .avanzarEstadoPedido(
        p.id,
        {
          nuevoEstado: t.estado as orderApi.AvanzarEstadoRequest['nuevoEstado'],
          comentario: resultado.comentario,
          dniVerificado: t.requiereDni ? resultado.confirmado : null,
          version: p.version,
        },
        'body',
        false,
        this.contexto(),
      )
      .subscribe(this.alResponder());
  }

  protected cancelar(): void {
    const p = this.pedido()!;
    this.dialogos
      .abrir({
        titulo: `¿Cancelar el pedido ${p.codigo}?`,
        mensaje: p.pago ? 'Como ya está pagado, payment devolverá el dinero y catalog reingresará el stock.' : undefined,
        accion: 'Cancelar pedido',
        peligro: true,
        comentario: { etiqueta: 'Motivo', obligatorio: true, maximo: MAXIMO_COMENTARIO },
      })
      .subscribe((resultado) => {
        if (!resultado?.comentario) return;
        this.enCurso.set(true);
        this.api
          .cancelarPedidoStaff(p.id, { motivo: resultado.comentario, version: p.version }, 'body', false, this.contexto())
          .subscribe(this.alResponder());
      });
  }

  /** Respuesta común: el detalle actualizado, o el aviso de conflicto (409) o del error. */
  private alResponder() {
    return {
      next: (r: orderApi.RespuestaPedidoAdmin) => {
        this.enCurso.set(false);
        this.conflicto.set(false);
        if (r.data) this.pedido.set(r.data);
      },
      error: (e: unknown) => {
        this.enCurso.set(false);
        const api = comoApiError(e);
        if (api?.codigo === 'CONFLICTO_VERSION') this.conflicto.set(true);
        else this.toasts.mostrar('error', api?.mensaje ?? 'No pudimos completar la acción');
      },
    };
  }

  protected descargarBoleta(url: string): void {
    this.cliente.get(url, { responseType: 'blob' }).subscribe({
      next: (archivo) => {
        const enlace = document.createElement('a');
        enlace.href = URL.createObjectURL(archivo);
        enlace.download = `boleta-${this.pedido()?.codigo ?? 'pedido'}.pdf`;
        enlace.click();
        URL.revokeObjectURL(enlace.href);
      },
      error: () => this.toasts.mostrar('error', 'No pudimos descargar la boleta'),
    });
  }
}
