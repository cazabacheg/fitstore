import { HttpContext } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { SIN_TOAST, type settingsApi } from 'shared';
import { InventarioService } from 'shared/api/catalog-service';

/** Parámetros que se guardan con `PUT /admin/settings/parametros` (sin la versión, que agrega la pantalla). */
export type DatosParametros = Omit<settingsApi.ParametrosRequest, 'version'>;

type Pago = settingsApi.MetodoPago;
type Entrega = settingsApi.ModalidadEntrega;

const PAGOS: { valor: Pago; etiqueta: string }[] = [
  { valor: 'CULQI', etiqueta: 'Tarjeta (Culqi)' },
  { valor: 'YAPE', etiqueta: 'Yape' },
  { valor: 'MERCADO_PAGO', etiqueta: 'Mercado Pago' },
];
const ENTREGAS: { valor: Entrega; etiqueta: string }[] = [
  { valor: 'DELIVERY_LOCAL', etiqueta: 'Delivery local' },
  { valor: 'AGENCIA', etiqueta: 'Envío por agencia' },
  { valor: 'RECOJO', etiqueta: 'Recojo en tienda' },
];
const MAXIMO_CORREOS = 5;
const CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** El cliente generado tipa las listas únicas como `Set`, pero viajan como arreglos JSON. */
const comoConjunto = <T>(valores: T[]): Set<T> => valores as unknown as Set<T>;

/** Número entero escrito en un campo; `null` si está vacío o no es entero. */
function entero(texto: string): number | null {
  return /^-?\d+$/.test(texto.trim()) ? Number(texto.trim()) : null;
}

/**
 * Pestaña Parámetros (HU-3-05): medios de pago, modalidades de entrega, expiración del pedido, días de reembolso,
 * moneda (solo lectura), correos para alertas y el bloque Inventario, que vive en catalog (CA-3-38).
 * Las dos partes se guardan por separado: si catalog no responde, esa parte muestra el error y los parámetros de
 * settings se guardan igual.
 */
