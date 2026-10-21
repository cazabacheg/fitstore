import { CdkDrag, CdkDropList, type CdkDragDrop } from '@angular/cdk/drag-drop';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { ToastService, catalogApi } from 'shared';
import { concat, last } from 'rxjs';
import { DialogoService } from '../../ui/dialogo/dialogo.service';
import { SubidaImagenes } from '../../ui/subida-imagenes/subida-imagenes';

const MAXIMO_IMAGENES = 10;

/** Pestaña Imágenes: subida con color obligatorio (si hay varios), orden arrastrable y foto principal. */
@Component({
  selector: 'fs-imagenes-producto',
  imports: [SubidaImagenes, CdkDropList, CdkDrag],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: grid;
      gap: var(--espacio-5);
    }
    .lista {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
      gap: var(--espacio-4);
    }
    figure {
      margin: 0;
      display: grid;
      gap: 8px;
      padding: 8px;
      border: 1px solid var(--fs-color-linea);
      border-radius: var(--radio-md);
      background: var(--fs-color-superficie);
    }
    figure img {
      width: 100%;
      aspect-ratio: 4 / 5;
      object-fit: cover;
      border-radius: var(--radio-sm, 6px);
      cursor: grab;
    }
    label {
      display: flex;
      gap: 6px;
      align-items: center;
    }
  `,
  template: `
    @if (!soloLectura()) {
      @if (imagenes().length >= maximo) {
        <p role="status">Ya tienes {{ maximo }} imágenes. Elimina una para subir otra.</p>
      } @else {
        @for (k of [clave()]; track k) {
          <fs-subida-imagenes
            [tipos]="tipos"
            [tamanoMaximoMb]="5"
            [maximoArchivos]="maximo - imagenes().length"
            (archivosChange)="archivos.set($event)"
          />
        }
        @if (colores().length > 1) {
          <label>
            Color de las fotos
            <select
              class="fs-select"
              name="colorNuevo"
              [value]="colorNuevo() ?? ''"
              (change)="colorNuevo.set(+$any($event.target).value || null)"
            >
              <option value="">Elige un color</option>
              @for (c of colores(); track c.id) {
                <option [value]="c.id">{{ c.nombre }}</option>
              }
            </select>
          </label>
          @if (archivos().length > 0 && !colorElegido()) {
            <p class="fs-campo__error">Elige el color de las fotos.</p>
          }
        }
        <div>
          <button
            type="button"
            class="fs-btn fs-btn--primario"
            [disabled]="archivos().length === 0 || !colorElegido() || subiendo()"
            (click)="subir()"
          >
            Subir {{ archivos().length }} {{ archivos().length === 1 ? 'imagen' : 'imágenes' }}
          </button>
        </div>
      }
    }
    <div
      cdkDropList
      cdkDropListOrientation="mixed"
      class="lista"
      (cdkDropListDropped)="reordenar($event)"
      [cdkDropListDisabled]="soloLectura()"
    >
      @for (img of ordenadas(); track img.id) {
        <figure cdkDrag [cdkDragDisabled]="soloLectura()">
          <img
            [src]="img.urlMiniatura"
            [alt]="'Foto ' + img.orden + ' de ' + nombreColor(img.colorId)"
          />
          <label>
            <input
              type="radio"
              name="principal"
              [attr.data-imagen]="img.id"
              [checked]="img.principal"
              [disabled]="soloLectura()"
              (click)="marcarPrincipal(img)"
            />
            Principal
          </label>
          <select
            class="fs-select"
            [attr.aria-label]="'Color de la foto ' + img.orden"
            [disabled]="soloLectura()"
            (change)="cambiarColor(img, +$any($event.target).value)"
          >
            @for (c of colores(); track c.id) {
              <option [value]="c.id" [selected]="c.id === img.colorId">{{ c.nombre }}</option>
            }
          </select>
          @if (!soloLectura()) {
            <button type="button" class="fs-btn fs-btn--texto fs-btn--sm" (click)="eliminar(img)">
              Eliminar
            </button>
          }
        </figure>
      }
    </div>
  `,
})
export class ImagenesProducto {
  private readonly api = inject(catalogApi.ProductosAdminService);
  private readonly dialogo = inject(DialogoService);
  private readonly toasts = inject(ToastService);

  readonly producto = input.required<catalogApi.ProductoAdmin>();
  readonly soloLectura = input(false);
  readonly cambio = output<catalogApi.ProductoAdmin>();

  protected readonly maximo = MAXIMO_IMAGENES;
  protected readonly tipos = ['image/jpeg', 'image/png', 'image/webp'];
  protected readonly archivos = signal<File[]>([]);
  protected readonly colorNuevo = signal<number | null>(null);
  protected readonly clave = signal(0);
  protected readonly subiendo = signal(false);

  protected readonly imagenes = computed(() => this.producto().imagenes);
  protected readonly ordenadas = computed(() =>
    [...this.imagenes()].sort((a, b) => a.orden - b.orden),
  );
  /** Colores de las variantes activas: son los que pueden llevar fotos. */
  protected readonly colores = computed(() => {
    const vistos = new Map<number, { id: number; nombre: string }>();
    for (const v of this.producto().variantes.filter((x) => x.activo))
      vistos.set(v.colorId, { id: v.colorId, nombre: v.color });
    return [...vistos.values()];
  });
  /** Con un solo color se asigna solo (CA-2-32). */
  protected readonly colorElegido = computed(() =>
    this.colores().length === 1 ? this.colores()[0].id : this.colorNuevo(),
  );

  protected nombreColor(id: number): string {
    return this.colores().find((c) => c.id === id)?.nombre ?? '';
  }

  protected subir(): void {
    const colorId = this.colorElegido();
    if (colorId === null || this.archivos().length === 0) return;
    const sinPrincipal = this.imagenes().length === 0;
    this.subiendo.set(true);
    concat(
      ...this.archivos().map((archivo, i) =>
        this.api.subirImagenProducto(this.producto().id, archivo, colorId, sinPrincipal && i === 0),
      ),
    )
      .pipe(last())
      .subscribe({
        next: (r) => {
          if (r.data) this.cambio.emit(r.data);
          this.archivos.set([]);
          this.clave.update((k) => k + 1);
          this.subiendo.set(false);
          this.toasts.mostrar('exito', 'Imágenes subidas.');
        },
        error: () => this.subiendo.set(false),
      });
  }

  protected marcarPrincipal(img: catalogApi.ImagenProducto): void {
    if (img.principal) return;
    this.editar(img, { orden: img.orden, principal: true, colorId: img.colorId });
  }

  protected cambiarColor(img: catalogApi.ImagenProducto, colorId: number): void {
    if (colorId === img.colorId) return;
    this.editar(img, { orden: img.orden, principal: img.principal, colorId });
  }

  protected reordenar(evento: CdkDragDrop<unknown>): void {
    if (evento.previousIndex === evento.currentIndex) return;
    const img = this.ordenadas()[evento.previousIndex];
    const destino = this.ordenadas()[evento.currentIndex];
    this.editar(img, { orden: destino.orden, principal: img.principal, colorId: img.colorId });
  }

  protected eliminar(img: catalogApi.ImagenProducto): void {
    this.dialogo
      .confirmar({
        titulo: '¿Eliminar esta foto?',
        mensaje: 'Se borran también sus archivos.',
        accion: 'Eliminar',
        peligro: true,
      })
      .subscribe((si) => {
        if (si)
          this.api.eliminarImagenProducto(img.id).subscribe((r) => {
            if (r.data) this.cambio.emit(r.data);
          });
      });
  }

  private editar(img: catalogApi.ImagenProducto, cuerpo: catalogApi.ImagenProductoRequest): void {
    this.api.editarImagenProducto(img.id, cuerpo).subscribe((r) => {
      if (r.data) this.cambio.emit(r.data);
    });
  }
}
