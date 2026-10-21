import { HttpContext } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  signal,
  type WritableSignal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { SIN_TOAST, type Pagina } from 'shared';
import { InventarioService } from 'shared/api/catalog-service';
import { EntregasService } from 'shared/api/delivery-service';
import { PedidosAdminService } from 'shared/api/order-service';
import { ReclamosService } from 'shared/api/settings-service';
import { type Observable } from 'rxjs';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { EstadoError } from '../../ui/estados/estado-error';
import { ESTADOS } from '../../ui/insignia/estados';

/** Estado de una tarjeta: cada una carga y falla por su cuenta (SPEC-8 I1). */
interface Tarjeta<T> {
  cargando: boolean;
  error: boolean;
  datos: T | null;
}
const nueva = <T>(): WritableSignal<Tarjeta<T>> =>
  signal({ cargando: true, error: false, datos: null });

interface StockCritico {
  varianteId: number;
  sku: string;
  disponible: number;
  estado: string;
}
interface Reclamos {
  porVencer: number;
  vencidos: number;
}
interface Entregas {
  programadas: number;
  entregadas: number;
  fallidas: number;
}

/** Estados de pedido que se atienden: los terminales no aparecen. */
const ESTADOS_ATENDER = [
  'PENDIENTE_PAGO',
  'PAGADO',
  'EN_PREPARACION',
  'LISTO_DESPACHO',
  'ENVIADO',
  'LISTO_RECOJO',
];

const hoyIso = () => {
  const d = new Date();
  const dos = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${dos(d.getMonth() + 1)}-${dos(d.getDate())}`;
};

/**
 * Dashboard del backoffice (P-8-01): pedidos por estado, stock bajo, entregas de hoy y reclamos.
 * Sin ventas ni top de productos (HU-4-11 es extensión de alcance). Los datos se piden al abrir y con "Actualizar".
 */
@Component({
  selector: 'fs-dashboard-page',
  imports: [RouterLink, EncabezadoPagina, EstadoError],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fs-encabezado-pagina titulo="Dashboard">
      <button type="button" accion class="fs-btn fs-btn--contorno" (click)="cargarTodo()">
        <span class="fs-icono" aria-hidden="true">refresh</span> Actualizar
      </button>
    </fs-encabezado-pagina>
    @if (ultimaCarga()) {
      <p class="fs-suave">Actualizado a las {{ ultimaCarga() }}</p>
    }

    <div class="db-rejilla">
      <section class="fs-tarjeta db-tarjeta">
        <h2>Pedidos por estado</h2>
        @if (pedidos().error) {
          <fs-estado-error
            titulo="No disponible"
            texto="No pudimos cargar estos datos."
            (reintentar)="cargarPedidos()"
          />
        } @else {
          <div class="db-estados">
            @for (e of estadosPedido; track e.codigo) {
              <a [routerLink]="['/pedidos']" [queryParams]="{ estado: e.codigo }">
                <span>{{ e.etiqueta }}</span> <b>{{ pedidos().datos?.[e.codigo] ?? 0 }}</b>
              </a>
            }
          </div>
        }
      </section>

      <section class="fs-tarjeta db-tarjeta">
        <h2>
          Stock bajo
          <a [routerLink]="['/inventario']" [queryParams]="{ estado: 'BAJO' }">Ver inventario</a>
        </h2>
        @if (stock().error) {
          <fs-estado-error
            titulo="No disponible"
            texto="No pudimos cargar estos datos."
            (reintentar)="cargarStock()"
          />
        } @else if (stock().datos?.length === 0) {
          <p class="fs-suave">Sin stock bajo 🎉</p>
        } @else {
          <div class="db-lista">
            @for (s of stock().datos; track s.varianteId) {
              <div>
                <span>{{ s.sku }}</span>
                <span>{{ s.estado === 'AGOTADO' ? 'Agotado' : s.disponible + ' un.' }}</span>
              </div>
            }
          </div>
        }
      </section>

      <section class="fs-tarjeta db-tarjeta">
        <h2><a [routerLink]="['/entregas']">Entregas de hoy</a></h2>
        @if (entregas().error) {
          <fs-estado-error
            titulo="No disponible"
            texto="No pudimos cargar estos datos."
            (reintentar)="cargarEntregas()"
          />
        } @else {
          <div class="db-lista">
            <div>
              <span>Programadas</span> <b>{{ entregas().datos?.programadas ?? 0 }}</b>
            </div>
            <div>
              <span>Entregadas</span> <b>{{ entregas().datos?.entregadas ?? 0 }}</b>
            </div>
            <div>
              <span>Fallidas</span> <b>{{ entregas().datos?.fallidas ?? 0 }}</b>
            </div>
          </div>
        }
      </section>

      <section class="fs-tarjeta db-tarjeta">
        <h2>Reclamos</h2>
        @if (reclamos().error) {
          <fs-estado-error
            titulo="No disponible"
            texto="No pudimos cargar estos datos."
            (reintentar)="cargarReclamos()"
          />
        } @else {
          <div class="db-lista">
            <div>
              <a [routerLink]="['/reclamos']" [queryParams]="{ plazo: 'porVencer' }">Por vencer</a>
              <b>{{ reclamos().datos?.porVencer ?? 0 }}</b>
            </div>
            <div>
              <a [routerLink]="['/reclamos']" [queryParams]="{ plazo: 'vencidos' }">Vencidos</a>
              <b>{{ reclamos().datos?.vencidos ?? 0 }}</b>
            </div>
          </div>
        }
      </section>
    </div>
  `,
  styles: `
    .db-rejilla {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: var(--espacio-5);
    }
    .db-tarjeta {
      display: grid;
      gap: var(--espacio-4);
      align-content: start;
    }
    .db-lista > div {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: var(--espacio-3);
      padding-block: var(--espacio-2);
      border-top: 1px solid var(--fs-color-linea);
    }
    .db-lista > div:first-child {
      border-top: 0;
    }
    .db-estados {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: var(--espacio-2);
    }
    .db-estados a {
      display: grid;
      gap: 2px;
      padding: var(--espacio-3);
      border-radius: var(--fs-radio-md);
      background: var(--fs-color-superficie-alterna);
      text-decoration: none;
      color: inherit;
    }
    .db-estados b {
      font-size: 22px;
      font-variant-numeric: tabular-nums;
    }
  `,
})
export class DashboardPage {
  private readonly pedidosApi = inject(PedidosAdminService);
  private readonly stockApi = inject(InventarioService);
  private readonly entregasApi = inject(EntregasService);
  private readonly reclamosApi = inject(ReclamosService);
  private readonly destruir = inject(DestroyRef);
  private readonly sinToast = { context: new HttpContext().set(SIN_TOAST, true) };

