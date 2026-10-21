import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs';
import { FechaPipe, PrecioPipe, paymentApi, type ConsultaPagina, type Pagina } from 'shared';
import { PagosAdminService } from 'shared/api/payment-service';
import { EstadoError } from '../../ui/estados/estado-error';
import { BarraFiltros, type SelectFiltro } from '../../ui/filtros/barra-filtros';
import { InsigniaEstado } from '../../ui/insignia/insignia-estado';
import { FsCelda } from '../../ui/tabla/fs-celda';
import { FsTabla, type ColumnaTabla } from '../../ui/tabla/fs-tabla';
import { proveedorYMedio } from './etiquetas';

type Fila = paymentApi.PagoResumen;

const fecha = new FechaPipe();
const precio = new PrecioPipe();

const ESTADOS: SelectFiltro = {
  clave: 'estado',
  etiqueta: 'Estado',
  opciones: [
    { valor: 'PENDIENTE', etiqueta: 'Pendiente de pago' },
    { valor: 'PAGADA', etiqueta: 'Pagado' },
    { valor: 'ANULADA', etiqueta: 'Anulado' },
    { valor: 'REEMBOLSADA_PARCIAL', etiqueta: 'Reembolso parcial' },
    { valor: 'REEMBOLSADA', etiqueta: 'Reembolsado' },
  ],
};
const PROVEEDORES: SelectFiltro = {
  clave: 'proveedor',
  etiqueta: 'Proveedor',
  opciones: [
    { valor: 'MERCADO_PAGO', etiqueta: 'Mercado Pago' },
    { valor: 'SIMULADO', etiqueta: 'Pago de prueba' },
    { valor: 'CULQI', etiqueta: 'Culqi' },
  ],
};

/** Pagos del backoffice (P-5-03, solo ADMIN): intenciones paginadas con filtros que viven en la URL. */
@Component({
  selector: 'fs-pagos-page',
  imports: [FsTabla, FsCelda, BarraFiltros, InsigniaEstado, EstadoError],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './pagos.page.html',
  styles: `
    .pagos-fechas {
      display: flex;
      gap: var(--espacio-3);
      flex-wrap: wrap;
      margin-block: var(--espacio-3);
    }
    .pagos-fechas label {
      display: grid;
      gap: var(--espacio-1);
      font-size: 13px;
    }
  `,
})
export class PagosPage {
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(PagosAdminService);

  protected readonly selects: SelectFiltro[] = [ESTADOS, PROVEEDORES];
  protected readonly columnas: ColumnaTabla<Fila>[] = [
    { clave: 'codigoPedido', titulo: 'Pedido', ordenable: true, valor: (f) => f.codigoPedido },
    { clave: 'cliente', titulo: 'Cliente', valor: (f) => f.clienteNombre },
    { clave: 'monto', titulo: 'Monto', ordenable: true, valor: (f) => precio.transform(f.monto) },
    { clave: 'cobro', titulo: 'Proveedor y medio', valor: (f) => proveedorYMedio(f.proveedor, f.medio) },
    { clave: 'estado', titulo: 'Estado', ordenable: true },
    { clave: 'creadoEn', titulo: 'Fecha', ordenable: true, valor: (f) => fecha.transform(f.creadoEn, 'fechaHora') },
  ];

  private readonly parametros = toSignal(this.ruta.queryParamMap, {
    initialValue: this.ruta.snapshot.queryParamMap,
  });
  protected readonly desde = computed(() => this.parametros().get('desde') ?? '');
  protected readonly hasta = computed(() => this.parametros().get('hasta') ?? '');
  protected readonly consulta = signal<ConsultaPagina>({ page: 0, size: 20 });

  private readonly peticion = computed(() => {
    const p = this.parametros();
    return {
      estado: p.get('estado'),
      proveedor: p.get('proveedor'),
      codigo: p.get('q'),
      desde: p.get('desde'),
      hasta: p.get('hasta'),
      ...this.consulta(),
    };
  });

  protected readonly datos = rxResource({
    params: () => this.peticion(),
    stream: ({ params: q }) =>
      this.api
        .listarPagos(
          (q.estado as paymentApi.EstadoIntencion | null) ?? undefined,
          (q.proveedor as paymentApi.Proveedor | null) ?? undefined,
          q.codigo ?? undefined,
          q.desde ?? undefined,
          q.hasta ?? undefined,
          q.page,
          q.size,
          q.sort?.length ? q.sort : undefined,
        )
        .pipe(map((r) => r.data as Pagina<Fila>)),
  });

  protected readonly enlaceDetalle = (fila: Fila) => ['/pagos', fila.id];

  /** Al cambiar un filtro se vuelve a la primera página. */
  protected alCambiarFiltros(): void {
    if (this.consulta().page !== 0) this.consulta.update((c) => ({ ...c, page: 0 }));
  }

  protected cambiarFecha(clave: 'desde' | 'hasta', valor: string): void {
    this.alCambiarFiltros();
    void this.router.navigate([], {
      relativeTo: this.ruta,
      queryParams: { [clave]: valor || null },
      queryParamsHandling: 'merge',
    });
  }
}
