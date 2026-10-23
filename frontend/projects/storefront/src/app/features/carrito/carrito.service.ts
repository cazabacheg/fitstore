import { HttpContext } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import {
  CarritoContador,
  SIN_TOAST,
  SesionStore,
  comoApiError,
  leerCarritoId,
  olvidarCarritoId,
  orderApi,
} from 'shared';
import { CarritoService as CarritoApi } from 'shared/api/order-service';
import { Observable, catchError, map, of, tap } from 'rxjs';

interface TramoConocido {
  unidadesMinimas: number;
  porcentaje: number;
}

/** Redondeo half-up a 2 decimales, como el de catalog. */
const redondear = (monto: number): number => Math.round((monto + Number.EPSILON) * 100) / 100;

/**
 * Estado del carrito de la tienda con señales; cada cambio actualiza el contador de la cabecera (O7, R8).
 *
 * Al cambiar la cantidad se muestra al instante un cálculo local (`previsualizar`) con los tramos que el servidor ya
 * mostró; la respuesta del servidor siempre manda y, si rechaza, se vuelve a lo último que confirmó (CA-4-66).
 */
@Injectable({ providedIn: 'root' })
export class CarritoService {
  private readonly api = inject(CarritoApi);
  private readonly contador = inject(CarritoContador);
  private readonly sesion = inject(SesionStore);

  readonly carrito = signal<orderApi.Carrito | null>(null);
  readonly estado = signal<'cargando' | 'listo' | 'error'>('cargando');
  /** Mensaje de la línea que el servidor rechazó (stock o límite), por variante. */
  readonly errores = signal<Record<number, string>>({});
  /** Avisos del servidor al fusionar (p. ej. una cantidad que se ajustó a 100). */
  readonly avisos = signal<string[]>([]);

  readonly vacio = computed(() => (this.carrito()?.lineas.length ?? 0) === 0);

  /** Último carrito que mandó el servidor: a él se vuelve si rechaza un cambio. */
  private confirmado: orderApi.Carrito | null = null;
  /** Stock disponible por variante: de `unidadesComprables` (CA-4-81), de los rechazos 409 y de las líneas con STOCK_INSUFICIENTE. */
  private readonly stock = signal<Record<number, number>>({});
  /** Tramos de descuento que el servidor ya mostró, por producto. El servidor sigue siendo quien decide. */
  private readonly tramos = new Map<number, TramoConocido[]>();
  /** Cantidad que la persona pidió por última vez en cada variante, para ignorar respuestas viejas. */
  private readonly deseada = new Map<number, number>();

  private readonly sinToast = () => ({ context: new HttpContext().set(SIN_TOAST, true) });

  constructor() {
    // Al cerrar sesión el carrito de la cuenta ya no es el de esta persona: contador en 0 y carrito anónimo.
    let habiaSesion = untracked(() => this.sesion.autenticado());
    effect(() => {
      const hay = this.sesion.autenticado();
      if (habiaSesion && !hay) untracked(() => this.alCerrarSesion());
      habiaSesion = hay;
    });
  }

  /**
   * CA-4-61: carga el carrito al arrancar la app (anónimo o de la cuenta) para que el contador de la bolsa no quede
   * en 0 hasta entrar al carrito. Si quedó un carritoId de antes de iniciar sesión, lo fusiona.
   */
  iniciarApp(): void {
    if (this.sesion.autenticado()) this.alIniciarSesion().subscribe();
    else this.cargar();
  }

  /**
   * CA-4-10 y CA-4-12: tras iniciar sesión (o registrarse) fusiona el carrito anónimo con el de la cuenta y borra el
   * carritoId. Termina siempre, también si la fusión falla, para no bloquear el ingreso; quien la llama espera a que
   * termine antes de navegar (así el checkout ya ve los productos). Sin carrito anónimo solo carga el de la cuenta.
   */
  alIniciarSesion(): Observable<void> {
    if (!leerCarritoId()) {
      this.cargar();
      return of(undefined);
    }
    return this.api.fusionarCarrito(undefined, 'body', false, this.sinToast()).pipe(
      tap((r) => {
        this.aplicar(r.data);
        olvidarCarritoId();
      }),
      map(() => undefined),
      catchError(() => {
        this.cargar();
        return of(undefined);
      }),
    );
  }

  private alCerrarSesion(): void {
    this.confirmado = null;
    this.carrito.set(null);
    this.stock.set({});
    this.avisos.set([]);
    this.deseada.clear();
    this.contador.establecer(0);
    this.cargar();
  }

  /** Pide el carrito al servidor y actualiza el contador. El checkout lo llama tras confirmar un pedido (queda vacío). */
  cargar(): void {
    this.estado.set('cargando');
    this.api.obtenerCarrito(undefined, 'body', false, this.sinToast()).subscribe({
      next: (r) => this.aplicar(r.data),
      error: () => this.estado.set('error'),
    });
  }

