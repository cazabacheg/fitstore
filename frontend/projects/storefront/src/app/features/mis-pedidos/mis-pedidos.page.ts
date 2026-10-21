import { HttpContext } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FechaPipe, PrecioPipe, SIN_TOAST, orderApi } from 'shared';
import { MisPedidosService } from 'shared/api/order-service';
import { EstadoCargando, EstadoError, EstadoVacio } from '../../ui/estados/estados';
import { FsPaginador } from '../../ui/paginador/paginador';
import { EtiquetaEstado } from './etiqueta-estado';

/** Mis pedidos (P-4-03): del más reciente al más antiguo, 10 por página (CA-4-33). */
@Component({
  selector: 'fs-mis-pedidos-page',
  imports: [
    RouterLink,
    FechaPipe,
    PrecioPipe,
    EtiquetaEstado,
    FsPaginador,
    EstadoCargando,
    EstadoError,
    EstadoVacio,
  ],
  template: `
    <h1>Mis pedidos</h1>
    @switch (estado()) {
      @case ('cargando') {
        <fs-estado-cargando />
      }
      @case ('error') {
        <fs-estado-error titulo="No pudimos cargar tus pedidos" (reintentar)="cargar(pagina())" />
      }
      @default {
        @if (pedidos().length === 0) {
          <fs-estado-vacio
            titulo="Aún no tienes pedidos"
            texto="Cuando compres, aquí podrás seguir cada pedido."
            accion="Ir a la tienda"
            (accionar)="irALaTienda()"
          />
        } @else {
          <ul style="list-style: none; padding: 0; display: grid; gap: var(--espacio-4)">
            @for (p of pedidos(); track p.codigo) {
              <li class="fs-tarjeta-pedido" style="display: flex; gap: var(--espacio-4); align-items: center">
                @if (p.miniaturaUrl) {
                  <img [src]="p.miniaturaUrl" alt="" width="64" height="64" />
                }
                <div style="flex: 1">
                  <a [routerLink]="['/cuenta/pedidos', p.codigo]"><strong>{{ p.codigo }}</strong></a>
                  <div class="fs-suave">{{ p.creadoEn | fecha: 'corta' }} · {{ p.cantidadProductos }} prendas</div>
                </div>
                <fs-etiqueta-estado [estado]="p.estado" />
                <strong>{{ p.total | precio }}</strong>
              </li>
            }
          </ul>
          <fs-paginador [pagina]="pagina()" [totalPaginas]="totalPaginas()" (cambio)="cargar($event)" />
        }
      }
    }
  `,
})
export class MisPedidosPage {
  private readonly api = inject(MisPedidosService);
  private readonly router = inject(Router);

  protected readonly estado = signal<'cargando' | 'listo' | 'error'>('cargando');
  protected readonly pedidos = signal<orderApi.PedidoResumen[]>([]);
  protected readonly pagina = signal(0);
  protected readonly totalPaginas = signal(0);

  constructor() {
    this.cargar(0);
  }

  protected cargar(pagina: number): void {
    this.estado.set('cargando');
    const contexto = { context: new HttpContext().set(SIN_TOAST, true) };
    this.api.listarMisPedidos(pagina, 10, 'body', false, contexto).subscribe({
      next: (r) => {
        this.pedidos.set(r.data?.contenido ?? []);
        this.pagina.set(r.data?.pagina ?? pagina);
        this.totalPaginas.set(r.data?.totalPaginas ?? 0);
        this.estado.set('listo');
      },
      error: () => this.estado.set('error'),
    });
  }

  protected irALaTienda(): void {
    void this.router.navigateByUrl('/productos');
  }
}
