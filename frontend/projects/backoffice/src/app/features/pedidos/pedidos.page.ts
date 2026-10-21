import { HttpContext } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink, type ParamMap } from '@angular/router';
import { FechaPipe, PrecioPipe, SIN_TOAST, orderApi, type ConsultaPagina, type Pagina } from 'shared';
import { Subscription } from 'rxjs';
import { EstadoError } from '../../ui/estados/estado-error';
import { BarraFiltros, type ChipFiltro, type SelectFiltro } from '../../ui/filtros/barra-filtros';
import { ESTADOS } from '../../ui/insignia/estados';
import { InsigniaEstado } from '../../ui/insignia/insignia-estado';
import { FsCelda } from '../../ui/tabla/fs-celda';
import { FsTabla, type ColumnaTabla } from '../../ui/tabla/fs-tabla';

type Fila = orderApi.PedidoAdminResumen;

const NOMBRE_MODALIDAD: Record<string, string> = {
  DELIVERY_LOCAL: 'Delivery local',
  AGENCIA: 'Agencia',
  RECOJO: 'Recojo',
};

const CHIPS: ChipFiltro[] = Object.entries(ESTADOS.pedido).map(([valor, d]) => ({ valor, etiqueta: d.etiqueta }));
const SELECTS: SelectFiltro[] = [
  {
    clave: 'modalidad',
    etiqueta: 'Modalidad',
    opciones: Object.entries(NOMBRE_MODALIDAD).map(([valor, etiqueta]) => ({ valor, etiqueta })),
  },
];

/** Pedidos del backoffice (P-4-05): tabla paginada con filtros en la URL y un contador por estado (CA-4-40 a CA-4-42). */
@Component({
  selector: 'fs-pedidos-page',
  imports: [BarraFiltros, FsTabla, FsCelda, InsigniaEstado, EstadoError, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1>Pedidos</h1>
    <fs-barra-filtros
      placeholder="Buscar por código, nombre, correo o DNI"
      [selects]="selects"
      [chips]="chips"
      [contadores]="contadores()"
      (cambio)="alCambiarFiltros()"
    />
    <div class="fs-filtros__controles" style="margin: var(--espacio-3) 0">
      <label>
        Desde
        <input type="date" class="fs-input" aria-label="Desde" [value]="valor('desde')" (change)="poner('desde', $event)" />
      </label>
      <label>
        Hasta
        <input type="date" class="fs-input" aria-label="Hasta" [value]="valor('hasta')" (change)="poner('hasta', $event)" />
      </label>
      @if (valor('clienteId')) {
        <span class="fs-chip-cliente">
          Pedidos de {{ nombreCliente() }}
          <a [routerLink]="[]" [queryParams]="{ clienteId: null }" queryParamsHandling="merge">Quitar</a>
        </span>
      }
    </div>
    @if (error()) {
      <fs-estado-error titulo="No pudimos cargar los pedidos" (reintentar)="recargar()" />
    } @else {
      <fs-tabla
        [columnas]="columnas"
        [datos]="datos()"
        [cargando]="cargando()"
        columnaId="codigo"
        [filaEnlace]="enlace"
        tituloVacio="No hay pedidos con esos filtros"
        (consulta)="consulta.set($event)"
      >
        <ng-template fsCelda="estado" let-fila>
          <fs-insignia-estado tipo="pedido" [estado]="fila.estado" />
        </ng-template>
      </fs-tabla>
    }
  `,
})
export class PedidosPage {
  private readonly api = inject(orderApi.PedidosAdminService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private carga: Subscription | null = null;

  protected readonly selects = SELECTS;
  protected readonly chips = CHIPS;
  protected readonly columnas: ColumnaTabla<Fila>[] = [
    { clave: 'codigo', titulo: 'Pedido', valor: (f) => f.codigo },
    { clave: 'creadoEn', titulo: 'Fecha', valor: (f) => new FechaPipe().transform(f.creadoEn, 'fechaHora'), ordenable: true },
    { clave: 'clienteNombre', titulo: 'Cliente', valor: (f) => f.clienteNombre },
    { clave: 'modalidad', titulo: 'Modalidad', valor: (f) => NOMBRE_MODALIDAD[f.modalidad] ?? f.modalidad },
    { clave: 'estado', titulo: 'Estado' },
    { clave: 'total', titulo: 'Total', valor: (f) => new PrecioPipe().transform(f.total), ordenable: true },
  ];
  protected readonly enlace = (fila: Fila) => ['/pedidos', fila.id];

  private readonly parametros = toSignal(this.ruta.queryParamMap, { initialValue: this.ruta.snapshot.queryParamMap });
  protected readonly consulta = signal<ConsultaPagina>({ page: 0, size: 20, sort: [] });
  protected readonly datos = signal<Pagina<Fila> | null>(null);
  protected readonly contadores = signal<Record<string, number> | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);
  protected readonly nombreCliente = signal('este cliente');

  constructor() {
    effect(() => {
      const parametros = this.parametros();
      const consulta = this.consulta();
      untracked(() => this.cargar(parametros, consulta));
    });
    inject(DestroyRef).onDestroy(() => this.carga?.unsubscribe());
  }

  protected valor(clave: string): string {
    return this.parametros().get(clave) ?? '';
  }

  /** Cambiar un filtro vuelve a la primera página. */
  protected alCambiarFiltros(): void {
    this.consulta.update((c) => ({ ...c, page: 0 }));
  }

  protected poner(clave: 'desde' | 'hasta', evento: Event): void {
    const valor = (evento.target as HTMLInputElement).value;
    this.alCambiarFiltros();
    void this.router.navigate([], {
      relativeTo: this.ruta,
      queryParams: { [clave]: valor || null },
      queryParamsHandling: 'merge',
    });
  }

  protected recargar(): void {
    this.cargar(this.parametros(), this.consulta());
  }

  private cargar(p: ParamMap, c: ConsultaPagina): void {
    this.carga?.unsubscribe();
    this.cargando.set(true);
    this.error.set(false);
    // El buscador acepta el código del pedido o datos del cliente (nombre, correo o DNI)
    const q = p.get('q') ?? '';
    const esCodigo = /^FS-\d{1,6}$/i.test(q);
    this.carga = this.api
      .listarPedidosAdmin(
        (p.get('estado') as orderApi.EstadoPedido) || undefined,
        (p.get('modalidad') as orderApi.Modalidad) || undefined,
        p.get('desde') || undefined,
        p.get('hasta') || undefined,
        esCodigo ? q.toUpperCase() : undefined,
        !esCodigo && q ? q : undefined,
        p.get('clienteId') || undefined,
        c.page,
        c.size,
        c.sort,
        'body',
        false,
        { context: new HttpContext().set(SIN_TOAST, true) },
      )
      .subscribe({
        next: (r) => {
          const pagina = r.data;
          this.datos.set(pagina ?? null);
          this.contadores.set(pagina?.contadores ?? null);
          if (p.get('clienteId') && pagina?.contenido[0]) this.nombreCliente.set(pagina.contenido[0].clienteNombre);
          this.cargando.set(false);
        },
        error: () => {
          this.cargando.set(false);
          this.error.set(true);
        },
      });
  }
}
