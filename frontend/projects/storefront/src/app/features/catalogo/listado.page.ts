import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { catalogApi } from 'shared';
import { combineLatest, map } from 'rxjs';
import { FsPaginador } from '../../ui/paginador/paginador';
import { FsTarjetaProducto } from '../../ui/tarjeta-producto/tarjeta-producto';
import { EstadoCargando, EstadoError, EstadoVacio } from '../../ui/estados/estados';
import { ChipsFiltrosActivos } from './chips-filtros-activos';
import {
  ORDENES,
  TAMANO_PAGINA,
  aQuery,
  hayFiltros,
  leerFiltros,
  type Filtros,
  type Orden,
} from './filtros';
import { PanelFiltros } from './panel-filtros';

/** P-2-01: listado de productos con filtros en la URL (C3), chips, orden y paginador. */
@Component({
  selector: 'fs-listado-page',
  imports: [
    PanelFiltros,
    ChipsFiltrosActivos,
    FsTarjetaProducto,
    FsPaginador,
    EstadoCargando,
    EstadoVacio,
    EstadoError,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .listado {
      display: grid;
      grid-template-columns: 240px minmax(0, 1fr);
      gap: 32px;
      padding-block: 32px;
    }
    .barra {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 16px;
      flex-wrap: wrap;
      margin-bottom: 16px;
    }
    .rejilla {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 24px;
    }
    fs-paginador {
      display: flex;
      justify-content: center;
      margin-top: 32px;
    }
    @media (max-width: 800px) {
      .listado {
        grid-template-columns: minmax(0, 1fr);
      }
    }
  `,
  template: `
    <section class="fs-pagina listado">
      <fs-panel-filtros
        [filtros]="filtros()"
        [arbol]="arbol()"
        [tallas]="tallas()"
        [colores]="colores()"
        (cambio)="aplicar($event)"
      />
      <div>
        <h1>{{ titulo() }}</h1>
        <div class="barra">
          <fs-chips-filtros-activos
            [filtros]="filtros()"
            [colores]="colores()"
            (cambio)="aplicar($event)"
          />
          <label>
            Ordenar por
            <select
              data-orden
              [value]="filtros().orden"
              (change)="ordenar($any($event.target).value)"
            >
              @for (o of ordenes; track o.valor) {
                <option [value]="o.valor" [selected]="o.valor === filtros().orden">
                  {{ o.etiqueta }}
                </option>
              }
            </select>
          </label>
        </div>
        @if (resultado.error()) {
          <fs-estado-error
            titulo="No pudimos cargar los productos"
            (reintentar)="intento.update((n) => n + 1)"
          />
        } @else if (resultado.isLoading() || !pagina()) {
          <div class="rejilla" role="status" aria-busy="true">
            @for (i of esqueletos; track i) {
              <fs-tarjeta-producto />
            }
          </div>
        } @else if (pagina()!.contenido.length === 0) {
          <fs-estado-vacio
            titulo="No encontramos productos con estos filtros"
            texto="Prueba quitando algún filtro."
            accion="Limpiar filtros"
            (accionar)="limpiar()"
          />
        } @else {
          <div class="rejilla">
            @for (p of pagina()!.contenido; track p.id) {
              <fs-tarjeta-producto [producto]="p" />
            }
          </div>
          <fs-paginador
            [pagina]="pagina()!.pagina"
            [totalPaginas]="pagina()!.totalPaginas"
            (cambio)="irA($event)"
          />
        }
      </div>
    </section>
  `,
})
export class ListadoPage {
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly tienda = inject(catalogApi.TiendaService);

  protected readonly ordenes = ORDENES;
  protected readonly esqueletos = [0, 1, 2, 3, 4, 5, 6, 7];
  protected readonly intento = signal(0);

  protected readonly filtros = toSignal(
    combineLatest([this.ruta.paramMap, this.ruta.queryParamMap]).pipe(
      map(([r, q]) => leerFiltros(r, q)),
    ),
    { initialValue: leerFiltros(this.ruta.snapshot.paramMap, this.ruta.snapshot.queryParamMap) },
  );

  protected readonly resultado = rxResource({
    params: () => ({ f: this.filtros(), intento: this.intento() }),
    stream: ({ params: { f } }) =>
      this.tienda
        .listarProductos(
          f.q || undefined,
          f.categoria ?? undefined,
          f.tallas.length ? f.tallas : undefined,
          f.colores.length ? f.colores : undefined,
          f.min ?? undefined,
          f.max ?? undefined,
          f.disponible ? true : undefined,
          f.pagina,
          TAMANO_PAGINA,
          f.orden,
        )
        .pipe(map((r) => r.data ?? null)),
  });
  protected readonly pagina = computed(() =>
    this.resultado.hasValue() ? this.resultado.value() : null,
  );

  private readonly maestros = rxResource({
    stream: () =>
      combineLatest([
        this.tienda.obtenerArbolCategorias().pipe(map((r) => r.data ?? [])),
        this.tienda.listarTallas().pipe(map((r) => r.data ?? [])),
        this.tienda.listarColores().pipe(map((r) => r.data ?? [])),
      ]),
  });
  protected readonly arbol = computed(() => this.maestros.value()?.[0] ?? []);
  protected readonly tallas = computed(() => this.maestros.value()?.[1] ?? []);
  protected readonly colores = computed(() => this.maestros.value()?.[2] ?? []);

  protected readonly titulo = computed(() => {
    const slug = this.filtros().categoria;
    if (!slug) return 'Todos los productos';
    const nodos = this.arbol().flatMap((c) => [c, ...c.hijas]);
    return nodos.find((c) => c.slug === slug)?.nombre ?? 'Productos';
  });

  protected aplicar(f: Filtros): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: aQuery(f) });
  }

  protected ordenar(orden: Orden): void {
    this.aplicar({ ...this.filtros(), orden, pagina: 0 });
  }

  protected irA(pagina: number): void {
    this.aplicar({ ...this.filtros(), pagina });
  }

  protected limpiar(): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: {} });
  }

  protected readonly hayFiltros = computed(() => hayFiltros(this.filtros()));
}