  /** CA-4-01: suma a la línea si ya existía; el contador de la cabecera muestra el total de unidades. */
  agregar(varianteId: number, cantidad: number): Observable<orderApi.RespuestaCarrito> {
    return this.api
      .agregarAlCarrito({ varianteId, cantidad }, undefined, 'body', false, this.sinToast())
      .pipe(tap((r) => this.aplicar(r.data)));
  }

  /** Stock disponible de la variante si el servidor ya lo informó; `null` si no se sabe. */
  stockMaximo(varianteId: number): number | null {
    return this.stock()[varianteId] ?? null;
  }

  /** CA-4-63: false si el stock conocido no alcanza para llegar al siguiente tramo del producto. */
  tramoAlcanzable(aviso: orderApi.AvisoTramo): boolean {
    const siguiente = aviso.siguienteTramo;
    if (!siguiente) return false;
    const lineas = (this.carrito()?.lineas ?? []).filter((l) => l.productoId === aviso.productoId);
    let capacidad = 0;
    for (const l of lineas) {
      const tope = this.stockMaximo(l.varianteId);
      if (tope === null) return true;
      capacidad += tope;
    }
    return lineas.length === 0 || aviso.unidades + siguiente.faltan <= capacidad;
  }

  /**
   * CA-4-66: muestra al instante la cantidad, el subtotal y el descuento por tramos de lo que la persona pidió, sin
   * esperar al servidor. Solo es una vista previa: `cambiarCantidad` confirma y su respuesta reemplaza este cálculo.
   */
  previsualizar(varianteId: number, cantidad: number): void {
    const actual = this.carrito();
    if (!actual) return;
    this.deseada.set(varianteId, cantidad);
    const calculado = this.recalcular(actual, varianteId, cantidad);
    this.carrito.set(calculado);
    this.contador.establecer(calculado.cantidadTotal);
  }

  /**
   * CA-4-03: recalcula con una nueva cotización del servidor (solo se envía la cantidad, nunca montos). Si lo
   * rechaza, vuelve a lo último confirmado y muestra el motivo (CA-4-04).
   */
  cambiarCantidad(varianteId: number, cantidad: number): void {
    this.deseada.set(varianteId, cantidad);
    this.api
      .cambiarCantidadCarrito(varianteId, { cantidad }, undefined, 'body', false, this.sinToast())
      .subscribe({
        next: (r) => {
          if (this.deseada.get(varianteId) === cantidad) {
            this.deseada.delete(varianteId);
            this.aplicar(r.data);
          } else if (r.data) {
            // La persona ya pidió otra cantidad: se aprende del servidor sin pisar lo que está viendo.
            this.confirmado = r.data;
            this.aprender(r.data);
          }
        },
        error: (e: unknown) => {
          this.deseada.delete(varianteId);
          const api = comoApiError(e);
          const disponible = api?.detalles?.['disponible'];
          if (api?.codigo === 'STOCK_INSUFICIENTE' && typeof disponible === 'number') {
            this.stock.update((s) => ({ ...s, [varianteId]: disponible }));
          }
          const motivo = api?.mensaje ?? 'No pudimos actualizar la cantidad';
          if (this.confirmado) {
            this.carrito.set(this.confirmado);
            this.contador.establecer(this.confirmado.cantidadTotal);
          }
          this.errores.update((m) => ({ ...m, [varianteId]: motivo }));
        },
      });
  }

  quitar(varianteId: number): void {
    this.api.quitarDelCarrito(varianteId, undefined, 'body', false, this.sinToast()).subscribe({
      next: (r) => this.aplicar(r.data),
      error: () => this.errores.update((m) => ({ ...m, [varianteId]: 'No pudimos quitar el producto' })),
    });
  }

  vaciar(): void {
    this.api.vaciarCarrito(undefined, 'body', false, this.sinToast()).subscribe({
      next: (r) => this.aplicar(r.data),
      error: () => this.estado.set('error'),
    });
  }

  private aplicar(carrito: orderApi.Carrito | undefined): void {
    if (!carrito) return;
    this.confirmado = carrito;
    this.aprender(carrito);
    this.carrito.set(carrito);
    this.errores.set({});
    this.avisos.set(carrito.avisos ?? []);
    this.contador.establecer(carrito.cantidadTotal);
    this.estado.set('listo');
  }

