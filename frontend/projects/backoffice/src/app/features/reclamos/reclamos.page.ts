import { HttpContext } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
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
import { FechaPipe, SIN_TOAST, settingsApi, type ConsultaPagina, type Pagina } from 'shared';
import { ReclamosService } from 'shared/api/settings-service';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { BarraFiltros, type ChipFiltro, type SelectFiltro } from '../../ui/filtros/barra-filtros';
import { EstadoError } from '../../ui/estados/estado-error';
import { FsCelda } from '../../ui/tabla/fs-celda';
import { FsTabla, type ColumnaTabla } from '../../ui/tabla/fs-tabla';
import { ESTADOS_RECLAMO, TIPOS_RECLAMO, claseInsignia } from './estados-reclamo';

type Fila = settingsApi.ReclamoResumen;

const FECHA = new FechaPipe();

/**
 * Bandeja de reclamos (P-3-06, `/reclamos`): OPERADOR consulta, ADMIN atiende. Filtros por estado, tipo, plazo y
 * texto (número, nombre o documento), con "por vencer" (3 días hábiles o menos) y "vencido" resaltados (R11).
 * Los filtros, la página y el orden viven en la URL.
 */
@Component({
  selector: 'fs-reclamos',
  imports: [EncabezadoPagina, BarraFiltros, FsTabla, FsCelda, EstadoError],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fs-encabezado-pagina titulo="Reclamos" />
    <fs-barra-filtros
      placeholder="Buscar por número, nombre o documento"
      [selects]="selects"
      [chips]="chips"
      (cambio)="reiniciarPagina()"
    />
    @if (error()) {
      <fs-estado-error
        texto="No pudimos cargar los reclamos."
        (reintentar)="reintento.next(reintento.value + 1)"
      />
    } @else {
      <fs-tabla
        [columnas]="columnas"
        [datos]="datos()"
        [cargando]="cargando()"
        columnaId="numero"
        [filaEnlace]="enlaceFila"
        [claseFila]="claseFila"
        tituloVacio="Sin reclamos"
        textoVacio="No hay reclamos con estos filtros."
        (consulta)="paginar($event)"
      >
        <ng-template fsCelda="tipo" let-fila>{{ tipo(fila) }}</ng-template>
        <ng-template fsCelda="estado" let-fila>
          <span [class]="clase(fila)">{{ etiquetaEstado(fila) }}</span>
        </ng-template>
        <ng-template fsCelda="plazo" let-fila>
          @if (fila.vencido) {
            <span class="fs-insignia fs-insignia--error">Vencido</span>
          } @else if (fila.porVencer) {
            <span class="fs-insignia fs-insignia--aviso">Por vencer</span>
          } @else {
            <span class="fs-suave">En plazo</span>
          }
        </ng-template>
      </fs-tabla>
    }
  `,
})
export class ReclamosPage {
  private readonly api = inject(ReclamosService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly selects: SelectFiltro[] = [
    {
      clave: 'tipo',
      etiqueta: 'Tipo',
      opciones: [
        { valor: 'RECLAMO', etiqueta: 'Reclamo' },
        { valor: 'QUEJA', etiqueta: 'Queja' },
      ],
    },
    {
      clave: 'plazo',
      etiqueta: 'Plazo',
      opciones: [
        { valor: 'porVencer', etiqueta: 'Por vencer' },
        { valor: 'vencidos', etiqueta: 'Vencidos' },
      ],
    },
  ];
  protected readonly chips: ChipFiltro[] = [
    { valor: 'REGISTRADO', etiqueta: 'Registrado' },
    { valor: 'EN_ATENCION', etiqueta: 'En atención' },
    { valor: 'RESPONDIDO', etiqueta: 'Respondido' },
  ];
  protected readonly columnas: ColumnaTabla<Fila>[] = [
    { clave: 'numero', titulo: 'Reclamo', valor: (f) => f.numero, ordenable: true },
    { clave: 'consumidorNombre', titulo: 'Consumidor', valor: (f) => f.consumidorNombre },
    { clave: 'tipo', titulo: 'Tipo' },
    { clave: 'estado', titulo: 'Estado' },
    {
      clave: 'fechaLimite',
      titulo: 'Fecha límite',
      valor: (f) => FECHA.transform(f.fechaLimite),
      ordenable: true,
    },
    { clave: 'plazo', titulo: 'Plazo' },
  ];
  protected readonly enlaceFila = (fila: Fila): unknown[] => ['/reclamos', fila.id];
  protected readonly claseFila = (fila: Fila): 'aviso' | 'error' | null =>
    fila.vencido ? 'error' : fila.porVencer ? 'aviso' : null;

  protected readonly datos = signal<Pagina<Fila> | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);
  protected readonly reintento = new BehaviorSubject(0);

  constructor() {
    combineLatest([this.ruta.queryParamMap, this.reintento])
      .pipe(
        tap(() => {
          this.cargando.set(true);
          this.error.set(false);
        }),
        switchMap(([p]) => {
          const plazo = p.get('plazo');
          return this.api
            .listarReclamos(
              (p.get('estado') as settingsApi.EstadoReclamo | null) ?? undefined,
              (p.get('tipo') as settingsApi.TipoReclamo | null) ?? undefined,
              plazo === 'porVencer' ? true : undefined,
              plazo === 'vencidos' ? true : undefined,
              p.get('q') ?? undefined,
              p.get('desde') ?? undefined,
              p.get('hasta') ?? undefined,
              Number(p.get('page') ?? 0),
              Number(p.get('size') ?? 20),
              p.getAll('sort').length > 0 ? p.getAll('sort') : undefined,
              'body',
              false,
              { context: new HttpContext().set(SIN_TOAST, true) },
            )
            .pipe(
              map((r) => r.data as Pagina<Fila>),
              catchError(() => {
                this.error.set(true);
                this.cargando.set(false);
                return EMPTY;
              }),
              startWith(undefined),
            );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((pagina) => {
        if (pagina) {
          this.datos.set(pagina);
          this.cargando.set(false);
        }
      });
  }

  protected reiniciarPagina(): void {
    if (this.ruta.snapshot.queryParamMap.has('page')) {
      void this.router.navigate([], {
        relativeTo: this.ruta,
        queryParams: { page: null },
        queryParamsHandling: 'merge',
      });
    }
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

  protected tipo(fila: Fila): string {
    return TIPOS_RECLAMO[fila.tipo] ?? fila.tipo;
  }

  protected etiquetaEstado(fila: Fila): string {
    return ESTADOS_RECLAMO[fila.estado]?.etiqueta ?? fila.estado;
  }

  protected clase(fila: Fila): string {
    return claseInsignia(ESTADOS_RECLAMO[fila.estado]?.variante ?? 'neutra');
  }
}
