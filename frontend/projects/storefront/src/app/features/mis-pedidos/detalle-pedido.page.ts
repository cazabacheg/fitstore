import { HttpClient, HttpContext } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FechaPipe, PrecioPipe, SIN_TOAST, ToastService, comoApiError, orderApi } from 'shared';
import { MisPedidosService } from 'shared/api/order-service';
import { FsBoton } from '../../ui/boton/boton';
import { DialogoConfirmacion } from '../../ui/dialogo/dialogo-confirmacion';
import { EstadoCargando, EstadoError } from '../../ui/estados/estados';
import { avisosDelHistorial, construirEtapas } from './etapas';
import { EtiquetaEstado } from './etiqueta-estado';

/** Cuenta regresiva hasta el vencimiento del pago ("Te quedan 24:10"). */
@Component({
  selector: 'fs-cuenta-regresiva',
  template: `<span class="fs-cuenta-regresiva" role="timer">{{ texto() }}</span>`,
})
export class CuentaRegresiva {
  readonly expiraEn = input.required<string>();

  private readonly ahora = signal(Date.now());
  protected readonly texto = computed(() => {
    const falta = Math.max(0, Date.parse(this.expiraEn()) - this.ahora());
    if (falta === 0) return 'El plazo para pagar venció';
    const segundos = Math.floor(falta / 1000);
    const mm = String(Math.floor(segundos / 60)).padStart(2, '0');
    const ss = String(segundos % 60).padStart(2, '0');
    return `Te quedan ${mm}:${ss} para pagar`;
  });

  constructor() {
    const intervalo = setInterval(() => this.ahora.set(Date.now()), 1000);
    inject(DestroyRef).onDestroy(() => clearInterval(intervalo));
  }
}

/** Detalle de mi pedido (P-4-04): línea de tiempo, productos, entrega, montos, boleta y cancelación (CA-4-34 a CA-4-39). */
@Component({
  selector: 'fs-detalle-pedido-page',
  imports: [
    RouterLink,
    FechaPipe,
    PrecioPipe,
    FsBoton,
    EtiquetaEstado,
    CuentaRegresiva,
    DialogoConfirmacion,
    EstadoCargando,
    EstadoError,
  ],
  template: `
    <a routerLink="/cuenta/pedidos" class="fs-suave">‹ Mis pedidos</a>
    @switch (estado()) {
      @case ('cargando') {
        <fs-estado-cargando />
      }
      @case ('no-encontrado') {
        <section class="fs-estado" role="alert">
          <h1>No encontramos ese pedido</h1>
          <p class="fs-suave">Revisa el código o vuelve a tus pedidos.</p>
          <a fsBoton variante="primario" routerLink="/cuenta/pedidos">Ver mis pedidos</a>
        </section>
      }
      @case ('error') {
        <fs-estado-error titulo="No pudimos cargar tu pedido" (reintentar)="cargar()" />
      }
      @default {
        @let p = pedido()!;
        <header style="display: flex; gap: var(--espacio-4); align-items: center; flex-wrap: wrap">
          <h1 style="margin: 0">Pedido {{ p.codigo }}</h1>
          <fs-etiqueta-estado [estado]="p.estado" />
        </header>
        <p class="fs-suave">Realizado el {{ p.creadoEn | fecha: 'fechaHora' }}</p>

        @if (devolucion(); as monto) {
          <p class="fs-banner-exito" role="status">Te devolveremos {{ monto | precio }} al mismo medio de pago.</p>
        }

        @if (p.puedePagar && p.expiraEn) {
          <section class="fs-pago-pendiente" style="display: flex; gap: var(--espacio-4); align-items: center; flex-wrap: wrap">
            <fs-cuenta-regresiva [expiraEn]="p.expiraEn" />
            <a fsBoton variante="primario" [routerLink]="['/pago', p.codigo]">Pagar ahora</a>
          </section>
        }

        <section aria-labelledby="seguimiento">
          <h2 id="seguimiento">Seguimiento</h2>
          <ol class="fs-linea-tiempo" style="list-style: none; padding: 0; display: grid; gap: var(--espacio-4)">
            @for (e of etapas(); track e.clave) {
              <li
                class="fs-etapa"
                [class.fs-etapa--futura]="e.situacion === 'futura'"
                [class.fs-etapa--actual]="e.situacion === 'actual'"
                [style.opacity]="e.situacion === 'futura' ? 0.5 : 1"
              >
                <strong class="fs-etapa__nombre">{{ e.nombre }}</strong>
                @if (e.fecha) {
                  <span class="fs-suave fs-etapa__fecha"> · {{ e.fecha | fecha: 'fechaHora' }}</span>
                }
                <div class="fs-suave fs-etapa__descripcion">{{ e.descripcion }}</div>
              </li>
            }
          </ol>
          @for (a of avisos(); track a.fecha + a.texto) {
            <p class="fs-aviso-historial" role="status">{{ a.fecha | fecha: 'fechaHora' }} · {{ a.texto }}</p>
          }
        </section>

        <section aria-labelledby="productos">
          <h2 id="productos">Productos</h2>
          <ul style="list-style: none; padding: 0">
            @for (i of p.items; track i.varianteId) {
              <li style="display: flex; gap: var(--espacio-3); align-items: center">
                @if (i.imagenUrl) {
                  <img [src]="i.imagenUrl" alt="" width="56" height="56" />
                }
                <span style="flex: 1">{{ i.producto }} · {{ i.talla }} · {{ i.color }} × {{ i.cantidad }}</span>
                <span>{{ i.total | precio }}</span>
              </li>
            }
          </ul>
        </section>

        <section aria-labelledby="entrega">
          <h2 id="entrega">Entrega</h2>
          @if (p.direccion; as d) {
            <p>{{ d.direccion }}, {{ d.distrito }}, {{ d.provincia }}, {{ d.departamento }}</p>
          } @else if (p.envio.tiendaDireccion) {
            <p>Recojo en tienda: {{ p.envio.tiendaDireccion }}</p>
          }
          <p class="fs-suave">Recibe {{ p.destinatario.nombres }} {{ p.destinatario.apellidos }}</p>
        </section>

        <section aria-labelledby="montos" class="fs-resumen-montos" style="display: grid; gap: var(--espacio-2)">
          <h2 id="montos">Montos</h2>
          <div class="fs-resumen__fila"><span>Subtotal</span><span>{{ p.subtotal | precio }}</span></div>
          <div class="fs-resumen__fila"><span>Descuento</span><span>-{{ p.descuentoTotal | precio }}</span></div>
          <div class="fs-resumen__fila"><span>Envío</span><span>{{ p.costoEnvio | precio }}</span></div>
          <div class="fs-resumen__fila"><strong>Total</strong><strong>{{ p.total | precio }}</strong></div>
        </section>

        @if (p.boletaUrl) {
          <button type="button" fsBoton variante="contorno" (click)="descargarBoleta(p.boletaUrl)">
            Descargar boleta
          </button>
        }
        @if (p.puedeCancelar) {
          <button type="button" fsBoton variante="peligro" (click)="confirmando.set(true)">
            Cancelar pedido
          </button>
        }
      }
    }
    <fs-dialogo-confirmacion
      titulo="¿Cancelar tu pedido?"
      [texto]="textoCancelar()"
      textoConfirmar="Sí, cancelar pedido"
      textoVolver="No, volver"
      [peligro]="true"
      [abierto]="confirmando()"
      (cancelado)="confirmando.set(false)"
      (confirmado)="cancelar()"
    />
  `,
})
export class DetallePedidoPage {
  private readonly api = inject(MisPedidosService);
  private readonly cliente = inject(HttpClient);
  private readonly toasts = inject(ToastService);
  private readonly codigo = inject(ActivatedRoute).snapshot.paramMap.get('codigo') ?? '';
  private readonly contexto = () => ({ context: new HttpContext().set(SIN_TOAST, true) });