  /** Guarda los tramos y el stock que trae una respuesta del servidor. */
  private aprender(carrito: orderApi.Carrito): void {
    this.aprenderTramos(carrito);
    this.stock.update((anterior) => {
      const siguiente = { ...anterior };
      for (const l of carrito.lineas) {
        const exacto = this.stockExacto(l);
        if (l.estado === 'STOCK_INSUFICIENTE' && typeof l.disponible === 'number') {
          siguiente[l.varianteId] = l.disponible;
        } else if (exacto !== null) {
          siguiente[l.varianteId] = exacto; // CA-4-81: se sabe desde que se carga el carrito
        } else if (l.estado === 'OK' && l.varianteId in siguiente && l.cantidad > siguiente[l.varianteId]) {
          delete siguiente[l.varianteId]; // el stock creció: el dato viejo ya no vale
        } else if (typeof l.unidadesComprables === 'number' && siguiente[l.varianteId] < l.unidadesComprables) {
          delete siguiente[l.varianteId]; // unidadesComprables ya no limita (CA-4-82)
        }
      }
      return siguiente;
    });
  }

  /**
   * CA-4-81 y CA-4-82: `unidadesComprables` es el stock acotado al tramo más alto del producto. Solo es el stock
   * exacto cuando es menor que ese tramo; si es igual (o mayor que lo que se conoce), el stock real puede ser más
   * grande y no se limita. Se compara con el mayor tramo que el servidor ya mostró, que nunca supera al real.
   */
  private stockExacto(linea: orderApi.LineaCarrito): number | null {
    if (typeof linea.unidadesComprables !== 'number' || linea.productoId == null) return null;
    const conocidos = this.tramos.get(linea.productoId) ?? [];
    const masAlto = conocidos.length ? conocidos[conocidos.length - 1].unidadesMinimas : null;
    return masAlto !== null && linea.unidadesComprables < masAlto ? linea.unidadesComprables : null;
  }

  private aprenderTramos(carrito: orderApi.Carrito): void {
    for (const aviso of carrito.avisosTramo) {
      const conocidos = this.tramos.get(aviso.productoId) ?? [];
      for (const t of [aviso.tramoAlcanzado, aviso.siguienteTramo]) {
        if (t && !conocidos.some((c) => c.unidadesMinimas === t.unidadesMinimas)) {
          conocidos.push({ unidadesMinimas: t.unidadesMinimas, porcentaje: t.porcentaje });
        }
      }
      this.tramos.set(aviso.productoId, conocidos.sort((a, b) => a.unidadesMinimas - b.unidadesMinimas));
    }
  }

  /** Mismo cálculo que catalog: tramo más alto alcanzado por producto, descuento redondeado por línea. */
  private recalcular(base: orderApi.Carrito, varianteId: number, cantidad: number): orderApi.Carrito {
    const cambiada = base.lineas.find((l) => l.varianteId === varianteId);
    if (!cambiada) return base;
    const producto = cambiada.productoId;
    const delProducto = (l: orderApi.LineaCarrito) =>
      l.varianteId === varianteId || (producto != null && l.productoId === producto);
    const cantidadDe = (l: orderApi.LineaCarrito) => (l.varianteId === varianteId ? cantidad : l.cantidad);

    const unidades = base.lineas.filter(delProducto).reduce((suma, l) => suma + cantidadDe(l), 0);
    const conocidos = producto != null ? (this.tramos.get(producto) ?? []) : [];
    const alcanzado = [...conocidos].reverse().find((t) => t.unidadesMinimas <= unidades) ?? null;
    const siguiente = conocidos.find((t) => t.unidadesMinimas > unidades) ?? null;
    const porcentaje = alcanzado?.porcentaje ?? 0;

    const lineas = base.lineas.map((l) => {
      if (!delProducto(l) || l.estado === 'NO_DISPONIBLE') return l;
      const importe = redondear((l.precioUnitario ?? 0) * cantidadDe(l));
      const descuento = redondear((importe * porcentaje) / 100);
      return {
        ...l,
        cantidad: cantidadDe(l),
        descuentoPorcentaje: porcentaje,
        descuentoMonto: descuento,
        total: redondear(importe - descuento),
      };
    });
    const subtotal = redondear(lineas.reduce((s, l) => s + redondear((l.precioUnitario ?? 0) * l.cantidad), 0));
    const descuentoTotal = redondear(lineas.reduce((s, l) => s + (l.descuentoMonto ?? 0), 0));
    const avisosTramo = base.avisosTramo.map((a) =>
      a.productoId === producto
        ? {
            ...a,
            unidades,
            tramoAlcanzado: alcanzado,
            siguienteTramo: siguiente
              ? { ...siguiente, faltan: siguiente.unidadesMinimas - unidades }
              : null,
          }
        : a,
    );
    return {
      ...base,
      lineas,
      avisosTramo,
      subtotal,
      descuentoTotal,
      total: redondear(subtotal - descuentoTotal),
      cantidadTotal: lineas.reduce((s, l) => s + l.cantidad, 0),
    };
  }
}
