import { HttpContext } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CarritoContador, SIN_TOAST, ToastService, catalogApi } from 'shared';
import { TiendaService } from 'shared/api/catalog-service';
import { CarritoService } from 'shared/api/order-service';
import { map } from 'rxjs';
import { FsGaleria } from '../../ui/galeria/galeria';
import { FsPrecio } from '../../ui/precio/precio';
import { EstadoCargando, EstadoError, EstadoVacio } from '../../ui/estados/estados';
import { GuiaTallasModal } from './guia-tallas-modal';
import { IndicadorStock } from './indicador-stock';
import { SelectorCantidad } from './selector-cantidad';
import { SelectorColor } from './selector-color';
import { SelectorTalla } from './selector-talla';
import { TramosDescuento } from './tramos-descuento';

/** Tope de la cantidad cuando la API solo dice "Disponible" (más de 5 unidades); el servidor valida el stock real. */
const MAXIMO_SIN_DATO = 99;

/** P-2-02: detalle del producto con color, talla, disponibilidad, tramos, guía de tallas y "Agregar al carrito". */
@Component({
  selector: 'fs-detalle-page',
  imports: [
    RouterLink,
    FsGaleria,
    FsPrecio,
    EstadoCargando,
    EstadoError,
    EstadoVacio,
    SelectorColor,
    SelectorTalla,
    SelectorCantidad,
    IndicadorStock,
    TramosDescuento,
    GuiaTallasModal,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .detalle {
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
      gap: 40px;
      padding-block: 32px;
    }
    .info {
      display: grid;
      gap: 20px;
      align-content: start;
    }
    .fila {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .enlace {
      border: 0;
      background: transparent;
      text-decoration: underline;
      cursor: pointer;
      font: inherit;
      color: inherit;
    }
    .acciones {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
      align-items: center;
    }
    @media (max-width: 800px) {
      .detalle {
        grid-template-columns: minmax(0, 1fr);
      }
    }
  `,
  template: `
    <section class="fs-pagina detalle">
      @if (producto.error()) {
        @if (noEncontrado()) {
          <fs-estado-vacio
            titulo="No encontramos este producto"
            texto="Puede que ya no esté disponible."
            accion="Ver todos los productos"
            (accionar)="irAlListado()"
          />
        } @else {
          <fs-estado-error (reintentar)="producto.reload()" />
        }
      } @else if (producto.value(); as p) {
        <fs-galeria [imagenes]="galeria()" />
        <div class="info">
          <nav aria-label="Migas"><a routerLink="/productos">Productos</a> / {{ p.nombre }}</nav>
          <h1>{{ p.nombre }}</h1>
          <fs-precio [monto]="precio()" />
          @if (agotadoTodo()) {
            <p role="status"><strong>Agotado</strong></p>
          }
          <div>
            <strong>Color: {{ colorElegido()?.nombre }}</strong>
            <fs-selector-color [colores]="p.colores" [(valor)]="colorId" />
          </div>
          <div>
            <div class="fila">
              <strong>Talla</strong>
              @if (p.tieneGuiaTallas && p.categorias.length) {
                <button type="button" class="enlace" (click)="guiaAbierta.set(true)">
                  Guía de tallas
                </button>
              }
            </div>
            <fs-selector-talla
              [tallas]="p.tallas"
              [agotadas]="tallasAgotadas()"
              [(valor)]="tallaId"
            />
          </div>
          <fs-indicador-stock [variante]="variante()" />
          <fs-tramos-descuento [tramos]="p.tramos" />
          <div class="acciones">
            <fs-selector-cantidad
              [maximo]="maximo()"
              [deshabilitado]="!puedeAgregar()"
              [(valor)]="cantidad"
            />
            <button
              type="button"
              class="fs-btn fs-btn--primario"
              [disabled]="!puedeAgregar() || agregando()"
              (click)="agregar()"
            >
              Agregar al carrito
            </button>
          </div>
          @if (!tallaId() && !agotadoTodo()) {
            <small>Elige una talla para agregar el producto.</small>
          }
          <p>{{ p.descripcion }}</p>
        </div>
        @if (p.categorias.length) {
          <fs-guia-tallas-modal [categoria]="p.categorias[0].slug" [(abierto)]="guiaAbierta" />
        }
      } @else {
        <fs-estado-cargando [filas]="6" />
      }
    </section>
  `,
})
export class DetallePage {
  private readonly ruta = inject(ActivatedRoute);
  private readonly tienda = inject(TiendaService);
  private readonly carrito = inject(CarritoService);
  private readonly contador = inject(CarritoContador);
  private readonly toasts = inject(ToastService);
  private readonly router = inject(Router);

  private readonly slug = toSignal(this.ruta.paramMap.pipe(map((m) => m.get('slug') ?? '')), {
    initialValue: '',
  });

  protected readonly producto = rxResource({
    params: () => this.slug() || undefined,
    stream: ({ params }) =>
      this.tienda
        .obtenerProducto(params, 'body', false, { context: new HttpContext().set(SIN_TOAST, true) })
        .pipe(map((r) => r.data as catalogApi.ProductoDetalle)),
  });
  protected readonly noEncontrado = computed(
    () => (this.producto.error() as { status?: number } | undefined)?.status === 404,
  );

  /** Primer color con stock; si ninguno tiene, el primero. */
  protected readonly colorId = linkedSignal<catalogApi.ProductoDetalle | undefined, number | null>({
    source: () => this.producto.value(),
    computation: (p) =>
      p ? ((p.colores.find((c) => c.conStock) ?? p.colores[0])?.id ?? null) : null,
  });
  protected readonly tallaId = linkedSignal<number | null, number | null>({
    source: this.colorId,
    computation: () => null,
  });
  protected readonly cantidad = linkedSignal<number | null, number>({
    source: this.tallaId,
    computation: () => 1,
  });
  protected readonly guiaAbierta = signal(false);
  protected readonly agregando = signal(false);

  protected readonly colorElegido = computed(() =>
    this.producto.value()?.colores.find((c) => c.id === this.colorId()),
  );
  protected readonly agotadoTodo = computed(
    () => !(this.producto.value()?.colores ?? []).some((c) => c.conStock),
  );
  protected readonly galeria = computed(() => {
    const p = this.producto.value();
    const color = this.colorId();
    if (!p) return [];
    return p.imagenes
      .filter((i) => i.colorId === color)
      .sort((a, b) => Number(b.principal) - Number(a.principal) || a.orden - b.orden)
      .map((i) => ({ url: i.url, alt: `${p.nombre} ${this.colorElegido()?.nombre ?? ''}`.trim() }));
  });

  /** Ids de tallas sin stock en el color elegido (sin variante activa o agotada). */
  protected readonly tallasAgotadas = computed(() => {
    const p = this.producto.value();
    if (!p) return [];
    return p.tallas
      .filter((t) => {
        const v = p.variantes.find((x) => x.colorId === this.colorId() && x.tallaId === t.id);
        return !v || v.disponibilidad === 'AGOTADO';
      })
      .map((t) => t.id);
  });
  protected readonly variante = computed(
    () =>
      this.producto
        .value()
        ?.variantes.find((v) => v.colorId === this.colorId() && v.tallaId === this.tallaId()) ??
      null,
  );
  protected readonly precio = computed(
    () => this.variante()?.precio ?? this.producto.value()?.precioDesde ?? 0,
  );
  protected readonly maximo = computed(() => {
    const v = this.variante();
    return v?.disponibilidad === 'ULTIMAS' ? (v.ultimasUnidades ?? 1) : MAXIMO_SIN_DATO;
  });
  protected readonly puedeAgregar = computed(() => {
    const v = this.variante();
    return !!v && v.disponibilidad !== 'AGOTADO';
  });

  protected agregar(): void {
    const v = this.variante();
    if (!v || this.agregando()) return;
    this.agregando.set(true);
    this.carrito.agregarAlCarrito({ varianteId: v.id, cantidad: this.cantidad() }).subscribe({
      next: (r) => {
        if (r.data) this.contador.establecer(r.data.cantidadTotal);
        this.toasts.mostrar('exito', 'Agregamos el producto a tu carrito.');
        this.agregando.set(false);
      },
      error: () => this.agregando.set(false),
    });
  }

  protected irAlListado(): void {
    void this.router.navigateByUrl('/productos');
  }
}