  protected readonly estado = signal<'cargando' | 'listo' | 'no-encontrado' | 'error'>('cargando');
  protected readonly pedido = signal<orderApi.PedidoDetalle | null>(null);
  protected readonly confirmando = signal(false);
  /** Monto que se devolverá, tras cancelar un pedido que ya estaba pagado (CA-4-38). */
  protected readonly devolucion = signal<number | null>(null);

  protected readonly etapas = computed(() => {
    const p = this.pedido();
    return p ? construirEtapas(p) : [];
  });
  protected readonly avisos = computed(() => avisosDelHistorial(this.pedido()?.historial ?? []));
  protected readonly textoCancelar = computed(() =>
    this.pedido()?.estado === 'PAGADO'
      ? `Te devolveremos ${formatoMonto(this.pedido()?.total ?? 0)} al mismo medio de pago.`
      : 'El stock apartado se liberará.',
  );

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.estado.set('cargando');
    this.api.obtenerMiPedido(this.codigo, 'body', false, this.contexto()).subscribe({
      next: (r) => {
        this.pedido.set(r.data ?? null);
        this.estado.set(r.data ? 'listo' : 'error');
      },
      error: (e: { status?: number }) => this.estado.set(e.status === 404 ? 'no-encontrado' : 'error'),
    });
  }

  protected cancelar(): void {
    const antes = this.pedido();
    this.confirmando.set(false);
    this.api.cancelarMiPedido(this.codigo, 'body', false, this.contexto()).subscribe({
      next: (r) => {
        this.pedido.set(r.data ?? antes);
        if (antes?.estado === 'PAGADO') this.devolucion.set(antes.total);
      },
      error: (e: unknown) =>
        this.toasts.mostrar('error', comoApiError(e)?.mensaje ?? 'No pudimos cancelar el pedido'),
    });
  }

  /** La boleta se descarga con la sesión (el enlace de payment es autenticado): se pide como archivo y se guarda. */
  protected descargarBoleta(url: string): void {
    this.cliente.get(url, { responseType: 'blob' }).subscribe({
      next: (archivo) => {
        const enlace = document.createElement('a');
        enlace.href = URL.createObjectURL(archivo);
        enlace.download = `boleta-${this.codigo}.pdf`;
        enlace.click();
        URL.revokeObjectURL(enlace.href);
      },
      error: () => this.toasts.mostrar('error', 'No pudimos descargar la boleta'),
    });
  }
}

function formatoMonto(monto: number): string {
  return `S/ ${monto.toFixed(2)}`;
}