  protected readonly estadosPedido = ESTADOS_ATENDER.map((codigo) => ({
    codigo,
    etiqueta: ESTADOS.pedido[codigo]?.etiqueta ?? codigo,
  }));
  protected readonly pedidos = nueva<Record<string, number>>();
  protected readonly stock = nueva<StockCritico[]>();
  protected readonly entregas = nueva<Entregas>();
  protected readonly reclamos = nueva<Reclamos>();
  protected readonly ultimaCarga = signal('');

  constructor() {
    this.cargarTodo();
  }

  protected cargarTodo(): void {
    this.cargarPedidos();
    this.cargarStock();
    this.cargarEntregas();
    this.cargarReclamos();
    const ahora = new Date();
    this.ultimaCarga.set(
      `${String(ahora.getHours()).padStart(2, '0')}:${String(ahora.getMinutes()).padStart(2, '0')}`,
    );
  }

  protected cargarPedidos(): void {
    this.pedir(
      this.pedidos,
      this.pedidosApi.listarPedidosAdmin(
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        0,
        1,
        undefined,
        'body',
        false,
        this.sinToast,
      ),
      (r) => r.data?.contadores ?? {},
    );
  }

  protected cargarStock(): void {
    this.pedir(
      this.stock,
      this.stockApi.listarStock(
        undefined,
        undefined,
        true,
        undefined,
        0,
        5,
        undefined,
        'body',
        false,
        this.sinToast,
      ),
      (r) => (r.data as unknown as Pagina<StockCritico>).contenido,
    );
  }

  protected cargarEntregas(): void {
    this.pedir(
      this.entregas,
      this.entregasApi.listarEntregas(
        undefined,
        undefined,
        hoyIso(),
        undefined,
        undefined,
        0,
        1,
        undefined,
        'body',
        false,
        this.sinToast,
      ),
      (r) => {
        const c = r.data?.contadores ?? {};
        const total = Object.values(c).reduce((a, n) => a + n, 0);
        return { programadas: total, entregadas: c['ENTREGADA'] ?? 0, fallidas: c['FALLIDA'] ?? 0 };
      },
    );
  }

  protected cargarReclamos(): void {
    // Dos contadores (por vencer y vencidos) que se juntan en una sola tarjeta
    const consulta = (porVencer: boolean) =>
      this.reclamosApi.listarReclamos(
        undefined,
        undefined,
        porVencer ? true : undefined,
        porVencer ? undefined : true,
        undefined,
        undefined,
        undefined,
        0,
        1,
        undefined,
        'body',
        false,
        this.sinToast,
      );
    this.reclamos.set({ cargando: true, error: false, datos: null });
    let porVencer: number | null = null;
    let vencidos: number | null = null;
    const terminar = () => {
      if (porVencer !== null && vencidos !== null) {
        this.reclamos.set({ cargando: false, error: false, datos: { porVencer, vencidos } });
      }
    };
    const fallar = () => this.reclamos.set({ cargando: false, error: true, datos: null });
    const a = consulta(true).subscribe({
      next: (r) => ((porVencer = r.data?.totalElementos ?? 0), terminar()),
      error: fallar,
    });
    const b = consulta(false).subscribe({
      next: (r) => ((vencidos = r.data?.totalElementos ?? 0), terminar()),
      error: fallar,
    });
    this.destruir.onDestroy(() => (a.unsubscribe(), b.unsubscribe()));
  }

  private pedir<R, T>(
    tarjeta: WritableSignal<Tarjeta<T>>,
    peticion: Observable<R>,
    extraer: (r: R) => T,
  ): void {
    tarjeta.set({ cargando: true, error: false, datos: null });
    const sub = peticion.subscribe({
      next: (r) => tarjeta.set({ cargando: false, error: false, datos: extraer(r) }),
      error: () => tarjeta.set({ cargando: false, error: true, datos: null }),
    });
    this.destruir.onDestroy(() => sub.unsubscribe());
  }
}
