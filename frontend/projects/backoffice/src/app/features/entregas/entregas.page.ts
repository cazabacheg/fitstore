import { HttpContext } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import {
  BehaviorSubject,
  EMPTY,
  catchError,
  combineLatest,
  map,
  startWith,
  switchMap,
  tap,
} from 'rxjs';
import {
  FechaPipe,
  SIN_TOAST,
  ToastService,
  deliveryApi,
  type ConsultaPagina,
  type Pagina,
} from 'shared';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { EstadoError } from '../../ui/estados/estado-error';
import { BarraFiltros, type ChipFiltro, type SelectFiltro } from '../../ui/filtros/barra-filtros';
import { ESTADOS } from '../../ui/insignia/estados';
import { InsigniaEstado } from '../../ui/insignia/insignia-estado';
import { FsCelda } from '../../ui/tabla/fs-celda';
import { FsTabla, type AccionFila, type ColumnaTabla } from '../../ui/tabla/fs-tabla';
import { ESTADOS_POR_MODALIDAD, etiquetaAccion } from './acciones-entrega';
import { AvanceEntrega } from './avance-entrega';

type Fila = deliveryApi.EntregaResumen;

const FECHA = new FechaPipe();
const MODALIDADES = [
  { valor: 'DELIVERY_LOCAL', etiqueta: 'Delivery local' },
  { valor: 'AGENCIA', etiqueta: 'Agencia' },
] as const;

/**
 * Entregas (P-6-01, `/entregas`): pestañas Delivery local y Agencia con un contador por estado, las pendientes
 * primero y filtros por estado, fecha programada, repartidor y código de pedido, todos en la URL (CA-6-31 y CA-6-32).
 * La fila ofrece como acción el siguiente paso de la entrega (E1); "No se pudo entregar" va en el detalle.
 */
