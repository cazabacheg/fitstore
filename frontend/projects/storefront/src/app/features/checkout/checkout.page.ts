import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { PrecioPipe, SIN_TOAST, authApi, comoApiError, deliveryApi, orderApi } from 'shared';
import { ClienteService } from 'shared/api/auth-service';
import { EnviosService } from 'shared/api/delivery-service';
import { CheckoutService } from 'shared/api/order-service';
import { ConfiguracionPublicaService } from 'shared/api/settings-service';
import { FsBoton } from '../../ui/boton/boton';
import { EstadoCargando, EstadoError } from '../../ui/estados/estados';
import { ESTILOS_PANTALLA } from '../catalogo/estilos-pantalla';
import { DialogoDireccionService } from '../cuenta/direcciones/dialogo-direccion.service';

/** Máximo de direcciones por cliente (invariante 4 de auth). */
export const MAXIMO_DIRECCIONES = 10;
const MAXIMO_NOTAS = 300;

type Modalidad = orderApi.Modalidad;

const NOMBRE_MODALIDAD: Record<Modalidad, string> = {
  DELIVERY_LOCAL: 'Delivery a domicilio',
  AGENCIA: 'Envío por agencia',
  RECOJO: 'Recojo en tienda',
};

/** Checkout en una sola página (P-4-02, O1): dirección, entrega y revisión, con el resumen al costado (CA-4-13 a CA-4-24). */
@Component({
  selector: 'fs-checkout-page',
  imports: [RouterLink, PrecioPipe, FsBoton, EstadoCargando, EstadoError],
  styles: [
    ESTILOS_PANTALLA,
    `
      .checkout {
        padding-block: 40px 80px;
        display: grid;
        gap: 28px;
      }
      .checkout__pasos {
        display: grid;
        gap: 32px;
        align-content: start;
      }
      .ck-paso {
        display: grid;
        gap: 16px;
        padding-bottom: 32px;
        border-bottom: 1px solid var(--fs-color-linea);
      }
      .ck-paso--ultimo {
        border-bottom: 0;
      }
      .ck-paso__titulo {
        display: flex;
        gap: 12px;
        align-items: center;
        margin: 0;
        font-size: 17px;
        line-height: 24px;
        font-weight: 600;
      }
      .ck-num {
        width: 28px;
        height: 28px;
        border-radius: var(--radio-pill);
        background: var(--fs-color-texto);
        color: var(--fs-color-superficie);
        display: grid;
        place-items: center;
        font-size: 13px;
        font-weight: 700;
      }
      .ck-opciones {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 12px;
      }
      .fs-opcion {
        display: flex;
        gap: 12px;
        align-items: flex-start;
        padding: 16px;
        border: 1px solid var(--fs-color-borde-control);
        border-radius: var(--fs-radio-md);
        background: var(--fs-color-superficie);
        cursor: pointer;
      }
      .fs-opcion--activa {
        border: 2px solid var(--fs-color-texto);
        padding: 15px;
      }
      .fs-opcion input[type='radio'] {
        width: 18px;
        height: 18px;
        flex: none;
        margin: 3px 0 0;
        accent-color: var(--fs-color-texto);
      }
      .ck-opcion__texto {
        flex: 1;
        min-width: 0;
      }
      .ck-items {
        list-style: none;
        margin: 0;
        padding: 0;
        display: grid;
        gap: 12px;
      }
      .ck-items li {
        display: flex;
        gap: 14px;
        align-items: center;
      }
      .ck-item__texto {
        flex: 1;
        min-width: 0;
        font-weight: 500;
      }
      .ck-item__total {
        font-variant-numeric: tabular-nums;
      }
      .fs-resumen {
        display: grid;
        gap: 16px;
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
        font-size: 18px;
      }
      .fs-linea__error {
        margin: 0;
        font-size: 13px;
        color: var(--fs-color-error);
      }
      textarea.fs-input {
        min-height: 72px;
        font-family: inherit;
        resize: vertical;
      }
      @media (max-width: 720px) {
        .checkout {
          padding-block: 24px 48px;
        }
        .ck-opciones {
          grid-template-columns: minmax(0, 1fr);
        }
      }
    `,
  ],
  template: `
    <section class="fs-pagina checkout">
      <div style="display: grid; gap: 8px">
        <a fsBoton variante="texto" tamano="sm" routerLink="/carrito" style="justify-self: start; padding: 0">
          <span class="fs-icono" aria-hidden="true">arrow_back</span>Volver al carrito
        </a>
        <h1 class="fs-titulo-pagina">Finalizar compra</h1>
      </div>
      @switch (estado()) {
        @case ('cargando') {
          <fs-estado-cargando />
        }
        @case ('error') {
          <fs-estado-error titulo="No pudimos preparar tu compra" (reintentar)="iniciar()" />
        }
        @default {
          <div class="fs-checkout fs-dos-columnas">
            <div class="checkout__pasos">
              <section class="ck-paso" aria-labelledby="sec-direccion">
                <h2 id="sec-direccion" class="ck-paso__titulo"><span class="ck-num" aria-hidden="true">1</span>Dirección</h2>
                @if (direcciones().length === 0) {
                  <p class="fs-suave">Aún no tienes direcciones: puedes recoger en tienda o agregar una.</p>
                }
                <div class="ck-opciones">
                  @for (d of direcciones(); track d.id) {
                    <label class="fs-opcion-direccion fs-opcion" [class.fs-opcion--activa]="direccionId() === d.id">
                      <input
                        type="radio"
                        name="direccion"
                        [value]="d.id"
                        [checked]="direccionId() === d.id"
                        (change)="elegirDireccion(d.id)"
                      />
                      <span class="ck-opcion__texto"><strong>{{ d.alias }}</strong> · {{ d.direccion }}</span>
                    </label>
                  }
                </div>
                <button
                  type="button"
                  fsBoton
                  variante="contorno"
                  style="justify-self: start"
                  [disabled]="limiteAlcanzado()"
                  (click)="agregarDireccion()"
                >
                  <span class="fs-icono" aria-hidden="true">add</span>Agregar dirección
                </button>
                @if (limiteAlcanzado()) {
                  <p class="fs-suave">
                    Llegaste al máximo de {{ maximoDirecciones }} direcciones. Elimina una para agregar otra.
                  </p>
                }
              </section>

              <section class="ck-paso" aria-labelledby="sec-entrega">
                <h2 id="sec-entrega" class="ck-paso__titulo"><span class="ck-num" aria-hidden="true">2</span>Entrega</h2>
                <div style="display: grid; gap: 12px">
                  @for (m of modalidades(); track m.modalidad) {
                    <label class="fs-opcion-modalidad fs-opcion" [class.fs-opcion--activa]="modalidad() === m.modalidad">
                      <input
                        type="radio"
                        name="modalidad"
                        [value]="m.modalidad"
                        [checked]="modalidad() === m.modalidad"
                        (change)="elegirModalidad(m.modalidad)"
                      />
                      <span class="ck-opcion__texto">
                        <strong>{{ m.nombre }}</strong>
                        @if (m.modalidad === 'RECOJO') {
                          · Sin costo
                        } @else {
                          · {{ m.costo | precio }} · {{ m.plazoDiasMin }} a {{ m.plazoDiasMax }} días
                        }
                      </span>
                    </label>
                  }
                </div>
                @if (modalidad() === 'RECOJO') {
                  <p class="fs-tienda fs-suave" style="margin: 0">
                    {{ tienda() }}
                  </p>
                }
                @if (modalidad() === 'AGENCIA') {
                  <label class="fs-agencia fs-campo">
                    <span class="fs-campo__etiqueta">Agencia</span>
                    <select class="fs-select" [value]="agenciaId() ?? ''" (change)="elegirAgencia($event)">
                      <option value="">Elige una agencia</option>
                      @for (a of agencias(); track a.id) {
                        <option [value]="a.id" [selected]="agenciaId() === a.id">{{ a.nombre }}</option>
                      }
                    </select>
                  </label>
                }
              </section>

              <section class="ck-paso ck-paso--ultimo" aria-labelledby="sec-revision">
                <h2 id="sec-revision" class="ck-paso__titulo"><span class="ck-num" aria-hidden="true">3</span>Revisa tu pedido</h2>
                @if (resumen(); as r) {
                  <ul class="ck-items">
                    @for (i of r.items; track i.varianteId) {
                      <li>
                        <span class="fs-miniatura" style="width: 48px"><span class="fs-icono" aria-hidden="true">checkroom</span></span>
                        <span class="ck-item__texto">{{ i.producto }} · {{ i.talla }} · {{ i.color }} × {{ i.cantidad }}</span>
                        <span class="ck-item__total">— {{ i.total | precio }}</span>
                      </li>
                    }
                  </ul>
                }
              </section>
            </div>

            <aside class="fs-resumen fs-tarjeta fs-lateral" aria-live="polite">
              <h2 class="fs-titulo-seccion">Resumen</h2>
              @if (errorEnvio()) {
                <p class="fs-linea__error" role="alert">
                  No pudimos calcular el envío, inténtalo en un momento
                </p>
              }
              @if (resumen(); as r) {
                <div class="fs-resumen__fila"><span>Subtotal</span><span>{{ r.subtotal | precio }}</span></div>
                <div class="fs-resumen__fila fs-resumen__fila--descuento">
                  <span>Descuento</span><span>-{{ r.descuentoTotal | precio }}</span>
                </div>
                <div class="fs-resumen__fila">
                  <span>Envío</span>
                  <span>
                    {{ r.costoEnvio | precio }}
                    @if (r.envio.plazoDiasMax) {
                      · {{ r.envio.plazoDiasMin }} a {{ r.envio.plazoDiasMax }} días
                    }
                  </span>
                </div>
                <hr class="fs-linea" />
                <div class="fs-resumen__fila fs-resumen__total">
                  <strong>Total</strong><strong>{{ r.total | precio }}</strong>
                </div>
                <p class="fs-suave" style="margin: 0">Apartamos tu stock por {{ r.expiracionMinutos }} minutos.</p>
              } @else if (recalculando()) {
                <p class="fs-suave" style="margin: 0">Calculando…</p>
              }
              <label class="fs-campo">
                <span class="fs-campo__etiqueta">Notas (opcional)</span>
                <textarea
                  class="fs-input"
                  maxlength="300"
                  [value]="notas()"
                  (input)="notas.set(valorDe($event))"
                  aria-label="Notas del pedido"
                ></textarea>
                <span class="fs-campo__ayuda">Hasta 300 caracteres</span>
              </label>
              @if (mensaje(); as texto) {
                <p class="fs-linea__error" role="alert">{{ texto }}</p>
              }
              <button
                type="button"
                fsBoton
                variante="primario"
                tamano="lg"
                [bloque]="true"
                [disabled]="!puedeConfirmar()"
                (click)="confirmar()"
              >
                {{ confirmando() ? 'Confirmando…' : 'Confirmar pedido' }}
              </button>
              <p class="fs-suave" style="margin: 0; text-align: center">
                Al confirmar aceptas los <a routerLink="/terminos">términos y condiciones</a>.
              </p>
            </aside>
          </div>
        }
      }
    </section>
  `,
})
export class CheckoutPage {
  private readonly clientes = inject(ClienteService);
  private readonly envios = inject(EnviosService);
  private readonly configuracion = inject(ConfiguracionPublicaService);
  private readonly checkout = inject(CheckoutService);
  private readonly dialogoDireccion = inject(DialogoDireccionService);
  private readonly router = inject(Router);
  private readonly sinToast = () => ({ context: new HttpContext().set(SIN_TOAST, true) });

