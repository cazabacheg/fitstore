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
import { ESTILOS_PANTALLA } from '../estilos-pantalla';
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
  styles: [
    ESTILOS_PANTALLA,
    `
      .detalle-pedido {
        padding-block: 32px 80px;
        display: grid;
        gap: 24px;
        align-content: start;
      }
      .fs-solo-lectores {
        position: absolute;
        width: 1px;
        height: 1px;
        overflow: hidden;
        clip-path: inset(50%);
        white-space: nowrap;
      }
      .fs-pago-pendiente {
        display: flex;
        gap: 16px;
        align-items: center;
        justify-content: space-between;
        flex-wrap: wrap;
        padding: 14px 16px;
        border-radius: var(--fs-radio-md);
        background: var(--fs-color-aviso-suave);
        color: var(--fs-color-aviso);
        font-weight: 600;
      }
      .fs-banner-exito {
        margin: 0;
        padding: 14px 16px;
        border-radius: var(--fs-radio-md);
        background: var(--fs-color-exito-suave);
        color: var(--fs-color-exito);
      }
      .fs-aviso-historial {
        margin: 0;
        padding: 10px 14px;
        border-radius: var(--fs-radio-md);
        background: var(--fs-color-aviso-suave);
        color: var(--fs-color-aviso);
        font-size: 14px;
      }
      .fs-linea-tiempo {
        list-style: none;
        margin: 0;
        padding: 0;
      }
      .fs-etapa {
        display: grid;
        grid-template-columns: 28px minmax(0, 1fr);
        gap: 14px;
        position: relative;
        padding-bottom: 22px;
      }
      .fs-etapa::before {
        content: '';
        position: absolute;
        left: 13px;
        top: 30px;
        bottom: 2px;
        width: 2px;
        background: var(--fs-color-linea);
      }
      .fs-etapa--hecha::before {
        background: var(--fs-color-texto);
      }
      .fs-etapa:last-child {
        padding-bottom: 0;
      }
      .fs-etapa:last-child::before {
        display: none;
      }
      .fs-etapa__punto {
        width: 28px;
        height: 28px;
        border-radius: var(--radio-pill);
        box-sizing: border-box;
        border: 2px solid var(--fs-color-linea);
        background: var(--fs-color-superficie);
        color: var(--fs-color-texto-suave);
        display: grid;
        place-items: center;
      }
      .fs-etapa__punto .fs-icono {
        font-size: 16px;
      }
      .fs-etapa--hecha .fs-etapa__punto {
        background: var(--fs-color-texto);
        border-color: var(--fs-color-texto);
        color: var(--fs-color-superficie);
      }
      .fs-etapa--actual .fs-etapa__punto {
        border-color: var(--fs-color-primario-texto);
        background: var(--fs-color-primario-suave);
        color: var(--fs-color-primario-texto);
      }
      .fs-etapa__texto {
        display: grid;
        gap: 2px;
        padding-top: 2px;
      }
      .fs-etapa__descripcion {
        font-size: 14px;
      }
      .pedido__items {
        list-style: none;
        margin: 0;
        padding: 0;
        display: grid;
        gap: 12px;
      }
      .pedido__items li {
        display: flex;
        gap: 12px;
        align-items: center;
      }
      .pedido__item-texto {
        flex: 1;
        min-width: 0;
        font-size: 14px;
        font-weight: 500;
      }
      .pedido__item-total {
        font-size: 14px;
        font-variant-numeric: tabular-nums;
      }
      .fs-resumen-montos {
        display: grid;
        gap: 12px;
      }
      .fs-resumen__fila {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        font-variant-numeric: tabular-nums;
      }
      .fs-resumen__fila--descuento {
        color: var(--fs-color-exito);
      }
      .fs-resumen__total {
        font-size: 17px;
      }
      @media (max-width: 720px) {
        .detalle-pedido {
          padding-block: 24px 48px;
        }
      }
    `,
  ],
  template: `
    <section class="fs-pagina detalle-pedido">
      <nav class="fs-migas" aria-label="Migas">
        <a routerLink="/cuenta/pedidos">‹ Mis pedidos</a>
      </nav>
      @switch (estado()) {
        @case ('cargando') {
          <fs-estado-cargando />
        }
        @case ('no-encontrado') {
          <section class="fs-estado" role="alert">
            <h1 class="fs-estado__titulo">No encontramos ese pedido</h1>
            <p class="fs-suave">Revisa el código o vuelve a tus pedidos.</p>
            <a fsBoton variante="primario" routerLink="/cuenta/pedidos">Ver mis pedidos</a>
          </section>
        }
        @case ('error') {
          <fs-estado-error titulo="No pudimos cargar tu pedido" (reintentar)="cargar()" />
        }
        @default {
          @let p = pedido()!;
          <div style="display: grid; gap: 8px">
            <header style="display: flex; gap: 12px; align-items: center; flex-wrap: wrap">
              <h1 class="fs-titulo-pagina">Pedido <span class="fs-codigo" style="font-size: inherit; font-weight: 700">{{ p.codigo }}</span></h1>
              <fs-etiqueta-estado [estado]="p.estado" />
            </header>
            <span class="fs-suave" style="font-size: 15px">Realizado el {{ p.creadoEn | fecha: 'fechaHora' }}</span>
          </div>

          @if (devolucion(); as monto) {
            <p class="fs-banner-exito" role="status">Te devolveremos {{ monto | precio }} al mismo medio de pago.</p>
          }

          @if (p.puedePagar && p.expiraEn) {
            <section class="fs-pago-pendiente">
              <fs-cuenta-regresiva [expiraEn]="p.expiraEn" />
              <a fsBoton variante="primario" [routerLink]="['/pago', p.codigo]">Pagar ahora</a>
            </section>
          }

          <div class="fs-dos-columnas">
            <section class="fs-tarjeta" aria-labelledby="seguimiento" style="display: grid; gap: 20px">
              <h2 id="seguimiento" class="fs-titulo-seccion">Seguimiento</h2>
              <ol class="fs-linea-tiempo">
                @for (e of etapas(); track e.clave) {
                  <li
                    class="fs-etapa"
                    [class.fs-etapa--hecha]="e.situacion === 'hecha'"
                    [class.fs-etapa--futura]="e.situacion === 'futura'"
                    [class.fs-etapa--actual]="e.situacion === 'actual'"
                    [style.opacity]="e.situacion === 'futura' ? 0.5 : 1"
                  >
                    <span class="fs-etapa__punto" aria-hidden="true">
                      @if (e.situacion === 'hecha') {
                        <span class="fs-icono">check</span>
                      } @else if (e.situacion === 'actual') {
                        <span class="fs-icono">{{ e.final ? 'close' : 'radio_button_checked' }}</span>
                      }
                    </span>
                    <div class="fs-etapa__texto">
                      <div class="fs-entre" style="align-items: baseline; flex-wrap: wrap">
                        <strong class="fs-etapa__nombre">{{ e.nombre }}</strong>
                        @if (e.fecha) {
                          <span class="fs-suave fs-etapa__fecha">{{ e.fecha | fecha: 'fechaHora' }}</span>
                        }
                      </div>
                      <div class="fs-suave fs-etapa__descripcion">{{ e.descripcion }}</div>
                    </div>
                  </li>
                }
              </ol>
              @for (a of avisos(); track a.fecha + a.texto) {
                <p class="fs-aviso-historial" role="status">{{ a.fecha | fecha: 'fechaHora' }} · {{ a.texto }}</p>
              }
            </section>

            <div style="display: grid; gap: 20px; align-content: start">
              <section class="fs-tarjeta" aria-labelledby="productos" style="display: grid; gap: 12px">
                <h2 id="productos" class="fs-titulo-seccion">Productos</h2>
                <ul class="pedido__items">
                  @for (i of p.items; track i.varianteId) {
                    <li>
                      <span class="fs-miniatura" style="width: 44px">
                        @if (i.imagenUrl) {
                          <img [src]="i.imagenUrl" alt="" width="44" height="55" />
                        } @else {
                          <span class="fs-icono" style="font-size: 16px" aria-hidden="true">checkroom</span>
                        }
                      </span>
                      <span class="pedido__item-texto">{{ i.producto }} · {{ i.talla }} · {{ i.color }} × {{ i.cantidad }}</span>
                      <span class="pedido__item-total">{{ i.total | precio }}</span>
                    </li>
                  }
                </ul>
                <hr class="fs-linea" />
                <div class="fs-resumen-montos" aria-labelledby="montos" role="group">
                  <h3 id="montos" class="fs-solo-lectores">Montos</h3>
                  <div class="fs-resumen__fila"><span>Subtotal</span><span>{{ p.subtotal | precio }}</span></div>
                  <div class="fs-resumen__fila fs-resumen__fila--descuento">
                    <span>Descuento</span><span>-{{ p.descuentoTotal | precio }}</span>
                  </div>
                  <div class="fs-resumen__fila"><span>Envío</span><span>{{ p.costoEnvio | precio }}</span></div>
                  <div class="fs-resumen__fila fs-resumen__total"><strong>Total</strong><strong>{{ p.total | precio }}</strong></div>
                </div>
                @if (p.boletaUrl) {
                  <button type="button" fsBoton variante="contorno" [bloque]="true" (click)="descargarBoleta(p.boletaUrl)">
                    <span class="fs-icono" aria-hidden="true">receipt_long</span>Descargar boleta
                  </button>
                }
                @if (p.puedeCancelar) {
                  <button type="button" fsBoton variante="peligro" [bloque]="true" (click)="confirmando.set(true)">
                    Cancelar pedido
                  </button>
                }
              </section>

              <section class="fs-tarjeta" aria-labelledby="entrega" style="display: grid; gap: 6px">
                <h2 id="entrega" class="fs-titulo-seccion">Entrega</h2>
                @if (p.direccion; as d) {
                  <span>{{ d.direccion }}, {{ d.distrito }}, {{ d.provincia }}, {{ d.departamento }}</span>
                } @else if (p.envio.tiendaDireccion) {
                  <span>Recojo en tienda: {{ p.envio.tiendaDireccion }}</span>
                }
                <span class="fs-suave">Recibe {{ p.destinatario.nombres }} {{ p.destinatario.apellidos }}</span>
              </section>
            </div>
          </div>
        }
      }
    </section>
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
