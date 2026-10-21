import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { FechaPipe, PrecioPipe, paymentApi } from 'shared';
import { PagosAdminService } from 'shared/api/payment-service';
import { EstadoCargando } from '../../ui/estados/estado-cargando';
import { EstadoError } from '../../ui/estados/estado-error';
import { InsigniaEstado } from '../../ui/insignia/insignia-estado';
import { nombreMedio, nombreMotivo, nombreProveedor, proveedorYMedio } from './etiquetas';

/** Detalle de un pago (P-5-04, solo ADMIN): intentos, reembolsos, montos y boleta. Nunca datos de tarjeta. */
@Component({
  selector: 'fs-detalle-pago-page',
  imports: [RouterLink, PrecioPipe, FechaPipe, InsigniaEstado, EstadoCargando, EstadoError],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './detalle-pago.page.html',
  styles: `
    .pago-tabla {
      width: 100%;
      border-collapse: collapse;
    }
    .pago-tabla th,
    .pago-tabla td {
      text-align: left;
      padding: var(--espacio-2) var(--espacio-3);
      border-bottom: 1px solid var(--fs-color-borde, #ddd);
      vertical-align: top;
    }
    .pago-montos {
      display: flex;
      flex-wrap: wrap;
      gap: var(--espacio-5);
    }
    .pago-montos > div {
      display: grid;
      gap: var(--espacio-1);
    }
  `,
})
export class DetallePagoPage {
  private readonly id = inject(ActivatedRoute).snapshot.paramMap.get('id') ?? '';
  private readonly api = inject(PagosAdminService);

  protected readonly descargando = signal(false);

  protected readonly pago = rxResource({
    params: () => this.id,
    stream: ({ params: id }) => this.api.obtenerPago(id).pipe(map((r) => r.data as paymentApi.PagoDetalle)),
  });

  protected readonly proveedorYMedio = proveedorYMedio;
  protected readonly nombreProveedor = nombreProveedor;
  protected readonly nombreMedio = nombreMedio;
  protected readonly nombreMotivo = nombreMotivo;

  protected descargarBoleta(numero: string): void {
    this.descargando.set(true);
    this.api.descargarBoletaAdmin(this.id, 'body', false, { httpHeaderAccept: 'application/pdf' }).subscribe({
      next: (pdf) => {
        const url = URL.createObjectURL(pdf);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = `Boleta-${numero}.pdf`;
        enlace.click();
        URL.revokeObjectURL(url);
        this.descargando.set(false);
      },
      error: () => this.descargando.set(false),
    });
  }
}