  protected readonly maximoDirecciones = MAXIMO_DIRECCIONES;
  protected readonly estado = signal<'cargando' | 'listo' | 'error'>('cargando');
  protected readonly direcciones = signal<authApi.Direccion[]>([]);
  protected readonly direccionId = signal<number | null>(null);
  protected readonly modalidad = signal<Modalidad | null>(null);
  protected readonly agenciaId = signal<number | null>(null);
  protected readonly agencias = signal<deliveryApi.Agencia[]>([]);
  protected readonly notas = signal('');
  protected readonly resumen = signal<orderApi.ResumenCheckout | null>(null);
  protected readonly recalculando = signal(false);
  protected readonly errorEnvio = signal(false);
  protected readonly mensaje = signal<string | null>(null);
  protected readonly confirmando = signal(false);

  /** Opciones de envío con tarifa en el distrito de la dirección elegida (GET /envios/opciones). */
  private readonly opciones = signal<deliveryApi.OpcionEnvio[]>([]);
  private readonly habilitadas = signal<Modalidad[]>(['DELIVERY_LOCAL', 'AGENCIA', 'RECOJO']);
  protected readonly tienda = signal('');
  /** Se reutiliza si la persona reintenta tras un corte; se renueva si el pedido cambió (409) o se creó. */
  private clave: string | null = null;

