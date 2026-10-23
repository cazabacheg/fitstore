import { NgTemplateOutlet } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  contentChildren,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorIntl, MatPaginatorModule, type PageEvent } from '@angular/material/paginator';
import { MatSortModule, type Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { Router, RouterLink } from '@angular/router';
import type { ConsultaPagina, Pagina } from 'shared';
import { EstadoCargando } from '../estados/estado-cargando';
import { EstadoVacio } from '../estados/estado-vacio';
import { FsCelda } from './fs-celda';
import { PaginadorEs } from './paginador-es';

export interface ColumnaTabla<T> {
  clave: string;
  titulo: string;
  /** Texto de la celda. Sin él, se usa la plantilla `fsCelda` de la misma clave. */
  valor?: (fila: T) => string | number;
  /** Se puede ordenar por esta columna (`sort=clave,asc|desc`). */
  ordenable?: boolean;
}

export interface AccionFila<T> {
  etiqueta: string;
  alEjecutar: (fila: T) => void;
}

/** Destino del detalle de una fila: una URL o los segmentos de `routerLink`. */
export type DestinoFila = string | unknown[];

/**
 * Tabla paginada del backoffice (`mat-table` + paginador + orden) con la convención de filas R11:
 * identificador como enlace, fila clicable y última columna de acciones ("Ver ›", 1 o 2 botones, o el menú "⋮").
 * Emite `consulta` con `page`, `size` y `sort` en el formato de la API (ADR-015).
 */
@Component({
  selector: 'fs-tabla',
  imports: [
    NgTemplateOutlet,
    RouterLink,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatMenuModule,
    MatButtonModule,
    EstadoCargando,
    EstadoVacio,
  ],
  providers: [{ provide: MatPaginatorIntl, useClass: PaginadorEs }],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './fs-tabla.html',
  styleUrl: './fs-tabla.css',
})
export class FsTabla<T extends object> {
  private readonly router = inject(Router);

  readonly columnas = input.required<ColumnaTabla<T>[]>();
  /** Página del contrato (K6). */
  readonly datos = input<Pagina<T> | null>(null);
  readonly cargando = input(false);
  /** Clave de la columna del identificador (código, nombre o número): es el enlace de la fila. */
  readonly columnaId = input.required<string>();
  /** Destino del detalle de cada fila. Sin él (y sin `abribleEnDialogo`) la tabla no tiene detalle y las filas no son clicables. */
  readonly filaEnlace = input<((fila: T) => unknown) | null>(null);
  /** Para entidades sin página de detalle: el identificador abre su diálogo (evento `alAbrir`). */
  readonly abribleEnDialogo = input(false);
  /** Acciones propias de cada fila. */
  readonly acciones = input<((fila: T) => AccionFila<T>[]) | null>(null);
  /** Variante de color de la fila: por vencer (`aviso`) o vencida/agotada (`error`). */
  readonly claseFila = input<((fila: T) => 'aviso' | 'error' | null) | null>(null);
  readonly tamanosPagina = input([10, 20, 50, 100]);
  readonly tituloVacio = input('Sin resultados');
  readonly textoVacio = input('Prueba con otros filtros.');

  readonly consulta = output<ConsultaPagina>();
  readonly alAbrir = output<T>();

  private readonly celdas = contentChildren(FsCelda);
  protected readonly plantillas = computed(
    () => new Map(this.celdas().map((c) => [c.fsCelda(), c.plantilla])),
  );
  /** Orden elegido: vive en la tabla (no en el `matSort`) para sobrevivir a la recarga que la reemplaza por el esqueleto. */
  protected readonly ordenActual = signal<Sort>({ active: '', direction: '' });
  private readonly orden = signal<string[]>([]);

  protected readonly conDetalle = computed(
    () => this.filaEnlace() !== null || this.abribleEnDialogo(),
  );
  protected readonly filas = computed(() => this.datos()?.contenido ?? []);
  protected readonly vacia = computed(() => this.filas().length === 0);
  protected readonly claves = computed(() => [...this.columnas().map((c) => c.clave), 'acciones']);

  protected destino(fila: T): DestinoFila | null {
    const d = this.filaEnlace()?.(fila);
    return d === null || d === undefined ? null : (d as DestinoFila);
  }

  protected accionesDe(fila: T): AccionFila<T>[] {
    return this.acciones()?.(fila) ?? [];
  }

  protected texto(columna: ColumnaTabla<T>, fila: T): string | number {
    return columna.valor?.(fila) ?? '';
  }

  protected alOrdenar(orden: Sort): void {
    this.ordenActual.set(orden);
    this.orden.set(orden.direction ? [`${orden.active},${orden.direction}`] : []);
    this.consulta.emit({ page: 0, size: this.datos()?.tamano ?? 20, sort: this.orden() });
  }

  protected alPaginar(evento: PageEvent): void {
    this.consulta.emit({ page: evento.pageIndex, size: evento.pageSize, sort: this.orden() });
  }

  /** Abre el detalle de la fila: navega a su destino o avisa para abrir el diálogo. */
  protected abrir(fila: T, evento?: Event): void {
    evento?.preventDefault();
    const destino = this.destino(fila);
    if (destino === null) this.alAbrir.emit(fila);
    else void this.router.navigate(Array.isArray(destino) ? destino : [destino]);
  }

  /** Clic en la fila: abre el detalle, salvo que el clic venga de un enlace, un botón o un menú. */
  protected clicFila(evento: MouseEvent, fila: T): void {
    if (!this.conDetalle()) return;
    if ((evento.target as HTMLElement).closest('a, button, [role="menuitem"]')) return;
    const destino = this.destino(fila);
    if ((evento.ctrlKey || evento.metaKey) && destino !== null) {
      window.open(
        this.router.serializeUrl(
          this.router.createUrlTree(Array.isArray(destino) ? destino : [destino]),
        ),
        '_blank',
      );
      return;
    }
    this.abrir(fila);
  }
}