@Component({
  selector: 'fs-entregas',
  imports: [EncabezadoPagina, BarraFiltros, FsTabla, FsCelda, EstadoError, InsigniaEstado],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .fs-pestanas {
      display: flex;
      gap: 4px;
      margin: 16px 0;
      border-bottom: 1px solid var(--fs-color-linea);
    }
    .fs-pestana {
      padding: 8px 16px;
      border: 0;
      border-bottom: 2px solid transparent;
      background: none;
      color: var(--fs-color-texto-suave);
      font: inherit;
      cursor: pointer;
    }
    .fs-pestana--activa {
      color: var(--fs-color-texto);
      border-bottom-color: var(--fs-color-marca, currentColor);
      font-weight: 600;
    }
  `,
  template: `
    <fs-encabezado-pagina titulo="Entregas" />
    <div class="fs-pestanas" role="tablist" aria-label="Modalidad de la entrega">
      @for (m of modalidades; track m.valor) {
        <button
          type="button"
          role="tab"
          class="fs-pestana"
          [class.fs-pestana--activa]="modalidad() === m.valor"
          [attr.aria-selected]="modalidad() === m.valor"
          (click)="cambiarModalidad(m.valor)"
        >
          {{ m.etiqueta }}
        </button>
      }
    </div>
    <fs-barra-filtros
      placeholder="Buscar por código de pedido"
      [selects]="selects()"
      [chips]="chips()"
      [contadores]="contadores()"
      (cambio)="reiniciarPagina()"
    />
    <div class="fs-campo" style="max-width: 220px">
      <label class="fs-campo__etiqueta" for="filtro-fecha">Fecha programada</label>
      <input
        id="filtro-fecha"
        type="date"
        class="fs-input"
        [value]="fecha()"
        (input)="poner('fecha', $any($event.target).value)"
      />
    </div>
    @if (error()) {
      <fs-estado-error
        texto="No pudimos cargar las entregas."
        (reintentar)="reintento.next(reintento.value + 1)"
      />
    } @else {
      <fs-tabla
        [columnas]="columnas"
        [datos]="datos()"
        [cargando]="cargando()"
        columnaId="codigoPedido"
        [filaEnlace]="enlaceFila"
        [acciones]="acciones"
        [tituloVacio]="'Sin entregas'"
        [textoVacio]="
          'No hay entregas ' +
          (modalidad() === 'AGENCIA' ? 'por agencia' : 'de delivery local') +
          ' con estos filtros.'
        "
        (consulta)="paginar($event)"
      >
        <ng-template fsCelda="estado" let-fila>
          <fs-insignia-estado tipo="entrega" [estado]="fila.estado" />
        </ng-template>
      </fs-tabla>
    }
  `,
})
export class EntregasPage {
  private readonly api = inject(deliveryApi.EntregasService);
  private readonly repartidoresApi = inject(deliveryApi.RepartidoresService);
  private readonly avance = inject(AvanceEntrega);
  private readonly toasts = inject(ToastService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destruir = inject(DestroyRef);

  protected readonly modalidades = MODALIDADES;
  protected readonly modalidad = signal<'DELIVERY_LOCAL' | 'AGENCIA'>('DELIVERY_LOCAL');
  protected readonly fecha = signal('');
  protected readonly chips = signal<ChipFiltro[]>([]);
  protected readonly selects = signal<SelectFiltro[]>([]);
  protected readonly contadores = signal<Record<string, number> | null>(null);
  protected readonly datos = signal<Pagina<Fila> | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);
  protected readonly reintento = new BehaviorSubject(0);

  protected readonly columnas: ColumnaTabla<Fila>[] = [
    { clave: 'codigoPedido', titulo: 'Pedido', valor: (f) => f.codigoPedido, ordenable: true },
    { clave: 'destinatarioNombre', titulo: 'Destinatario', valor: (f) => f.destinatarioNombre },
    { clave: 'distrito', titulo: 'Distrito', valor: (f) => f.distrito ?? '—' },
    { clave: 'estado', titulo: 'Estado' },
    {
      clave: 'fechaProgramada',
      titulo: 'Fecha',
      valor: (f) => (f.fechaProgramada ? FECHA.transform(f.fechaProgramada) : '—'),
      ordenable: true,
    },
    {
      clave: 'responsable',
      titulo: 'Repartidor / agencia',
      valor: (f) => f.repartidorNombre ?? f.agenciaNombre ?? '—',
    },
    { clave: 'intento', titulo: 'Intento', valor: (f) => (f.intento > 0 ? f.intento : '—') },
  ];
  protected readonly enlaceFila = (fila: Fila): unknown[] => ['/entregas', fila.id];
  /** El siguiente paso como botón de la fila (E1); en los estados finales solo queda "Ver ›". */
  protected readonly acciones = (fila: Fila): AccionFila<Fila>[] =>
    fila.siguienteAccion
      ? [
          {
            etiqueta: etiquetaAccion(fila.siguienteAccion, fila.modalidad),
            alEjecutar: (f) => this.avanzar(f),
          },
        ]
      : [];

  constructor() {
    this.repartidoresApi
      .listarRepartidores(undefined, 'body', false, {
        context: new HttpContext().set(SIN_TOAST, true),
      })
      .pipe(
        takeUntilDestroyed(this.destruir),
        catchError(() => EMPTY),
      )
      .subscribe((r) => this.armarFiltros(r.data ?? []));
    this.armarFiltros([]);

    combineLatest([this.ruta.queryParamMap, this.reintento])
      .pipe(
        tap(([p]) => {
          this.modalidad.set(p.get('modalidad') === 'AGENCIA' ? 'AGENCIA' : 'DELIVERY_LOCAL');
          this.fecha.set(p.get('fecha') ?? '');
          this.chips.set(this.chipsDe(this.modalidad()));
          this.cargando.set(true);
          this.error.set(false);
        }),
        switchMap(([p]) =>
          this.api
            .listarEntregas(
              this.modalidad(),
              (p.get('estado') as deliveryApi.EstadoEntrega | null) ?? undefined,
              p.get('fecha') ?? undefined,
              p.get('repartidorId') ? Number(p.get('repartidorId')) : undefined,
              p.get('q') ?? undefined,
              Number(p.get('page') ?? 0),
              Number(p.get('size') ?? 20),
              p.getAll('sort').length > 0 ? p.getAll('sort') : undefined,
              'body',
              false,
              { context: new HttpContext().set(SIN_TOAST, true) },
            )
            .pipe(
              map((r) => r.data as deliveryApi.PaginaEntregas),
              catchError(() => {
                this.error.set(true);
                this.cargando.set(false);
                return EMPTY;
              }),
              startWith(undefined),
            ),
        ),
        takeUntilDestroyed(this.destruir),
      )
      .subscribe((pagina) => {
        if (pagina) {
          this.datos.set(pagina as unknown as Pagina<Fila>);
          this.contadores.set(pagina.contadores as Record<string, number>);
          this.cargando.set(false);
        }
      });
  }

  private chipsDe(modalidad: string): ChipFiltro[] {
    return (ESTADOS_POR_MODALIDAD[modalidad] ?? []).map((valor) => ({
      valor,
      etiqueta: ESTADOS.entrega[valor]?.etiqueta ?? valor,
    }));
  }

  private armarFiltros(repartidores: deliveryApi.Repartidor[]): void {
    this.selects.set([
      {
        clave: 'repartidorId',
        etiqueta: 'Repartidor',
        opciones: repartidores.map((r) => ({ valor: '' + r.id, etiqueta: r.nombres })),
      },
    ]);
  }

  protected cambiarModalidad(modalidad: string): void {
    void this.router.navigate([], {
      relativeTo: this.ruta,
      queryParams: {
        modalidad: modalidad === 'DELIVERY_LOCAL' ? null : modalidad,
        estado: null,
        page: null,
      },
      queryParamsHandling: 'merge',
    });
  }

  protected poner(clave: string, valor: string): void {
    void this.router.navigate([], {
      relativeTo: this.ruta,
      queryParams: { [clave]: valor || null, page: null },
      queryParamsHandling: 'merge',
    });
  }

  protected reiniciarPagina(): void {
    if (this.ruta.snapshot.queryParamMap.has('page')) this.poner('page', '');
  }

  protected paginar(c: ConsultaPagina): void {
    void this.router.navigate([], {
      relativeTo: this.ruta,
      queryParams: {
        page: c.page === 0 ? null : c.page,
        size: c.size === 20 ? null : c.size,
        sort: c.sort?.length ? c.sort : null,
      },
      queryParamsHandling: 'merge',
    });
  }

  private avanzar(fila: Fila): void {
    this.avance.ejecutarPorId(fila.id, fila.siguienteAccion as string).subscribe({
      next: (entrega) => {
        if (!entrega) return;
        this.toasts.mostrar('exito', 'Entrega actualizada');
        this.reintento.next(this.reintento.value + 1);
      },
      // El interceptor ya mostró el motivo (por ejemplo, "La entrega cambió; recarga la página")
      error: () => this.reintento.next(this.reintento.value + 1),
    });
  }
}
