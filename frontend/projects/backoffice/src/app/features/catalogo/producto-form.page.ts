import { HttpContext } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SIN_TOAST, SesionStore, ToastService, camposConError, catalogApi, comoApiError } from 'shared';
import { CategoriasAdminService, MaestrosAdminService, ProductosAdminService } from 'shared/api/catalog-service';
import { forkJoin, map, of, switchMap } from 'rxjs';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { EstadoCargando } from '../../ui/estados/estado-cargando';
import { AvisoPublicacion } from './aviso-publicacion';
import { DescuentoProducto } from './descuento-producto';
import { ImagenesProducto } from './imagenes-producto';
import { VariantesProducto } from './variantes-producto';

type Pestana = 'datos' | 'variantes' | 'imagenes' | 'descuento';
const PESTANAS: { clave: Pestana; etiqueta: string }[] = [
  { clave: 'datos', etiqueta: 'Datos' },
  { clave: 'variantes', etiqueta: 'Variantes' },
  { clave: 'imagenes', etiqueta: 'Imágenes' },
  { clave: 'descuento', etiqueta: 'Descuento' },
];

interface OpcionCategoria {
  id: number;
  nombre: string;
  nivel: 0 | 1;
}

/** P-2-04: formulario de producto con pestañas Datos, Variantes, Imágenes y Descuento. OPERADOR: solo lectura. */
@Component({
  selector: 'fs-producto-form-page',
  imports: [
    RouterLink,
    EncabezadoPagina,
    EstadoCargando,
    AvisoPublicacion,
    VariantesProducto,
    ImagenesProducto,
    DescuentoProducto,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: grid;
      gap: var(--espacio-5);
    }
    .pestanas {
      display: flex;
      gap: var(--espacio-2);
      border-bottom: 1px solid var(--fs-color-linea);
    }
    .pestanas button {
      border: 0;
      background: transparent;
      padding: 10px 16px;
      cursor: pointer;
      font: inherit;
      border-bottom: 2px solid transparent;
    }
    .pestanas button[aria-selected='true'] {
      border-bottom-color: var(--fs-color-primario);
      font-weight: 600;
    }
    .pestanas button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    form {
      display: grid;
      gap: var(--espacio-4);
      max-width: 640px;
    }
    .categorias {
      display: grid;
      gap: 6px;
    }
    .categorias label {
      display: flex;
      gap: 8px;
      align-items: center;
    }
    .hija {
      padding-left: 24px;
    }
    .acciones {
      display: flex;
      gap: var(--espacio-3);
      flex-wrap: wrap;
    }
  `,
  template: `
    <fs-encabezado-pagina [titulo]="producto()?.nombre ?? 'Nuevo producto'">
      <div accion class="acciones">
        <a class="fs-btn fs-btn--contorno" routerLink="/catalogo/productos">‹ Productos</a>
        @if (!soloLectura() && producto(); as p) {
          @if (p.estado === 'ACTIVO') {
            <button
              type="button"
              class="fs-btn fs-btn--contorno"
              (click)="cambiarEstado('desactivar')"
            >
              Desactivar
            </button>
          } @else {
            <button
              type="button"
              class="fs-btn fs-btn--primario"
              (click)="cambiarEstado('publicar')"
            >
              Publicar
            </button>
          }
        }
      </div>
    </fs-encabezado-pagina>
    @if (!cargado()) {
      <fs-estado-cargando />
    } @else {
      <fs-aviso-publicacion [faltantes]="faltantes()" />
      <div class="pestanas" role="tablist">
        @for (p of pestanas; track p.clave) {
          <button
            type="button"
            role="tab"
            [attr.aria-selected]="pestana() === p.clave"
            [disabled]="p.clave !== 'datos' && !producto()"
            (click)="pestana.set(p.clave)"
          >
            {{ p.etiqueta }}
          </button>
        }
      </div>
      @switch (pestana()) {
        @case ('datos') {
          <form (submit)="$event.preventDefault(); guardar()">
            <label class="fs-campo" [class.fs-campo--error]="!!errores()['codigo']">
              <span class="fs-campo__etiqueta">Código</span>
              <input
                class="fs-input"
                name="codigo"
                [value]="codigo()"
                [disabled]="soloLectura()"
                [attr.aria-invalid]="errores()['codigo'] ? 'true' : null"
                (input)="codigo.set($any($event.target).value); limpiarError('codigo')"
              />
              @if (errores()['codigo']; as e) {
                <span class="fs-campo__error" data-error="codigo">{{ e }}</span>
              }
            </label>
            <label class="fs-campo" [class.fs-campo--error]="!!errores()['nombre']">
              <span class="fs-campo__etiqueta">Nombre</span>
              <input
                class="fs-input"
                name="nombre"
                [value]="nombre()"
                [disabled]="soloLectura()"
                [attr.aria-invalid]="errores()['nombre'] ? 'true' : null"
                (input)="nombre.set($any($event.target).value); limpiarError('nombre')"
              />
              @if (errores()['nombre']; as e) {
                <span class="fs-campo__error" data-error="nombre">{{ e }}</span>
              }
            </label>
            <label class="fs-campo" [class.fs-campo--error]="!!errores()['precioBase']">
              <span class="fs-campo__etiqueta">Precio base (S/)</span>
              <input
                class="fs-input"
                name="precioBase"
                type="number"
                min="0"
                step="0.01"
                [value]="precioBase()"
                [disabled]="soloLectura()"
                [attr.aria-invalid]="errores()['precioBase'] ? 'true' : null"
                (input)="precioBase.set($any($event.target).value); limpiarError('precioBase')"
              />
              @if (errores()['precioBase']; as e) {
                <span class="fs-campo__error" data-error="precioBase">{{ e }}</span>
              }
            </label>
            <label class="fs-campo" [class.fs-campo--error]="!!errores()['descripcion']">
              <span class="fs-campo__etiqueta">Descripción</span>
              <textarea
                class="fs-input"
                name="descripcion"
                rows="4"
                [value]="descripcion()"
                [disabled]="soloLectura()"
                [attr.aria-invalid]="errores()['descripcion'] ? 'true' : null"
                (input)="descripcion.set($any($event.target).value); limpiarError('descripcion')"
              ></textarea>
              @if (errores()['descripcion']; as e) {
                <span class="fs-campo__error" data-error="descripcion">{{ e }}</span>
              }
            </label>
            <fieldset class="categorias">
              <legend class="fs-campo__etiqueta">Categorías</legend>
              @for (c of opcionesCategoria(); track c.id) {
                <label [class.hija]="c.nivel === 1">
                  <input
                    type="checkbox"
                    [attr.data-categoria]="c.id"
                    [checked]="categoriaIds().includes(c.id)"
                    [disabled]="soloLectura()"
                    (change)="alternarCategoria(c.id)"
                  />
                  {{ c.nombre }}
                </label>
              }
            </fieldset>
            @if (!soloLectura()) {
              <div>
                <button type="submit" class="fs-btn fs-btn--primario" [disabled]="guardando()">
                  Guardar
                </button>
              </div>
            }
          </form>
        }
        @case ('variantes') {
          @if (producto(); as p) {
            <fs-variantes-producto
              [producto]="p"
              [tallas]="tallas()"
              [colores]="colores()"
              [soloLectura]="soloLectura()"
              (cambio)="producto.set($event)"
              (recargar)="recargar()"
            />
          }
        }
        @case ('imagenes') {
          @if (producto(); as p) {
            <fs-imagenes-producto
              [producto]="p"
              [soloLectura]="soloLectura()"
              (cambio)="producto.set($event)"
            />
          }
        }
        @case ('descuento') {
          @if (producto(); as p) {
            <fs-descuento-producto
              [producto]="p"
              [soloLectura]="soloLectura()"
              (cambio)="producto.set($event)"
            />
          }
        }
      }
    }
  `,
})
export class ProductoFormPage {
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(ProductosAdminService);
  private readonly maestros = inject(MaestrosAdminService);
  private readonly categoriasApi = inject(CategoriasAdminService);
  private readonly toasts = inject(ToastService);
  private readonly sesion = inject(SesionStore);

  protected readonly pestanas = PESTANAS;
  protected readonly pestana = signal<Pestana>('datos');
  protected readonly producto = signal<catalogApi.ProductoAdmin | null>(null);
  protected readonly cargado = signal(false);
  protected readonly guardando = signal(false);
  protected readonly faltantes = signal<string[]>([]);
  protected readonly tallas = signal<catalogApi.Talla[]>([]);
  protected readonly colores = signal<catalogApi.Color[]>([]);
  protected readonly categorias = signal<catalogApi.CategoriaAdmin[]>([]);

  protected readonly codigo = signal('');
  protected readonly nombre = signal('');
  protected readonly precioBase = signal<string | number>('');
  protected readonly descripcion = signal('');
  protected readonly categoriaIds = signal<number[]>([]);
  /** Errores por campo: los de la validación del cliente y los que devuelve el servidor (400 con `errores`). */
  protected readonly errores = signal<Record<string, string>>({});

  protected readonly soloLectura = computed(() => this.sesion.rol() !== 'ADMIN');
  protected readonly opcionesCategoria = computed<OpcionCategoria[]>(() =>
    this.categorias().flatMap((c) => [
      { id: c.id, nombre: c.nombre, nivel: 0 as const },
      ...c.hijas.map((h) => ({ id: h.id, nombre: h.nombre, nivel: 1 as const })),
    ]),
  );

  constructor() {
    this.ruta.paramMap
      .pipe(
        map((m) => m.get('id')),
        switchMap((id) =>
          forkJoin({
            producto: id
              ? this.api.obtenerProductoAdmin(+id).pipe(map((r) => r.data ?? null))
              : of(null),
            tallas: this.maestros.listarTallasAdmin().pipe(map((r) => r.data ?? [])),
            colores: this.maestros.listarColoresAdmin().pipe(map((r) => r.data ?? [])),
            categorias: this.categoriasApi.listarCategoriasAdmin().pipe(map((r) => r.data ?? [])),
          }),
        ),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe((d) => {
        this.tallas.set(d.tallas);
        this.colores.set(d.colores);
        this.categorias.set(d.categorias);
        this.establecer(d.producto);
        this.cargado.set(true);
      });
  }

  /** Reemplaza el producto abierto y refresca los campos del formulario. */
  private establecer(p: catalogApi.ProductoAdmin | null): void {
    this.producto.set(p);
    this.codigo.set(p?.codigo ?? '');
    this.nombre.set(p?.nombre ?? '');
    this.precioBase.set(p?.precioBase ?? '');
    this.descripcion.set(p?.descripcion ?? '');
    this.categoriaIds.set(p?.categoriaIds ?? []);
  }

  /**
   * Marcar una subcategoría marca también a su padre (el producto pertenece a ambas por herencia). Desmarcar al padre
   * desmarca sus subcategorías, para no dejar una hija marcada sin su padre; desmarcar solo una hija deja al padre.
   */
  protected alternarCategoria(id: number): void {
    const padre = this.categorias().find((c) => c.hijas.some((h) => h.id === id));
    const hijas = this.categorias().find((c) => c.id === id)?.hijas.map((h) => h.id) ?? [];
    this.categoriaIds.update((lista) => {
      if (lista.includes(id)) return lista.filter((x) => x !== id && !hijas.includes(x));
      return padre && !lista.includes(padre.id) ? [...lista, id, padre.id] : [...lista, id];
    });
  }

  protected limpiarError(campo: string): void {
    if (!this.errores()[campo]) return;
    this.errores.update((actuales) => Object.fromEntries(Object.entries(actuales).filter(([c]) => c !== campo)));
  }

  /** Las mismas reglas que `ProductoRequest` del contrato (código, nombre, precio y descripción). */
  private validar(): Record<string, string> {
    const errores: Record<string, string> = {};
    if (!/^[A-Z0-9]{2,20}$/.test(this.codigo().trim()))
      errores['codigo'] = 'El código usa de 2 a 20 letras mayúsculas o números';
    const nombre = this.nombre().trim();
    if (nombre.length < 3 || nombre.length > 120)
      errores['nombre'] = 'El nombre debe tener de 3 a 120 caracteres';
    if (!/^\d{1,8}(\.\d{1,2})?$/.test(String(this.precioBase()).trim()))
      errores['precioBase'] = 'Ingresa un precio igual o mayor que 0, con hasta 2 decimales';
    if (this.descripcion().trim().length > 5000)
      errores['descripcion'] = 'La descripción admite hasta 5000 caracteres';
    return errores;
  }

  protected guardar(): void {
    const invalidos = this.validar();
    this.errores.set(invalidos);
    if (Object.keys(invalidos).length > 0) return;
    const cuerpo: catalogApi.ProductoRequest = {
      codigo: this.codigo().trim(),
      nombre: this.nombre().trim(),
      descripcion: this.descripcion().trim(),
      precioBase: Number(this.precioBase()),
      categoriaIds: this.categoriaIds(),
      version: this.producto()?.version ?? null,
    };
    const actual = this.producto();
    this.guardando.set(true);
    // SIN_TOAST: los errores de campo se pintan bajo cada campo; el resto se avisa aquí
    const opciones = { context: new HttpContext().set(SIN_TOAST, true) };
    (actual
      ? this.api.editarProducto(actual.id, cuerpo, 'body', false, opciones)
      : this.api.crearProducto(cuerpo, 'body', false, opciones)
    ).subscribe({
      next: (r) => {
        this.guardando.set(false);
        this.toasts.mostrar(
          'exito',
          actual ? 'Cambios guardados.' : 'Producto creado en borrador.',
        );
        if (!actual && r.data) void this.router.navigate(['/catalogo/productos', r.data.id]);
        else this.establecer(r.data ?? actual);
      },
      error: (e: unknown) => {
        this.guardando.set(false);
        this.mostrarErrorDelServidor(e);
      },
    });
  }

  /** Cada error de campo va bajo su campo; lo que el formulario no tiene (o un error sin campos) sale en un aviso. */
  private mostrarErrorDelServidor(e: unknown): void {
    const campos = camposConError(e);
    const propios = ['codigo', 'nombre', 'precioBase', 'descripcion'];
    const porCampo = Object.fromEntries(
      campos.filter((c) => propios.includes(c.campo)).map((c) => [c.campo, c.mensaje]),
    );
    this.errores.set(porCampo);
    const otros = campos.filter((c) => !propios.includes(c.campo)).map((c) => c.mensaje);
    if (otros.length > 0) this.toasts.mostrar('error', otros.join('. '));
    else if (campos.length === 0)
      this.toasts.mostrar(
        'error',
        comoApiError(e)?.mensaje ?? 'No pudimos guardar el producto. Inténtalo de nuevo.',
      );
  }

  protected cambiarEstado(accion: 'publicar' | 'desactivar'): void {
    const p = this.producto();
    if (!p) return;
    this.faltantes.set([]);
    const opciones = { context: new HttpContext().set(SIN_TOAST, true) };
    const llamada =
      accion === 'publicar'
        ? this.api.publicarProducto(p.id, 'body', false, opciones)
        : this.api.desactivarProducto(p.id, 'body', false, opciones);
    llamada.subscribe({
      next: (r) => {
        if (r.data) this.producto.set(r.data);
        this.toasts.mostrar(
          'exito',
          accion === 'publicar' ? 'Producto publicado.' : 'Producto desactivado.',
        );
      },
      error: (e: unknown) => {
        const error = comoApiError(e);
        const lista = error?.detalles?.['faltantes'];
        if (Array.isArray(lista)) this.faltantes.set(lista.map(String));
        else if (error) this.toasts.mostrar('error', error.mensaje);
      },
    });
  }

  protected recargar(): void {
    const p = this.producto();
    if (p) this.api.obtenerProductoAdmin(p.id).subscribe((r) => this.producto.set(r.data ?? p));
  }
}
