import { HttpContext } from '@angular/common/http';
import { Component, OnDestroy, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { SIN_TOAST, PrecioPipe, comoApiError, paymentApi } from 'shared';
import { PagoService } from 'shared/api/payment-service';
import { firstValueFrom } from 'rxjs';
import { FsBoton } from '../../ui/boton/boton';
import { EstadoCargando, EstadoError } from '../../ui/estados/estados';
import { ESTILOS_PANTALLA } from '../catalogo/estilos-pantalla';
import { BrickMercadoPago, type EnvioBrick } from './brick-mercado-pago';
import { FormularioSimulado } from './formulario-simulado';
import { ResultadoPago, type TipoResultado } from './resultado-pago';

type Pagina = paymentApi.PaginaPago;
type Medio = paymentApi.MedioDisponible;

type Fase = 'cargando' | 'preparando' | 'listo' | 'error';
interface Resultado {
  tipo: TipoResultado;
  motivo?: string;
}

/** Intentos para encontrar la intención de pago recién creada (CA-5-04): uno por segundo. */
const REINTENTOS_PREPARANDO = 10;
/** Cada cuánto se consulta el estado tras un 202 y cuánto se espera en total (P4). */
const CADA_CONSULTA_MS = 3_000;
const ESPERA_MAXIMA_MS = 120_000;
const CONSULTAS_BOLETA = 20;

const PAGADOS = ['PAGADA', 'REEMBOLSADA_PARCIAL', 'REEMBOLSADA'];

/** Página de pago y su resultado (P-5-01 y P-5-02): una sola ruta, el resultado es un estado de la página (P3). */
@Component({
  selector: 'fs-pago-page',
  imports: [RouterLink, PrecioPipe, FsBoton, EstadoCargando, EstadoError, BrickMercadoPago, FormularioSimulado, ResultadoPago],
  templateUrl: './pago.page.html',
  styles: [
    ESTILOS_PANTALLA,
    `
    .pago-medios {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 10px;
    }
    .pago-medio {
      display: grid;
      justify-items: center;
      gap: 6px;
      padding: 14px 8px;
      border: 1px solid var(--fs-color-borde-control);
      border-radius: var(--fs-radio-md);
      background: var(--fs-color-superficie);
      font-size: 13px;
      font-weight: 500;
      text-align: center;
      cursor: pointer;
    }
    .pago-medio[aria-checked='true'] {
      border: 2px solid var(--fs-color-texto);
      padding: 13px 7px;
    }
    .pago-medio[aria-disabled='true'] {
      opacity: 0.55;
      cursor: not-allowed;
    }
    .pago-medio__motivo {
      font-size: 12px;
      font-weight: 400;
    }
    .pago-fila {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      font-variant-numeric: tabular-nums;
    }
  `,
  ],
})
export class PagoPage implements OnInit, OnDestroy {
  protected readonly codigo = inject(ActivatedRoute).snapshot.paramMap.get('codigo') ?? '';

  private readonly api = inject(PagoService);
  private readonly contexto = new HttpContext().set(SIN_TOAST, true);

  protected readonly fase = signal<Fase>('cargando');
  protected readonly pagina = signal<Pagina | null>(null);
  protected readonly resultado = signal<Resultado | null>(null);
  protected readonly medioElegido = signal<string | null>(null);
  protected readonly procesando = signal(false);
  protected readonly errorCobro = signal('');
  protected readonly descargando = signal(false);
  protected readonly ahora = signal(Date.now());

  protected readonly simulado = viewChild(FormularioSimulado);

  /** Milisegundos que le quedan al plazo de pago. */
  protected readonly restanteMs = computed(() => {
    const p = this.pagina();
    return p ? Math.max(0, Date.parse(p.expiraEn) - this.ahora()) : 0;
  });
  protected readonly cuentaRegresiva = computed(() => {
    const segundos = Math.floor(this.restanteMs() / 1000);
    const minutos = Math.floor(segundos / 60);
    return `${String(minutos).padStart(2, '0')}:${String(segundos % 60).padStart(2, '0')}`;
  });

  /** Qué se muestra: el resultado, el estado del pedido o el formulario de pago. */
  protected readonly vista = computed<'resultado' | 'pagado' | 'anulado' | 'vencido' | 'pagar' | null>(() => {
    const p = this.pagina();
    if (!p) return null;
    if (this.resultado()) return 'resultado';
    if (PAGADOS.includes(p.estado)) return 'pagado';
    if (p.estado === 'ANULADA') return 'anulado';
    return this.restanteMs() <= 0 ? 'vencido' : 'pagar';
  });
  protected readonly medio = computed(() => {
    const clave = this.medioElegido();
    return this.pagina()?.medios.find((m) => claveDe(m) === clave) ?? null;
  });

  /** Se genera al montar el formulario y se reutiliza en los reintentos de red (CA-5-17). */
  private clave = crypto.randomUUID();
  private reloj: ReturnType<typeof setInterval> | undefined;
  private reintento: ReturnType<typeof setTimeout> | undefined;
  private sondeo: ReturnType<typeof setInterval> | undefined;
  private sondeoBoleta: ReturnType<typeof setInterval> | undefined;

  ngOnInit(): void {
    this.reloj = setInterval(() => this.ahora.set(Date.now()), 1_000);
    this.cargar(0);
  }

  ngOnDestroy(): void {
    clearInterval(this.reloj);
    clearTimeout(this.reintento);
    clearInterval(this.sondeo);
    clearInterval(this.sondeoBoleta);
  }

  protected reintentarCarga(): void {
    this.fase.set('cargando');
    this.cargar(0);
  }

  private cargar(intento: number): void {
    this.api.obtenerPaginaPago(this.codigo, 'body', false, { context: this.contexto }).subscribe({
      next: (r) => {
        this.ahora.set(Date.now());
        this.pagina.set(r.data ?? null);
        this.fase.set('listo');
        if (r.data) this.alCargar(r.data);
      },
      error: (e: unknown) => {
        if (comoApiError(e)?.codigo === 'PREPARANDO_PAGO' && intento < REINTENTOS_PREPARANDO) {
          this.fase.set('preparando');
          this.reintento = setTimeout(() => this.cargar(intento + 1), 1_000);
        } else {
          this.fase.set('error');
        }
      },
    });
  }

  private alCargar(p: Pagina): void {
    if (p.procesando) {
      this.resultado.set({ tipo: 'confirmando' });
      this.seguirResultado();
    } else if (PAGADOS.includes(p.estado) && !p.boletaDisponible) {
      this.esperarBoleta();
    }
  }

  // ------------------------------------------------------------ elegir el medio

  protected elegir(m: Medio): void {
    if (m.disponible) this.medioElegido.set(claveDe(m));
  }

  protected nombreDe(m: Medio): string {
    if (m.proveedor === 'MERCADO_PAGO') return 'Mercado Pago';
    if (m.proveedor === 'SIMULADO') return 'Pago de prueba';
    return m.medio === 'YAPE' ? 'Yape' : 'Tarjeta';
  }

  protected iconoDe(m: Medio): string {
    if (m.proveedor === 'MERCADO_PAGO') return 'account_balance_wallet';
    if (m.proveedor === 'SIMULADO') return 'science';
    return m.medio === 'YAPE' ? 'smartphone' : 'credit_card';
  }

  protected claveDe(m: Medio): string {
    return claveDe(m);
  }

  // ------------------------------------------------------------ cobrar

  protected pagarSimulado(): void {
    const token = this.simulado()?.token();
    if (token) void this.cobrar({ proveedor: 'SIMULADO', medio: 'TARJETA', token });
  }

  protected alEnviarBrick(envio: EnvioBrick): void {
    const d = envio.datos;
    void this.cobrar({
      proveedor: 'MERCADO_PAGO',
      medio: 'TARJETA',
      token: d.token,
      email: d.payer?.email,
      datosAdicionales: { payment_method_id: d.payment_method_id, installments: d.installments, issuer_id: d.issuer_id },
    }).then((ok) => envio.terminar(ok));
  }

  /** @returns `true` si el cobro quedó aprobado o en confirmación; `false` si se rechazó o falló */
  private async cobrar(cargo: paymentApi.CargoRequest): Promise<boolean> {
    if (this.procesando()) return false;
    this.procesando.set(true);
    this.errorCobro.set('');
    try {
      const r = await firstValueFrom(
        this.api.cobrarPedido(this.codigo, this.clave, cargo, 'body', false, { context: this.contexto }),
      );
      if (r.data?.estado === 'APROBADA') {
        this.aprobar();
      } else {
        this.resultado.set({ tipo: 'confirmando' });
        this.seguirResultado();
      }
      return true;
    } catch (e) {
      this.alFallarElCobro(e);
      return false;
    } finally {
      this.procesando.set(false);
    }
  }

  private alFallarElCobro(e: unknown): void {
    const error = comoApiError(e);
    const estado = (e as { status?: number }).status;
    if (estado === 402) {
      this.resultado.set({ tipo: 'rechazado', motivo: error?.mensaje });
    } else if (error?.codigo === 'PAGO_EN_CURSO') {
      this.resultado.set({ tipo: 'confirmando' });
      this.seguirResultado();
    } else if (estado === 409) {
      // ya pagado, vencido o anulado: la página muestra el estado real
      this.cargar(REINTENTOS_PREPARANDO);
    } else if (estado === 0) {
      this.errorCobro.set('No pudimos conectar con el servidor. Inténtalo de nuevo: no se te cobrará dos veces.');
    } else {
      this.errorCobro.set(error?.mensaje ?? 'No pudimos procesar tu pago. Inténtalo de nuevo.');
    }
  }

  private aprobar(): void {
    clearInterval(this.sondeo);
    this.resultado.set({ tipo: 'aprobado' });
    this.pagina.update((p) => (p ? { ...p, estado: 'PAGADA', procesando: false } : p));
    this.esperarBoleta();
  }

  protected reintentarPago(): void {
    this.resultado.set(null);
    this.errorCobro.set('');
    this.clave = crypto.randomUUID();
  }

  // ------------------------------------------------------------ resultado en curso (P4) y boleta

  /** Consulta el estado cada 3 s hasta tener el resultado o hasta 2 minutos. */
  private seguirResultado(): void {
    clearInterval(this.sondeo);
    const inicio = Date.now();
    this.sondeo = setInterval(() => {
      if (Date.now() - inicio >= ESPERA_MAXIMA_MS) {
        clearInterval(this.sondeo);
        this.resultado.set({ tipo: 'sin-confirmar' });
        return;
      }
      this.api.obtenerPaginaPago(this.codigo, 'body', false, { context: this.contexto }).subscribe({
        next: (r) => this.evaluar(r.data),
        error: () => undefined, // un fallo suelto no corta la espera; se vuelve a consultar
      });
    }, CADA_CONSULTA_MS);
  }

  private evaluar(p: Pagina | undefined): void {
    if (!p || this.resultado()?.tipo !== 'confirmando') return;
    this.pagina.set(p);
    if (PAGADOS.includes(p.estado)) {
      this.aprobar();
    } else if (!p.procesando) {
      clearInterval(this.sondeo);
      this.resultado.set({ tipo: 'rechazado', motivo: p.intentosRechazados.at(-1)?.motivo });
    }
  }

  /** La boleta se genera en segundo plano: se consulta hasta que esté lista. */
  private esperarBoleta(): void {
    clearInterval(this.sondeoBoleta);
    let consultas = 0;
    this.sondeoBoleta = setInterval(() => {
      if (++consultas > CONSULTAS_BOLETA) {
        clearInterval(this.sondeoBoleta);
        return;
      }
      this.api.obtenerPaginaPago(this.codigo, 'body', false, { context: this.contexto }).subscribe({
        next: (r) => {
          if (r.data?.boletaDisponible) {
            clearInterval(this.sondeoBoleta);
            this.pagina.update((p) => (p ? { ...p, boletaDisponible: true } : p));
          }
        },
        error: () => undefined,
      });
    }, CADA_CONSULTA_MS);
  }

  protected descargarBoleta(): void {
    this.descargando.set(true);
    this.api.descargarMiBoleta(this.codigo, 'body', false, { httpHeaderAccept: 'application/pdf' }).subscribe({
      next: (pdf) => {
        const url = URL.createObjectURL(pdf);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = `Boleta-${this.codigo}.pdf`;
        enlace.click();
        URL.revokeObjectURL(url);
        this.descargando.set(false);
      },
      error: () => this.descargando.set(false),
    });
  }

  protected tipoResultado(): TipoResultado {
    return this.resultado()?.tipo ?? 'confirmando';
  }
}

function claveDe(m: Medio): string {
  return `${m.proveedor}:${m.medio}`;
}