  protected readonly limiteAlcanzado = computed(() => this.direcciones().length >= MAXIMO_DIRECCIONES);

  protected readonly modalidades = computed(() => {
    const habilitadas = this.habilitadas();
    const lista: {
      modalidad: Modalidad;
      nombre: string;
      costo: number;
      plazoDiasMin: number;
      plazoDiasMax: number;
    }[] = this.opciones()
      .filter((o) => habilitadas.includes(o.modalidad as Modalidad))
      .map((o) => ({
        modalidad: o.modalidad as Modalidad,
        nombre: NOMBRE_MODALIDAD[o.modalidad as Modalidad],
        costo: o.costo,
        plazoDiasMin: o.plazoDiasMin,
        plazoDiasMax: o.plazoDiasMax,
      }));
    if (habilitadas.includes('RECOJO')) {
      lista.push({ modalidad: 'RECOJO', nombre: NOMBRE_MODALIDAD.RECOJO, costo: 0, plazoDiasMin: 0, plazoDiasMax: 0 });
    }
    return lista;
  });

  protected readonly puedeConfirmar = computed(
    () => this.resumen() !== null && !this.errorEnvio() && !this.recalculando() && !this.confirmando(),
  );

  constructor() {
    this.iniciar();
  }

  protected iniciar(): void {
    this.estado.set('cargando');
    this.configuracion.obtenerConfiguracionPublica('body', false, this.sinToast()).subscribe({
      next: (r) => {
        const modalidades = r.data?.parametros.modalidadesEntrega as Modalidad[] | undefined;
        if (modalidades?.length) this.habilitadas.set(modalidades);
        const t = r.data?.contenido.tiendaFisica;
        const horario = r.data?.contenido.horario;
        this.tienda.set(
          t ? `${t.direccion}${t.ubigeoNombre ? ', ' + t.ubigeoNombre : ''}${horario ? ' · ' + horario : ''}` : '',
        );
        this.cargarDirecciones();
      },
      error: () => this.cargarDirecciones(),
    });
  }