@Component({
  selector: 'fs-parametros-tab',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="parametros" novalidate (submit)="guardarTodo($event)">
      <fieldset>
        <legend>Medios de pago</legend>
        @for (p of pagos; track p.valor) {
          <label class="fs-check">
            <input
              type="checkbox"
              [attr.data-testid]="'pago-' + p.valor"
              [checked]="seleccionPagos().includes(p.valor)"
              (change)="alternarPago(p.valor)"
            />
            <span>{{ p.etiqueta }}</span>
          </label>
        }
        @if (error('metodosPago')) {
          <span class="fs-campo__error">{{ error('metodosPago') }}</span>
        }
      </fieldset>

      <fieldset>
        <legend>Modalidades de entrega</legend>
        @for (e of entregas; track e.valor) {
          <label class="fs-check">
            <input
              type="checkbox"
              [attr.data-testid]="'entrega-' + e.valor"
              [checked]="seleccionEntregas().includes(e.valor)"
              (change)="alternarEntrega(e.valor)"
            />
            <span>{{ e.etiqueta }}</span>
          </label>
        }
        @if (error('modalidadesEntrega')) {
          <span class="fs-campo__error">{{ error('modalidadesEntrega') }}</span>
        }
      </fieldset>

      <fieldset class="parametros__numeros">
        <legend>Pedidos y reembolsos</legend>
        <div class="fs-campo" [class.fs-campo--error]="!!error('expiracionPedidoMinutos')">
          <label class="fs-campo__etiqueta" for="p-expiracion"
            >Expiración del pedido (minutos)</label
          >
          <input
            id="p-expiracion"
            class="fs-input"
            type="number"
            data-testid="expiracion"
            [value]="expiracion()"
            [attr.aria-invalid]="error('expiracionPedidoMinutos') ? 'true' : null"
            (input)="expiracion.set($any($event.target).value)"
          />
          <span class="fs-campo__ayuda">Entre 10 y 120 minutos.</span>
          @if (error('expiracionPedidoMinutos')) {
            <span class="fs-campo__error">{{ error('expiracionPedidoMinutos') }}</span>
          }
        </div>
        <div class="fs-campo" [class.fs-campo--error]="!!error('diasReembolsoPostventa')">
          <label class="fs-campo__etiqueta" for="p-reembolso">Días para reembolso postventa</label>
          <input
            id="p-reembolso"
            class="fs-input"
            type="number"
            data-testid="reembolso"
            [value]="reembolso()"
            [attr.aria-invalid]="error('diasReembolsoPostventa') ? 'true' : null"
            (input)="reembolso.set($any($event.target).value)"
          />
          <span class="fs-campo__ayuda">Entre 0 y 365 días (0 = sin límite).</span>
          @if (error('diasReembolsoPostventa')) {
            <span class="fs-campo__error">{{ error('diasReembolsoPostventa') }}</span>
          }
        </div>
        <div class="fs-campo">
          <label class="fs-campo__etiqueta" for="p-moneda">Moneda</label>
          <input id="p-moneda" class="fs-input" data-testid="moneda" value="PEN" readonly />
          <span class="fs-campo__ayuda"
            >Todos los precios están en soles; no se puede cambiar.</span
          >
        </div>
      </fieldset>

      <fieldset>
        <legend>Correos para alertas</legend>
        <p class="fs-suave">
          De 1 a 5 correos que reciben los avisos de la tienda. Son internos: no se muestran en la
          tienda.
        </p>
        @for (c of correos(); track $index; let i = $index) {
          <div class="parametros__correo">
            <input
              class="fs-input"
              type="email"
              data-testid="correo-alerta"
              [value]="c"
              [attr.aria-label]="'Correo para alertas ' + (i + 1)"
              [attr.aria-invalid]="errorCorreo(i) ? 'true' : null"
              (input)="cambiarCorreo(i, $any($event.target).value)"
            />
            <button
              type="button"
              class="fs-btn fs-btn--texto fs-btn--sm"
              data-testid="quitar-correo"
              [disabled]="correos().length <= 1"
              [attr.aria-label]="'Quitar el correo ' + (i + 1)"
              (click)="quitarCorreo(i)"
            >
              Quitar
            </button>
            @if (errorCorreo(i)) {
              <span class="fs-campo__error">{{ errorCorreo(i) }}</span>
            }
          </div>
        }
        <button
          type="button"
          class="fs-btn fs-btn--contorno fs-btn--sm"
          data-testid="agregar-correo"
          [disabled]="correos().length >= maximoCorreos"
          (click)="agregarCorreo()"
        >
          Agregar correo
        </button>
        @if (error('correosAlertas')) {
          <span class="fs-campo__error">{{ error('correosAlertas') }}</span>
        }
      </fieldset>

      <fieldset>
        <legend>Inventario</legend>
        <p class="fs-suave">Estos valores los guarda y usa el catálogo.</p>
        @if (errorInventario()) {
          <p class="parametros__error" role="alert" data-testid="error-inventario">
            {{ errorInventario() }}
          </p>
          @if (!inventarioCargado()) {
            <button
              type="button"
              class="fs-btn fs-btn--contorno fs-btn--sm"
              data-testid="reintentar-inventario"
              (click)="cargarInventario()"
            >
              Reintentar
            </button>
          }
        }
        @if (okInventario()) {
          <p class="fs-suave" role="status" data-testid="ok-inventario">Guardamos el inventario.</p>
        }
        @if (inventarioCargado()) {
          <div class="parametros__numeros">
            <div class="fs-campo" [class.fs-campo--error]="!!errorLocalInventario('stock')">
              <label class="fs-campo__etiqueta" for="p-stock">Stock mínimo por defecto</label>
              <input
                id="p-stock"
                class="fs-input"
                type="number"
                data-testid="stock-minimo"
                [value]="stockMinimo()"
                [attr.aria-invalid]="errorLocalInventario('stock') ? 'true' : null"
                (input)="stockMinimo.set($any($event.target).value)"
              />
              <span class="fs-campo__ayuda">Entre 0 y 999 unidades.</span>
              @if (errorLocalInventario('stock')) {
                <span class="fs-campo__error">{{ errorLocalInventario('stock') }}</span>
              }
            </div>
            <div class="fs-campo" [class.fs-campo--error]="!!errorLocalInventario('margen')">
              <label class="fs-campo__etiqueta" for="p-margen">Margen de reserva (minutos)</label>
              <input
                id="p-margen"
                class="fs-input"
                type="number"
                data-testid="margen-reserva"
                [value]="margenReserva()"
                [attr.aria-invalid]="errorLocalInventario('margen') ? 'true' : null"
                (input)="margenReserva.set($any($event.target).value)"
              />
              <span class="fs-campo__ayuda">Entre 0 y 30 minutos.</span>
              @if (errorLocalInventario('margen')) {
                <span class="fs-campo__error">{{ errorLocalInventario('margen') }}</span>
              }
            </div>
          </div>
        }
      </fieldset>

      <div>
        <button
          type="submit"
          class="fs-btn fs-btn--primario"
          data-testid="guardar-parametros"
          [disabled]="guardando()"
        >
          {{ guardando() ? 'Guardando…' : 'Guardar parámetros' }}
        </button>
      </div>
    </form>
  `,
  styles: `
    .parametros {
      display: grid;
      gap: 24px;
      max-width: 820px;
    }
    .parametros fieldset {
      display: grid;
      gap: 12px;
      margin: 0;
      padding: 16px 20px;
      border: 1px solid var(--fs-color-linea);
      border-radius: 8px;
    }
    .parametros legend {
      padding-inline: 8px;
      font-weight: 600;
    }
    .parametros__numeros {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px;
    }
    .parametros__correo {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      align-items: center;
    }
    .parametros__correo input {
      flex: 1 1 260px;
    }
    .parametros__error {
      margin: 0;
      color: var(--fs-color-error, #b3261e);
    }
  `,
})
export class ParametrosTab {
  private readonly inventario = inject(InventarioService);
  private readonly sinAviso = new HttpContext().set(SIN_TOAST, true);

  readonly parametros = input.required<settingsApi.ParametrosPublicos>();
  readonly correosAlertas = input.required<string[]>();
  readonly guardando = input(false);
  readonly erroresServidor = input<Record<string, string>>({});

  /** Los parámetros de settings validados; la pantalla los guarda con la versión de la configuración. */
  readonly guardar = output<DatosParametros>();
  readonly sinGuardar = output<boolean>();

  protected readonly pagos = PAGOS;
  protected readonly entregas = ENTREGAS;
  protected readonly maximoCorreos = MAXIMO_CORREOS;

  protected readonly seleccionPagos = signal<Pago[]>([]);
  protected readonly seleccionEntregas = signal<Entrega[]>([]);
  protected readonly expiracion = signal('');
  protected readonly reembolso = signal('');
  protected readonly correos = signal<string[]>([]);
  private readonly intentado = signal(false);

  protected readonly inventarioCargado = signal(false);
  protected readonly stockMinimo = signal('');
  protected readonly margenReserva = signal('');
  protected readonly errorInventario = signal('');
  protected readonly okInventario = signal(false);

  private readonly errores = computed<Record<string, string>>(() => {
    const e: Record<string, string> = {};
    if (this.seleccionPagos().length === 0) e['metodosPago'] = 'Habilita al menos un medio de pago';
    if (this.seleccionEntregas().length === 0)
      e['modalidadesEntrega'] = 'Habilita al menos una modalidad de entrega';
    const expiracion = entero(this.expiracion());
    if (expiracion === null || expiracion < 10 || expiracion > 120)
      e['expiracionPedidoMinutos'] = 'La expiración debe ser de 10 a 120 minutos';
    const reembolso = entero(this.reembolso());
    if (reembolso === null || reembolso < 0 || reembolso > 365)
      e['diasReembolsoPostventa'] = 'Los días para reembolso van de 0 a 365 (0 = sin límite)';
    const lista = this.correos().map((c) => c.trim());
    if (lista.length < 1 || lista.length > MAXIMO_CORREOS)
      e['correosAlertas'] = 'Indica de 1 a 5 correos para alertas';
    return e;
  });

  constructor() {
    effect(() => {
      const p = this.parametros();
      this.seleccionPagos.set(Array.from(p.metodosPago));
      this.seleccionEntregas.set(Array.from(p.modalidadesEntrega));
      this.expiracion.set(String(p.expiracionPedidoMinutos));
      this.reembolso.set(String(p.diasReembolsoPostventa));
      this.correos.set([...this.correosAlertas()]);
      this.intentado.set(false);
    });
    effect(() => {
      const p = this.parametros();
      const igual =
        this.firma(this.seleccionPagos()) === this.firma(Array.from(p.metodosPago)) &&
        this.firma(this.seleccionEntregas()) === this.firma(Array.from(p.modalidadesEntrega)) &&
        this.expiracion() === String(p.expiracionPedidoMinutos) &&
        this.reembolso() === String(p.diasReembolsoPostventa) &&
        JSON.stringify(this.correos()) === JSON.stringify(this.correosAlertas());
      this.sinGuardar.emit(!igual);
    });
    this.cargarInventario();
  }

  private firma(valores: string[]): string {
    return [...valores].sort().join(',');
  }

  protected cargarInventario(): void {
    this.errorInventario.set('');
    this.inventario
      .obtenerConfiguracionInventario('body', false, { context: this.sinAviso })
      .subscribe({
        next: (r) => {
          this.stockMinimo.set(String(r.data?.stockMinimoPorDefecto ?? 0));
          this.margenReserva.set(String(r.data?.margenReservaMinutos ?? 0));
          this.inventarioCargado.set(true);
        },
        error: () => {
          this.inventarioCargado.set(false);
          this.errorInventario.set(
            'No pudimos cargar el inventario. Los demás parámetros se pueden guardar igual.',
          );
        },
      });
  }

  protected error(campo: string): string {
    return this.erroresServidor()[campo] ?? (this.intentado() ? (this.errores()[campo] ?? '') : '');
  }

  protected errorCorreo(i: number): string {
    if (!this.intentado()) return '';
    const correo = this.correos()[i]?.trim() ?? '';
    return correo.length === 0 || !CORREO.test(correo) ? 'Correo inválido' : '';
  }

  protected errorLocalInventario(campo: 'stock' | 'margen'): string {
    if (!this.intentado() || !this.inventarioCargado()) return '';
    if (campo === 'stock') {
      const v = entero(this.stockMinimo());
      return v === null || v < 0 || v > 999 ? 'El stock mínimo va de 0 a 999' : '';
    }
    const v = entero(this.margenReserva());
    return v === null || v < 0 || v > 30 ? 'El margen de reserva va de 0 a 30 minutos' : '';
  }

  protected alternarPago(valor: Pago): void {
    this.seleccionPagos.update((l) =>
      l.includes(valor) ? l.filter((x) => x !== valor) : [...l, valor],
    );
  }

  protected alternarEntrega(valor: Entrega): void {
    this.seleccionEntregas.update((l) =>
      l.includes(valor) ? l.filter((x) => x !== valor) : [...l, valor],
    );
  }

  protected agregarCorreo(): void {
    if (this.correos().length < MAXIMO_CORREOS) this.correos.update((l) => [...l, '']);
  }

  protected quitarCorreo(i: number): void {
    if (this.correos().length > 1) this.correos.update((l) => l.filter((_, k) => k !== i));
  }

  protected cambiarCorreo(i: number, valor: string): void {
    this.correos.update((l) => l.map((c, k) => (k === i ? valor : c)));
  }

  protected guardarTodo(evento: Event): void {
    evento.preventDefault();
    this.intentado.set(true);
    const hayErrorCorreos = this.correos().some((_, i) => this.errorCorreo(i) !== '');
    const hayErrorInventario =
      this.errorLocalInventario('stock') !== '' || this.errorLocalInventario('margen') !== '';
    if (Object.keys(this.errores()).length > 0 || hayErrorCorreos || hayErrorInventario) return;

    this.guardar.emit({
      metodosPago: comoConjunto(this.seleccionPagos()),
      modalidadesEntrega: comoConjunto(this.seleccionEntregas()),
      expiracionPedidoMinutos: Number(this.expiracion()),
      diasReembolsoPostventa: Number(this.reembolso()),
      correosAlertas: this.correos().map((c) => c.trim()),
    });
    this.guardarInventario();
  }

  /** Independiente de settings: si catalog falla, solo este bloque muestra el error. */
  private guardarInventario(): void {
    if (!this.inventarioCargado()) return;
    this.errorInventario.set('');
    this.okInventario.set(false);
    this.inventario
      .guardarConfiguracionInventario(
        {
          stockMinimoPorDefecto: Number(this.stockMinimo()),
          margenReservaMinutos: Number(this.margenReserva()),
        },
        'body',
        false,
        { context: this.sinAviso },
      )
      .subscribe({
        next: () => this.okInventario.set(true),
        error: () =>
          this.errorInventario.set(
            'No pudimos guardar el inventario porque el catálogo no responde. Inténtalo de nuevo en un momento.',
          ),
      });
  }
}
