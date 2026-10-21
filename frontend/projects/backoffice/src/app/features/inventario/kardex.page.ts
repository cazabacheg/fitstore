import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FechaPipe, ToastService, catalogApi, type ConsultaPagina } from 'shared';
import { map } from 'rxjs';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { EstadoError } from '../../ui/estados/estado-error';
import { FsCelda } from '../../ui/tabla/fs-celda';
import { FsTabla, type ColumnaTabla } from '../../ui/tabla/fs-tabla';
import { DialogoAjuste, type DatosDialogoAjuste } from './dialogo-ajuste';

const TIPOS: Record<catalogApi.TipoMovimiento, string> = {
  ENTRADA: 'Entrada',
  AJUSTE: 'Ajuste',
  VENTA: 'Venta',
  REINGRESO: 'Reingreso',
};

/** P-2-08: kardex de un SKU. Las filas no tienen detalle (no son clicables); el pedido, si existe, es un enlace. */
@Component({
  selector: 'fs-kardex-page',
  imports: [RouterLink, EncabezadoPagina, FsTabla, FsCelda, EstadoError],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .resumen {
      display: flex;
      gap: var(--espacio-5);
      flex-wrap: wrap;
      margin: 0;
    }
    .resumen div {
      display: grid;
      gap: 2px;
    }
    dt {
      color: var(--fs-color-texto-suave);
      font-size: 13px;
    }
    dd {
      margin: 0;
      font-weight: 600;
    }
    :host {
      display: grid;
      gap: var(--espacio-4);
    }
  `,
  template: `
    <fs-encabezado-pagina [titulo]="'Kardex ' + (stock()?.sku ?? '')">
      <div accion style="display: flex; gap: 8px">
        <a class="fs-btn fs-btn--contorno" routerLink="/inventario">‹ Inventario</a>
        @if (stock()) {
          <button type="button" class="fs-btn fs-btn--primario" (click)="ajustar()">
            Registrar ajuste
          </button>
        }
      </div>
    </fs-encabezado-pagina>
    @if (lista.error()) {
      <fs-estado-error (reintentar)="lista.reload()" />
    } @else {
      @if (stock(); as s) {
        <dl class="resumen">
          <div>
            <dt>Producto</dt>
            <dd>{{ s.producto }} · {{ s.talla }} · {{ s.color }}</dd>
          </div>
          <div>
            <dt>Físico</dt>
            <dd>{{ s.fisico }}</dd>
          </div>
          <div>
            <dt>Reservado</dt>
            <dd>{{ s.reservado }}</dd>
          </div>
          <div>
            <dt>Disponible</dt>
            <dd>{{ s.disponible }}</dd>
          </div>
          <div>
            <dt>Mínimo</dt>
            <dd>{{ s.minimo }}</dd>
          </div>
        </dl>
      }
      <fs-tabla
        [columnas]="columnas"
        [datos]="movimientos()"
        [cargando]="lista.isLoading()"
        columnaId="fecha"
        tituloVacio="Este SKU aún no tiene movimientos"
        textoVacio=""
        (consulta)="consulta.set($event)"
      >
        <ng-template fsCelda="detalle" let-m>
          @if (m.referencia && (m.tipo === 'VENTA' || m.tipo === 'REINGRESO')) {
            <a [routerLink]="['/pedidos']" [queryParams]="{ codigo: m.referencia }">{{
              m.referencia
            }}</a>
          } @else {
            {{ m.motivo || '—' }}
          }
        </ng-template>
      </fs-tabla>
    }
  `,
})
export class KardexPage {
  private readonly ruta = inject(ActivatedRoute);
  private readonly api = inject(catalogApi.InventarioService);
  private readonly dialogo = inject(MatDialog);
  private readonly toasts = inject(ToastService);
  private readonly fecha = new FechaPipe();

  protected readonly consulta = signal<ConsultaPagina>({ page: 0, size: 20 });
  private readonly varianteId = toSignal(
    this.ruta.paramMap.pipe(map((m) => Number(m.get('varianteId')))),
    { initialValue: Number(this.ruta.snapshot.paramMap.get('varianteId')) },
  );

  protected readonly lista = rxResource({
    params: () => ({ id: this.varianteId(), c: this.consulta() }),
    stream: ({ params: { id, c } }) =>
      this.api.obtenerKardex(id, c.page, c.size).pipe(map((r) => r.data as catalogApi.Kardex)),
  });
  protected readonly stock = computed(() =>
    this.lista.hasValue() ? this.lista.value().stock : null,
  );
  protected readonly movimientos = computed(() =>
    this.lista.hasValue() ? this.lista.value().movimientos : null,
  );

  protected readonly columnas: ColumnaTabla<catalogApi.Movimiento>[] = [
    {
      clave: 'fecha',
      titulo: 'Fecha',
      valor: (m) => this.fecha.transform(m.creadoEn, 'fechaHora'),
    },
    { clave: 'tipo', titulo: 'Tipo', valor: (m) => TIPOS[m.tipo] },
    {
      clave: 'cantidad',
      titulo: 'Cantidad',
      valor: (m) => (m.cantidad > 0 ? `+${m.cantidad}` : `−${Math.abs(m.cantidad)}`),
    },
    { clave: 'saldo', titulo: 'Saldo', valor: (m) => m.saldoFisico },
    { clave: 'detalle', titulo: 'Motivo o pedido' },
    { clave: 'usuario', titulo: 'Usuario', valor: (m) => m.usuarioNombre || 'sistema' },
  ];

  protected ajustar(): void {
    const stock = this.stock();
    if (!stock) return;
    const datos: DatosDialogoAjuste = { stock };
    this.dialogo
      .open<DialogoAjuste, DatosDialogoAjuste, catalogApi.AjusteRequest | null>(DialogoAjuste, {
        data: datos,
        width: '460px',
        maxWidth: '100%',
      })
      .afterClosed()
      .subscribe((cuerpo) => {
        if (!cuerpo) return;
        this.api.registrarAjuste(cuerpo).subscribe(() => {
          this.toasts.mostrar('exito', 'Ajuste registrado.');
          this.lista.reload();
        });
      });
  }
}
