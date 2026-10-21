import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SesionStore, ToastService, catalogApi, type ConsultaPagina, type Pagina } from 'shared';
import { ProductosAdminService } from 'shared/api/catalog-service';
import { map } from 'rxjs';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { DialogoService } from '../../ui/dialogo/dialogo.service';
import { EstadoError } from '../../ui/estados/estado-error';
import { BarraFiltros, type ChipFiltro } from '../../ui/filtros/barra-filtros';
import { FsCelda } from '../../ui/tabla/fs-celda';
import { FsTabla, type AccionFila, type ColumnaTabla } from '../../ui/tabla/fs-tabla';

const ESTADOS: ChipFiltro[] = [
  { valor: 'BORRADOR', etiqueta: 'Borrador' },
  { valor: 'ACTIVO', etiqueta: 'Activo' },
  { valor: 'INACTIVO', etiqueta: 'Inactivo' },
];
const VARIANTE_ESTADO: Record<string, string> = {
  BORRADOR: 'aviso',
  ACTIVO: 'exito',
  INACTIVO: 'neutra',
};

/** P-2-03: tabla de productos. ADMIN: menú "⋮"; OPERADOR: "Ver ›" (R11). */
@Component({
  selector: 'fs-productos-page',
  imports: [RouterLink, EncabezadoPagina, BarraFiltros, FsTabla, FsCelda, EstadoError],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fs-encabezado-pagina titulo="Productos">
      @if (esAdmin()) {
        <div accion>
          <a class="fs-btn fs-btn--primario" routerLink="/catalogo/productos/nuevo"
            >Nuevo producto</a
          >
        </div>
      }
    </fs-encabezado-pagina>
    <fs-barra-filtros
      placeholder="Buscar por nombre o código"
      [chips]="estados"
      claveChip="estado"
    />
    @if (lista.error()) {
      <fs-estado-error (reintentar)="lista.reload()" />
    } @else {
      <fs-tabla
        [columnas]="columnas"
        [datos]="datos()"
        [cargando]="lista.isLoading()"
        columnaId="nombre"
        [filaEnlace]="destino"
        [acciones]="esAdmin() ? accionesAdmin : null"
        tituloVacio="Aún no hay productos"
        textoVacio="Prueba con otros filtros o crea un producto."
        (consulta)="consulta.set($event)"
      >
        <ng-template fsCelda="estado" let-fila>
          <span class="fs-insignia" [class]="'fs-insignia fs-insignia--' + variante(fila.estado)">{{
            etiqueta(fila.estado)
          }}</span>
        </ng-template>
      </fs-tabla>
    }
  `,
})
export class ProductosPage {
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(ProductosAdminService);
  private readonly dialogo = inject(DialogoService);
  private readonly toasts = inject(ToastService);

  private readonly sesion = inject(SesionStore);
  protected readonly esAdmin = computed(() => this.sesion.rol() === 'ADMIN');
  protected readonly estados = ESTADOS;
  protected readonly consulta = signal<ConsultaPagina>({ page: 0, size: 20 });
  private readonly filtros = toSignal(this.ruta.queryParamMap, {
    initialValue: this.ruta.snapshot.queryParamMap,
  });

  protected readonly lista = rxResource({
    params: () => ({ f: this.filtros(), c: this.consulta() }),
    stream: ({ params: { f, c } }) =>
      this.api
        .listarProductosAdmin(
          f.get('q') ?? undefined,
          (f.get('estado') as catalogApi.EstadoProducto | null) ?? undefined,
          undefined,
          undefined,
          c.page,
          c.size,
          c.sort,
        )
        .pipe(map((r) => r.data as Pagina<catalogApi.ProductoAdminResumen>)),
  });
  protected readonly datos = computed(() => (this.lista.hasValue() ? this.lista.value() : null));

  protected readonly columnas: ColumnaTabla<catalogApi.ProductoAdminResumen>[] = [
    { clave: 'nombre', titulo: 'Producto', ordenable: true, valor: (p) => p.nombre },
    { clave: 'codigo', titulo: 'Código', valor: (p) => p.codigo },
    { clave: 'estado', titulo: 'Estado' },
    { clave: 'variantes', titulo: 'Variantes', valor: (p) => p.cantidadVariantes },
    { clave: 'stock', titulo: 'Stock', valor: (p) => p.stockDisponible },
    { clave: 'precio', titulo: 'Desde', valor: (p) => `S/ ${p.precioDesde.toFixed(2)}` },
  ];
  protected readonly destino = (p: catalogApi.ProductoAdminResumen) => [
    '/catalogo/productos',
    p.id,
  ];

  protected readonly accionesAdmin = (
    p: catalogApi.ProductoAdminResumen,
  ): AccionFila<catalogApi.ProductoAdminResumen>[] => [
    {
      etiqueta: 'Editar',
      alEjecutar: () => void this.router.navigate(['/catalogo/productos', p.id]),
    },
    p.estado === 'ACTIVO'
      ? { etiqueta: 'Desactivar', alEjecutar: () => this.cambiarEstado(p, 'desactivar') }
      : { etiqueta: 'Publicar', alEjecutar: () => this.cambiarEstado(p, 'publicar') },
    { etiqueta: 'Eliminar', alEjecutar: () => this.eliminar(p) },
  ];

  protected variante(estado: string): string {
    return VARIANTE_ESTADO[estado] ?? 'neutra';
  }
  protected etiqueta(estado: string): string {
    return ESTADOS.find((e) => e.valor === estado)?.etiqueta ?? estado;
  }

  private cambiarEstado(
    p: catalogApi.ProductoAdminResumen,
    accion: 'publicar' | 'desactivar',
  ): void {
    const llamada =
      accion === 'publicar' ? this.api.publicarProducto(p.id) : this.api.desactivarProducto(p.id);
    llamada.subscribe(() => {
      this.toasts.mostrar(
        'exito',
        accion === 'publicar' ? 'Producto publicado.' : 'Producto desactivado.',
      );
      this.lista.reload();
    });
  }

  private eliminar(p: catalogApi.ProductoAdminResumen): void {
    this.dialogo
      .confirmar({
        titulo: `¿Eliminar ${p.nombre}?`,
        mensaje:
          'Solo se elimina un borrador sin movimientos ni reservas; si no, usa "Desactivar".',
        accion: 'Eliminar',
        peligro: true,
      })
      .subscribe((si) => {
        if (!si) return;
        this.api.eliminarProducto(p.id).subscribe(() => {
          this.toasts.mostrar('exito', 'Producto eliminado.');
          this.lista.reload();
        });
      });
  }
}