  private cargarDirecciones(): void {
    this.clientes.listarMisDirecciones('body', false, this.sinToast()).subscribe({
      next: (r) => {
        const lista = r.data ?? [];
        this.direcciones.set(lista);
        this.estado.set('listo');
        const predeterminada = lista.find((d) => d.predeterminada) ?? lista[0];
        if (predeterminada) this.elegirDireccion(predeterminada.id);
        else this.prepararSinDireccion();
      },
      error: () => this.estado.set('error'),
    });
  }

  private prepararSinDireccion(): void {
    this.opciones.set([]);
    this.direccionId.set(null);
    this.modalidad.set(this.habilitadas().includes('RECOJO') ? 'RECOJO' : null);
    this.recalcular();
  }

  protected elegirDireccion(id: number): void {
    this.direccionId.set(id);
    const direccion = this.direcciones().find((d) => d.id === id);
    if (!direccion) return;
    this.envios.obtenerOpcionesEnvio(direccion.ubigeo, 'body', false, this.sinToast()).subscribe({
      next: (r) => {
        this.opciones.set(r.data ?? []);
        this.ajustarModalidad();
        this.recalcular();
      },
      error: () => {
        this.opciones.set([]);
        this.ajustarModalidad();
        this.errorEnvio.set(true);
        this.resumen.set(null);
      },
    });
  }

  /** Mantiene la modalidad elegida si sigue disponible en la nueva dirección; si no, toma la primera. */
  private ajustarModalidad(): void {
    const disponibles = this.modalidades();
    const actual = this.modalidad();
    if (!actual || !disponibles.some((m) => m.modalidad === actual)) {
      this.modalidad.set(disponibles[0]?.modalidad ?? null);
    }
    if (this.modalidad() === 'AGENCIA') this.cargarAgencias();
  }

  protected elegirModalidad(modalidad: Modalidad): void {
    this.modalidad.set(modalidad);
    if (modalidad === 'AGENCIA') this.cargarAgencias();
    this.recalcular();
  }

  private cargarAgencias(): void {
    if (this.agencias().length > 0) return;
    this.envios.listarAgenciasActivas('body', false, this.sinToast()).subscribe({
      next: (r) => this.agencias.set(r.data ?? []),
    });
  }

