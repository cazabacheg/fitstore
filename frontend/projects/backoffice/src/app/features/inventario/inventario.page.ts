import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastService, catalogApi, type ConsultaPagina, type Pagina } from 'shared';
import { map } from 'rxjs';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { EstadoError } from '../../ui/estados/estado-error';
import { BarraFiltros, type ChipFiltro } from '../../ui/filtros/barra-filtros';
import { FsTabla, type AccionFila, type ColumnaTabla } from '../../ui/tabla/fs-tabla';
import { DialogoEntrada, type DatosDialogoEntrada } from './dialogo-entrada';
import { DialogoMinimo, type DatosDialogoMinimo } from './dialogo-minimo';

const CHIPS: ChipFiltro[] = [
  { valor: 'BAJO', etiqueta: 'Stock bajo' },
  { valor: 'AGOTADO', etiqueta: 'Agotados' },
];

/** P-2-07: inventario por SKU con filas marcadas por stock bajo y agotado (R11: el SKU abre el kardex). */
@Component({
  selector: 'fs-inventario-page',
  imports: [EncabezadoPagina, BarraFiltros, FsTabla, EstadoError],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fs-encabezado-pagina titulo="Inventario">
      <div accion>
        <button type="button" class="fs-btn fs-btn--primario" (click)="registrarEntrada()">
          Registrar entrada
        </button>
      </div>
    </fs-encabezado-pagina>
    <fs-barra-filtros placeholder="Buscar por SKU o producto" [chips]="chips" claveChip="estado" />
    @if (lista.error()) {
      <fs-estado-error (reintentar)="lista.reload()" />
    } @else {
      <fs-tabla
        [columnas]="columnas"
        [datos]="datos()"
        [cargando]="lista.isLoading()"
        columnaId="sku"
        [filaEnlace]="destino"
        [acciones]="acciones"
        [claseFila]="claseFila"
        tituloVacio="No hay SKU con estos filtros"
        (consulta)="consulta.set($event)"
      />
    }
  `,
})
export class InventarioPage {
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(catalogApi.InventarioService);
  private readonly dialogo = inject(MatDialog);
  private readonly toasts = inject(ToastService);

  protected readonly chips = CHIPS;
  protected readonly consulta = signal<ConsultaPagina>({ page: 0, size: 20 });
  private readonly filtros = toSignal(this.ruta.queryParamMap, {
    initialValue: this.ruta.snapshot.queryParamMap,
  });

  protected readonly lista = rxResource({
    params: () => ({ f: this.filtros(), c: this.consulta() }),
    stream: ({ params: { f, c } }) =>
      this.api
        .listarStock(
          f.get('q') ?? undefined,
          undefined,
          f.get('estado') === 'BAJO' ? true : undefined,
          f.get('estado') === 'AGOTADO' ? true : undefined,
          c.page,
          c.size,
          c.sort,
        )
        .pipe(map((r) => r.data as Pagina<catalogApi.Stock>)),
  });
  protected readonly datos = computed(() => (this.lista.hasValue() ? this.lista.value() : null));

  protected readonly columnas: ColumnaTabla<catalogApi.Stock>[] = [
    { clave: 'sku', titulo: 'SKU', ordenable: true, valor: (s) => s.sku },
    { clave: 'producto', titulo: 'Producto', valor: (s) => s.producto },
    { clave: 'talla', titulo: 'Talla', valor: (s) => s.talla },
    { clave: 'color', titulo: 'Color', valor: (s) => s.color },
    { clave: 'fisico', titulo: 'Físico', valor: (s) => s.fisico },
    { clave: 'reservado', titulo: 'Reservado', valor: (s) => s.reservado },
    { clave: 'disponible', titulo: 'Disponible', ordenable: true, valor: (s) => s.disponible },
    {
      clave: 'minimo',
      titulo: 'Mínimo',
      valor: (s) => (s.minimoPropio ? `${s.minimo}` : `${s.minimo} (tienda)`),
    },
  ];
  protected readonly destino = (s: catalogApi.Stock) => ['/inventario', s.varianteId];
  protected readonly claseFila = (s: catalogApi.Stock): 'aviso' | 'error' | null =>
    s.estado === 'AGOTADO' ? 'error' : s.estado === 'BAJO' ? 'aviso' : null;
  protected readonly acciones = (s: catalogApi.Stock): AccionFila<catalogApi.Stock>[] => [
    {
      etiqueta: 'Kardex ›',
      alEjecutar: () => void this.router.navigate(['/inventario', s.varianteId]),
    },
    { etiqueta: 'Mínimo', alEjecutar: () => this.cambiarMinimo(s) },
  ];

  protected registrarEntrada(): void {
    const datos: DatosDialogoEntrada = { opciones: this.datos()?.contenido ?? [] };
    this.dialogo
      .open<DialogoEntrada, DatosDialogoEntrada, catalogApi.EntradaRequest | null>(DialogoEntrada, {
        data: datos,
        maxWidth: '100%',
      })
      .afterClosed()
      .subscribe((cuerpo) => {
        if (!cuerpo) return;
        this.api.registrarEntrada(cuerpo).subscribe(() => {
          this.toasts.mostrar('exito', 'Entrada registrada.');
          this.lista.reload();
        });
      });
  }

  private cambiarMinimo(stock: catalogApi.Stock): void {
    const datos: DatosDialogoMinimo = { stock };
    this.dialogo
      .open<DialogoMinimo, DatosDialogoMinimo, catalogApi.MinimoRequest | null>(DialogoMinimo, {
        data: datos,
        width: '440px',
        maxWidth: '100%',
      })
      .afterClosed()
      .subscribe((cuerpo) => {
        if (!cuerpo) return;
        this.api.cambiarMinimoVariante(stock.varianteId, cuerpo).subscribe(() => {
          this.toasts.mostrar('exito', 'Mínimo guardado.');
          this.lista.reload();
        });
      });
  }
}
