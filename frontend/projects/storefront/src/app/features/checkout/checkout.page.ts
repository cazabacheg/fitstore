import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  PrecioPipe,
  SIN_TOAST,
  authApi,
  comoApiError,
  deliveryApi,
  orderApi,
  settingsApi,
} from 'shared';
import { FsBoton } from '../../ui/boton/boton';
import { EstadoCargando, EstadoError } from '../../ui/estados/estados';
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
  template: `
    <h1>Finalizar compra</h1>
    @switch (estado()) {
      @case ('cargando') {
        <fs-estado-cargando />
      }
      @case ('error') {
        <fs-estado-error titulo="No pudimos preparar tu compra" (reintentar)="iniciar()" />
      }
      @default {
        <div class="fs-checkout" style="display: grid; gap: var(--espacio-5)">
          <section aria-labelledby="sec-direccion">
            <h2 id="sec-direccion">1. Dirección</h2>
            @if (direcciones().length === 0) {
              <p class="fs-suave">Aún no tienes direcciones: puedes recoger en tienda o agregar una.</p>
            }
            @for (d of direcciones(); track d.id) {
              <label class="fs-opcion-direccion" style="display: block">
                <input
                  type="radio"
                  name="direccion"
                  [value]="d.id"
                  [checked]="direccionId() === d.id"
                  (change)="elegirDireccion(d.id)"
                />
                <strong>{{ d.alias }}</strong> · {{ d.direccion }}
              </label>
            }
            <button
              type="button"
              fsBoton
              variante="contorno"
              [disabled]="limiteAlcanzado()"
              (click)="agregarDireccion()"
            >
              Agregar dirección
            </button>
            @if (limiteAlcanzado()) {
              <p class="fs-suave">
                Llegaste al máximo de {{ maximoDirecciones }} direcciones. Elimina una para agregar otra.
              </p>
            }
          </section>

          <section aria-labelledby="sec-entrega">
            <h2 id="sec-entrega">2. Entrega</h2>
            @for (m of modalidades(); track m.modalidad) {
              <label class="fs-opcion-modalidad" style="display: block">
                <input
                  type="radio"
                  name="modalidad"
                  [value]="m.modalidad"
                  [checked]="modalidad() === m.modalidad"
                  (change)="elegirModalidad(m.modalidad)"
                />
                <strong>{{ m.nombre }}</strong>
                @if (m.modalidad === 'RECOJO') {
                  · Sin costo
                } @else {
                  · {{ m.costo | precio }} · {{ m.plazoDiasMin }} a {{ m.plazoDiasMax }} días
                }
              </label>
            }
            @if (modalidad() === 'RECOJO') {
              <p class="fs-tienda" style="margin-top: var(--espacio-2)">
                {{ tienda() }}
              </p>
            }
            @if (modalidad() === 'AGENCIA') {
              <label class="fs-agencia" style="display: block; margin-top: var(--espacio-2)">
                Agencia
                <select [value]="agenciaId() ?? ''" (change)="elegirAgencia($event)">
                  <option value="">Elige una agencia</option>
                  @for (a of agencias(); track a.id) {
                    <option [value]="a.id" [selected]="agenciaId() === a.id">{{ a.nombre }}</option>
                  }
                </select>
              </label>
            }
          </section>

          <section aria-labelledby="sec-revision">
            <h2 id="sec-revision">3. Revisa tu pedido</h2>
            @if (resumen(); as r) {
              <ul style="list-style: none; padding: 0">
                @for (i of r.items; track i.varianteId) {
                  <li>{{ i.producto }} · {{ i.talla }} · {{ i.color }} × {{ i.cantidad }} — {{ i.total | precio }}</li>
                }
              </ul>
            }
          </section>

          <aside class="fs-resumen" aria-live="polite" style="display: grid; gap: var(--espacio-2)">
            <h2>Resumen</h2>
            @if (errorEnvio()) {
              <p class="fs-linea__error" role="alert">
                No pudimos calcular el envío, inténtalo en un momento
              </p>
            }
            @if (resumen(); as r) {
              <div class="fs-resumen__fila"><span>Subtotal</span><span>{{ r.subtotal | precio }}</span></div>
              <div class="fs-resumen__fila"><span>Descuento</span><span>-{{ r.descuentoTotal | precio }}</span></div>
              <div class="fs-resumen__fila">
                <span>Envío</span>
                <span>
                  {{ r.costoEnvio | precio }}
                  @if (r.envio.plazoDiasMax) {
                    · {{ r.envio.plazoDiasMin }} a {{ r.envio.plazoDiasMax }} días
                  }
                </span>
              </div>
              <div class="fs-resumen__fila fs-resumen__total">
                <strong>Total</strong><strong>{{ r.total | precio }}</strong>
              </div>
              <p class="fs-suave">Apartamos tu stock por {{ r.expiracionMinutos }} minutos.</p>
            } @else if (recalculando()) {
              <p class="fs-suave">Calculando…</p>
            }
            <label>
              Notas (opcional)
              <textarea
                maxlength="300"
                [value]="notas()"
                (input)="notas.set(valorDe($event))"
                aria-label="Notas del pedido"
              ></textarea>
            </label>
            @if (mensaje(); as texto) {
              <p class="fs-linea__error" role="alert">{{ texto }}</p>
            }
            <button
              type="button"
              fsBoton
              variante="primario"
              [bloque]="true"
              [disabled]="!puedeConfirmar()"
              (click)="confirmar()"
            >
              {{ confirmando() ? 'Confirmando…' : 'Confirmar pedido' }}
            </button>
            <p class="fs-suave">
              Al confirmar aceptas los <a routerLink="/terminos">términos y condiciones</a>.
            </p>
          </aside>
        </div>
      }
    }
  `,
})
export class CheckoutPage {
  private readonly clientes = inject(authApi.ClienteService);
  private readonly envios = inject(deliveryApi.EnviosService);
  private readonly configuracion = inject(settingsApi.ConfiguracionPublicaService);
  private readonly checkout = inject(orderApi.CheckoutService);
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
