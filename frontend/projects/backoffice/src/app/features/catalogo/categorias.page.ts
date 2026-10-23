import { HttpContext } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { SIN_TOAST, ToastService, catalogApi } from 'shared';
import { CategoriasAdminService, TiendaService } from 'shared/api/catalog-service';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { DialogoService } from '../../ui/dialogo/dialogo.service';
import { EstadoCargando } from '../../ui/estados/estado-cargando';
import { EstadoError } from '../../ui/estados/estado-error';
import { ArbolCategorias } from './arbol-categorias';
import { DialogoCategoria, type DatosDialogoCategoria } from './dialogo-categoria';
import { EditorGuiaTallas } from './editor-guia-tallas';

type Nodo = catalogApi.CategoriaAdmin | catalogApi.CategoriaAdminHija;
const GUIA_VACIA: catalogApi.GuiaTallas = { columnas: ['Talla'], filas: [], nota: null };

/** P-2-05: árbol de categorías y guía de tallas de la categoría elegida. */
@Component({
  selector: 'fs-categorias-page',
  imports: [EncabezadoPagina, ArbolCategorias, EditorGuiaTallas, EstadoCargando, EstadoError],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: grid;
      gap: var(--espacio-4);
    }
    .cuerpo {
      display: grid;
      grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
      align-items: start;
      gap: var(--espacio-6);
    }
    .aviso {
      padding: var(--espacio-3) var(--espacio-4);
      border: 1px solid var(--fs-color-error);
      border-radius: var(--radio-md);
    }
    @media (max-width: 1280px) {
      .cuerpo {
        grid-template-columns: minmax(0, 1fr);
      }
    }
  `,
  template: `
    <fs-encabezado-pagina titulo="Categorías">
      <div accion>
        <button type="button" class="fs-btn fs-btn--primario" (click)="nueva()">
          Nueva categoría
        </button>
      </div>
    </fs-encabezado-pagina>
    @if (error()) {
      <fs-estado-error (reintentar)="cargar()" />
    } @else if (categorias() === null) {
      <fs-estado-cargando />
    } @else {
      @if (enUso(); as n) {
        <div class="aviso" role="alert">
          No se puede eliminar «{{ n.nombre }}» porque tiene subcategorías o productos; desactívala
          para que deje de verse en la tienda.
          <button
            type="button"
            class="fs-btn fs-btn--contorno fs-btn--sm"
            (click)="alternar(n); enUso.set(null)"
          >
            Desactivar {{ n.nombre }}
          </button>
        </div>
      }
      <div class="cuerpo">
        <fs-arbol-categorias
          [categorias]="categorias()!"
          [(seleccionada)]="seleccionada"
          (elegir)="elegir($event)"
          (editar)="editar($event)"
          (alternar)="alternar($event)"
          (eliminar)="eliminar($event)"
          (ordenar)="ordenar($event)"
        />
        <section>
          @if (guia(); as g) {
            <h2>Guía de tallas de {{ nombreSeleccionada() }}</h2>
            <fs-editor-guia-tallas [guia]="g" (guiaChange)="guia.set($event)" />
            <button type="button" class="fs-btn fs-btn--primario" (click)="guardarGuia()">
              Guardar guía
            </button>
          } @else {
            <p class="fs-suave">Elige una categoría para editar su guía de tallas.</p>
          }
        </section>
      </div>
    }
  `,
})
export class CategoriasPage {
  private readonly api = inject(CategoriasAdminService);
  private readonly tienda = inject(TiendaService);
  private readonly dialogo = inject(MatDialog);
  private readonly confirmacion = inject(DialogoService);
  private readonly toasts = inject(ToastService);

  protected readonly categorias = signal<catalogApi.CategoriaAdmin[] | null>(null);
  protected readonly error = signal(false);
  protected readonly seleccionada = signal<number | null>(null);
  protected readonly guia = signal<catalogApi.GuiaTallas | null>(null);
  protected readonly enUso = signal<Nodo | null>(null);

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.error.set(false);
    this.api.listarCategoriasAdmin().subscribe({
      next: (r) => this.categorias.set(r.data ?? []),
      error: () => this.error.set(true),
    });
  }

  protected nombreSeleccionada(): string {
    const id = this.seleccionada();
    return (
      (this.categorias() ?? []).flatMap((c) => [c as Nodo, ...c.hijas]).find((n) => n.id === id)
        ?.nombre ?? ''
    );
  }

  protected nueva(): void {
    this.abrir(null);
  }

  protected editar(n: Nodo): void {
    this.abrir(n);
  }

  private abrir(categoria: Nodo | null): void {
    const datos: DatosDialogoCategoria = { categoria, raices: this.categorias() ?? [] };
    this.dialogo
      .open<DialogoCategoria, DatosDialogoCategoria, catalogApi.CategoriaRequest | null>(
        DialogoCategoria,
        { data: datos, width: '440px', maxWidth: '100%' },
      )
      .afterClosed()
      .subscribe((cuerpo) => {
        if (!cuerpo) return;
        const llamada = categoria
          ? this.api.editarCategoria(categoria.id, cuerpo)
          : this.api.crearCategoria(cuerpo);
        llamada.subscribe(() => {
          this.toasts.mostrar('exito', categoria ? 'Categoría guardada.' : 'Categoría creada.');
          this.cargar();
        });
      });
  }

  protected alternar(n: Nodo): void {
    (n.activo ? this.api.desactivarCategoria(n.id) : this.api.activarCategoria(n.id)).subscribe(
      () => this.cargar(),
    );
  }

  protected eliminar(n: Nodo): void {
    this.enUso.set(null);
    this.confirmacion
      .confirmar({
        titulo: `¿Eliminar ${n.nombre}?`,
        mensaje: 'Esta acción no se puede deshacer.',
        accion: 'Eliminar',
        peligro: true,
      })
      .subscribe((si) => {
        if (!si) return;
        this.api
          .eliminarCategoria(n.id, 'body', false, {
            context: new HttpContext().set(SIN_TOAST, true),
          })
          .subscribe({
            next: () => {
              this.toasts.mostrar('exito', 'Categoría eliminada.');
              this.cargar();
            },
            error: (e: { status?: number }) => {
              if (e.status === 409) this.enUso.set(n);
              else this.toasts.mostrar('error', 'No pudimos eliminar la categoría.');
            },
          });
      });
  }

  protected ordenar(o: { id: number; orden: number }): void {
    this.api
      .ordenarCategoria(o.id, { orden: o.orden })
      .subscribe((r) => this.categorias.set(r.data ?? this.categorias()));
  }

  protected elegir(n: Nodo): void {
    this.guia.set(null);
    if (!n.tieneGuiaTallas) {
      this.guia.set({ ...GUIA_VACIA, filas: [] });
      return;
    }
    this.tienda.obtenerGuiaTallas(n.slug).subscribe((r) => {
      if (this.seleccionada() === n.id) this.guia.set(r.data ?? { ...GUIA_VACIA, filas: [] });
    });
  }

  protected guardarGuia(): void {
    const id = this.seleccionada();
    const guia = this.guia();
    if (id === null || !guia) return;
    this.api.guardarGuiaTallas(id, guia).subscribe((r) => {
      this.guia.set(r.data ?? guia);
      this.toasts.mostrar('exito', 'Guía de tallas guardada.');
      this.cargar();
    });
  }
}