  protected elegirAgencia(evento: Event): void {
    const valor = Number(valorDeEvento(evento));
    this.agenciaId.set(Number.isFinite(valor) && valor > 0 ? valor : null);
    this.recalcular();
  }

  /** CA-4-17: la nueva dirección se agrega en un diálogo sobre el checkout, queda elegida y recalcula el resumen. */
  protected async agregarDireccion(): Promise<void> {
    const nueva = await this.dialogoDireccion.abrir();
    if (!nueva) return;
    this.direcciones.update((lista) => [...lista, nueva]);
    this.elegirDireccion(nueva.id);
  }

  /** CA-4-16: cada cambio de dirección, modalidad o agencia pide un resumen nuevo (no reserva ni guarda nada). */
  private recalcular(): void {
    this.mensaje.set(null);
    this.clave = null;
    const modalidad = this.modalidad();
    const direccionId = this.direccionId();
    const agenciaId = this.agenciaId();
    this.errorEnvio.set(false);
    if (!modalidad || (modalidad !== 'RECOJO' && direccionId === null) || (modalidad === 'AGENCIA' && agenciaId === null)) {
      this.resumen.set(null);
      return;
    }
    this.recalculando.set(true);
    const entrega: orderApi.EntregaElegida = {
      modalidad,
      direccionId: modalidad === 'RECOJO' ? null : direccionId,
      agenciaId: modalidad === 'AGENCIA' ? agenciaId : null,
    };
    this.checkout.resumirCheckout(entrega, 'body', false, this.sinToast()).subscribe({
      next: (r) => {
        this.resumen.set(r.data ?? null);
        this.recalculando.set(false);
      },
      error: (e: unknown) => {
        this.resumen.set(null);
        this.recalculando.set(false);
        const api = comoApiError(e);
        if (api?.codigo === 'FUERA_DE_COBERTURA' || api?.codigo === 'SIN_COBERTURA' || api?.codigo === 'MODALIDAD_NO_DISPONIBLE') {
          this.mensaje.set(api.mensaje);
        } else {
          this.errorEnvio.set(true);
        }
      },
    });
  }

  protected confirmar(): void {
    const resumen = this.resumen();
    const modalidad = this.modalidad();
    if (!resumen || !modalidad || this.confirmando()) return;
    this.clave ??= crypto.randomUUID();
    this.confirmando.set(true);
    this.mensaje.set(null);
    const notas = this.notas().trim().slice(0, MAXIMO_NOTAS);
    this.checkout
      .confirmarPedido(
        this.clave,
        {
          modalidad,
          direccionId: modalidad === 'RECOJO' ? null : this.direccionId(),
          agenciaId: modalidad === 'AGENCIA' ? this.agenciaId() : null,
          notas: notas || null,
          totalEsperado: resumen.total,
        },
        'body',
        false,
        this.sinToast(),
      )
      .subscribe({
        next: (r) => {
          this.confirmando.set(false);
          this.clave = null;
          if (r.data) void this.router.navigate(['/pago', r.data.codigo]);
        },
        error: (e: unknown) => {
          this.confirmando.set(false);
          this.alFallar(comoApiError(e));
        },
      });
  }

  /** Los 409 no crean nada: se explican y se vuelve a calcular el resumen con lo que hay ahora. */
  private alFallar(api: ReturnType<typeof comoApiError>): void {
    if (api?.codigo === 'PRECIO_CAMBIADO') {
      this.recalcular();
      this.mensaje.set('Los precios cambiaron, revisa tu pedido');
    } else if (api?.codigo === 'SIN_STOCK' || api?.codigo === 'LINEAS_NO_DISPONIBLES') {
      this.recalcular();
      this.mensaje.set(api.mensaje);
    } else if (api?.codigo === 'SERVICIO_NO_DISPONIBLE') {
      this.mensaje.set('Inténtalo en un momento');
    } else if (api?.codigo === 'DEMASIADOS_INTENTOS') {
      this.mensaje.set('Demasiados intentos, espera un momento e inténtalo de nuevo');
    } else {
      this.mensaje.set(api?.mensaje ?? 'No pudimos confirmar tu pedido, inténtalo en un momento');
    }
  }

  protected valorDe(evento: Event): string {
    return valorDeEvento(evento);
  }
}

function valorDeEvento(evento: Event): string {
  return (evento.target as HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement).value;
}
