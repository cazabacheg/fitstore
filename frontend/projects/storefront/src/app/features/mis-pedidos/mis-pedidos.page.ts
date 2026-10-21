import { HttpContext } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FechaPipe, PrecioPipe, SIN_TOAST, orderApi } from 'shared';
import { MisPedidosService } from 'shared/api/order-service';
import { EstadoCargando, EstadoError, EstadoVacio } from '../../ui/estados/estados';
import { FsPaginador } from '../../ui/paginador/paginador';
import { ESTILOS_PANTALLA } from '../catalogo/estilos-pantalla';
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
  styles: [
    ESTILOS_PANTALLA,
    `
      .pedidos {
        padding-block: 40px 80px;
        display: grid;
        gap: 24px;
        align-content: start;
      }
      .pedidos__lista {
        list-style: none;
        margin: 0;
        padding: 0;
        display: grid;
        gap: 16px;
      }
      .fs-tarjeta-pedido {
        position: relative;
        display: grid;
        grid-template-columns: 56px minmax(0, 1fr) auto;
        gap: 16px;
        align-items: center;
      }
      .fs-tarjeta-pedido:hover {
        box-shadow: var(--sombra-1);
      }
      .fs-tarjeta-pedido .fs-miniatura {
        width: 56px;
      }
      .pedidos__info {
        display: grid;
        gap: 8px;
        min-width: 0;
      }
      .pedidos__codigo {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
        align-items: center;
      }
      .pedidos__codigo a {
        color: inherit;
        text-decoration: none;
        font-size: 14px;
      }
      /* Toda la tarjeta es el enlace al detalle */
      .pedidos__codigo a::after {
        content: '';
        position: absolute;
        inset: 0;
      }
      .pedidos__total {
        display: grid;
        justify-items: end;
        gap: 8px;
        font-variant-numeric: tabular-nums;
      }
      .pedidos__total .fs-icono {
        color: var(--fs-color-texto-suave);
      }
      fs-paginador {
        display: flex;
        justify-content: center;
      }
      @media (max-width: 720px) {
        .pedidos {
          padding-block: 24px 48px;
        }
        .fs-tarjeta-pedido {
          grid-template-columns: 48px minmax(0, 1fr) auto;
          gap: 12px;
        }
        .fs-tarjeta-pedido .fs-miniatura {
          width: 48px;
        }
      }
    `,
  ],
  template: `
    <section class="fs-pagina pedidos">
      <div style="display: grid; gap: 4px">
        <h1 class="fs-titulo-pagina">Mis pedidos</h1>
        <span class="fs-suave">Aquí ves el estado de cada compra.</span>
      </div>
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
            <ul class="pedidos__lista">
              @for (p of pedidos(); track p.codigo) {
                <li class="fs-tarjeta-pedido fs-tarjeta">
                  <span class="fs-miniatura">
                    @if (p.miniaturaUrl) {
                      <img [src]="p.miniaturaUrl" alt="" width="56" height="70" />
                    } @else {
                      <span class="fs-icono" aria-hidden="true">checkroom</span>
                    }
                  </span>
                  <div class="pedidos__info">
                    <div class="pedidos__codigo">
                      <a class="fs-codigo" [routerLink]="['/cuenta/pedidos', p.codigo]"><strong>{{ p.codigo }}</strong></a>
                      <fs-etiqueta-estado [estado]="p.estado" />
                    </div>
                    <span class="fs-suave">{{ p.creadoEn | fecha: 'corta' }} · {{ p.cantidadProductos }} prendas</span>
                  </div>
                  <div class="pedidos__total">
                    <strong>{{ p.total | precio }}</strong>
                    <span class="fs-icono" aria-hidden="true">chevron_right</span>
                  </div>
                </li>
              }
            </ul>
            <fs-paginador [pagina]="pagina()" [totalPaginas]="totalPaginas()" (cambio)="cargar($event)" />
          }
        }
      }
    </section>
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
