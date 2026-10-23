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
import { catchError, map, of } from 'rxjs';
import { capitalizar } from '../../ui/capitalizar';
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

/**
 * Unidades que se sabe que hay en la variante para los tramos: AGOTADO = 0, `unidadesComprables` (stock acotado al
 * tramo más alto, CA-2-73), "Últimas N" = N; sin ese dato, el tope genérico.
 */
const stockTramos = (v: catalogApi.VariantePublica): number =>
  v.disponibilidad === 'AGOTADO'
    ? 0
    : (v.unidadesComprables ??
      (v.disponibilidad === 'ULTIMAS' ? (v.ultimasUnidades ?? 1) : MAXIMO_SIN_DATO));

/** Tope del selector de cantidad: el stock exacto si se conoce (menor que el tramo más alto o "Últimas N"); si no, el genérico. */
const stockSelector = (v: catalogApi.VariantePublica, tramoMasAlto: number): number => {
  if (v.disponibilidad === 'AGOTADO') return 0;
  if (v.disponibilidad === 'ULTIMAS') return v.ultimasUnidades ?? 1;
  return v.unidadesComprables != null && v.unidadesComprables < tramoMasAlto
    ? v.unidadesComprables
    : MAXIMO_SIN_DATO;
};

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
      grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr);
      gap: 56px;
      padding-block: 24px 80px;
      align-items: start;
    }
    .detalle > :is(fs-estado-vacio, fs-estado-error, fs-estado-cargando) {
      grid-column: 1 / -1;
    }
    :host ::ng-deep .fs-galeria {
      max-width: none;
    }
    .info {
      display: grid;
      gap: 24px;
      align-content: start;
    }
    .migas {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      font-size: 13px;
      line-height: 20px;
      color: var(--fs-color-texto-suave);
    }
    .migas a {
      color: var(--fs-color-texto-suave);
      text-decoration: none;
    }
    h1 {
      margin: 0;
      font-size: 28px;
      line-height: 34px;
      font-weight: 700;
      letter-spacing: -0.01em;
    }
    .descripcion {
      margin: 0;
      color: var(--fs-color-texto-suave);
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
        gap: 24px;
        padding-block: 16px 48px;
      }
      h1 {
        font-size: 24px;
        line-height: 30px;
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
          <nav class="migas" aria-label="Migas">
            <a routerLink="/productos">Productos</a>
            @for (m of migasCategoria(); track m.slug) {
              <span aria-hidden="true">/</span>
              <a [routerLink]="['/categoria', m.slug]">{{ m.nombre }}</a>
            }
            <span aria-hidden="true">/</span><span>{{ p.nombre }}</span>
          </nav>
          <h1>{{ p.nombre }}</h1>
          <fs-precio [monto]="precio()" />
          @if (agotadoTodo()) {
            <p role="status"><strong>Agotado</strong></p>
          }
          <div>
            <strong>Color: {{ nombreColor(colorElegido()?.nombre) }}</strong>
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
          <fs-tramos-descuento
            [tramos]="p.tramos"
            [maximo]="maximoTramos()"
            [(cantidad)]="cantidad"
          />
          <div class="acciones">
            <fs-selector-cantidad
              [maximo]="maximo()"
              [deshabilitado]="!puedeAgregar()"
              [(valor)]="cantidad"
            />
            <button
              type="button"
              class="fs-btn fs-btn--primario fs-btn--lg"
              [disabled]="!puedeAgregar() || agregando()"
              (click)="agregar()"
            >
              Agregar al carrito
            </button>
          </div>
          @if (!tallaId() && !agotadoTodo()) {
            <small>Elige una talla para agregar el producto.</small>
          }
          <p class="descripcion">{{ p.descripcion }}</p>
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
  /** Al cambiar de color la talla se conserva si en el nuevo color sigue disponible; si no, se deselecciona (CA-2-66). */
  protected readonly tallaId = linkedSignal<
    { p: catalogApi.ProductoDetalle | undefined; c: number | null },
    number | null
  >({
    source: () => ({ p: this.producto.value(), c: this.colorId() }),
    computation: (fuente, previo) => {
      const talla = previo?.value ?? null;
      if (!talla || previo?.source.p !== fuente.p) return null;
      const v = fuente.p?.variantes.find((x) => x.colorId === fuente.c && x.tallaId === talla);
      return v && v.disponibilidad !== 'AGOTADO' ? talla : null;
    },
  });
  /** La cantidad se conserva al cambiar de talla o color, recortada al stock conocido de la nueva variante. */
  protected readonly cantidad = linkedSignal<number, number>({
    source: () => this.maximo(),
    computation: (tope, previo) => Math.min(previo?.value ?? 1, Math.max(tope, 1)),
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
  /**
   * Tope del selector de cantidad: el stock de la variante elegida cuando se conoce (AGOTADO = 0, ULTIMAS = N,
   * unidadesComprables si es menor que el tramo más alto; si no, el tope genérico). Sin talla elegida, el mayor de las
   * variantes del color (CA-2-69, CA-2-73).
   */
  protected readonly maximo = computed(() => {
    const tope = Math.max(
      0,
      ...(this.producto.value()?.tramos ?? []).map((t) => t.unidadesMinimas),
    );
    return this.mayorStock((v) => stockSelector(v, tope));
  });
  /** Unidades para decidir qué tramos se ven grises (CA-2-69, CA-2-73). */
  protected readonly maximoTramos = computed(() => this.mayorStock(stockTramos));

  /** Valor de la variante elegida o, sin talla, el mayor entre las variantes del color. */
  private mayorStock(valor: (v: catalogApi.VariantePublica) => number): number {
    const v = this.variante();
    if (v) return valor(v);
    const delColor = (this.producto.value()?.variantes ?? []).filter(
      (x) => x.colorId === this.colorId(),
    );
    return delColor.length ? Math.max(...delColor.map(valor)) : MAXIMO_SIN_DATO;
  }
  protected readonly nombreColor = capitalizar;

  /** Categorías de las migas: la padre y la hija (o solo la raíz), sacadas del árbol; sin árbol, la del producto. */
  private readonly arbol = rxResource({
    stream: () =>
      this.tienda
        .obtenerArbolCategorias('body', false, { context: new HttpContext().set(SIN_TOAST, true) })
        .pipe(
          map((r) => r.data ?? []),
          catchError(() => of([] as catalogApi.CategoriaNodo[])),
        ),
  });
  protected readonly migasCategoria = computed(() => {
    const cats = this.producto.value()?.categorias ?? [];
    const arbol = this.arbol.value() ?? [];
    const hijaConPadre = cats
      .map((c) => ({ c, padre: arbol.find((n) => n.hijas.some((h) => h.slug === c.slug)) }))
      .find((x) => x.padre);
    if (hijaConPadre)
      return [
        { nombre: hijaConPadre.padre!.nombre, slug: hijaConPadre.padre!.slug },
        { nombre: hijaConPadre.c.nombre, slug: hijaConPadre.c.slug },
      ];
    return cats.length ? [{ nombre: cats[0].nombre, slug: cats[0].slug }] : [];
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
