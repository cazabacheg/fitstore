import { CdkDrag, CdkDropList, moveItemInArray, type CdkDragDrop } from '@angular/cdk/drag-drop';
import { HttpContext } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { firstValueFrom, forkJoin, of } from 'rxjs';
import { SIN_TOAST, comoApiError, settingsApi } from 'shared';
import { DialogoService } from '../../ui/dialogo/dialogo.service';
import { SubidaImagenes } from '../../ui/subida-imagenes/subida-imagenes';

/** Máximo de imágenes en el carrusel de la landing (CA-3-14). */
export const MAXIMO_IMAGENES_LANDING = 5;
const ENLACE = /^(\/(?!\/)\S*|https:\/\/\S+)$/;

interface Fila {
  imagen: settingsApi.ImagenTienda;
  alt: string;
  enlace: string;
  errorEnlace: boolean;
}

const aFila = (imagen: settingsApi.ImagenTienda): Fila => ({
  imagen,
  alt: imagen.textoAlternativo ?? '',
  enlace: imagen.enlace ?? '',
  errorEnlace: false,
});

/**
 * Imágenes de la landing (CA-3-14): subida de hasta 5 (JPG o WebP, 5 MB), reordenar arrastrando o con los botones,
 * texto alternativo, enlace y eliminar. Cada cambio se guarda al momento en settings (las imágenes no usan la
 * versión de la configuración).
 */
@Component({
  selector: 'fs-imagenes-landing',
  imports: [SubidaImagenes, CdkDropList, CdkDrag],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="imagenes" aria-label="Imágenes de la landing">
      <h3>Imágenes del carrusel</h3>
      <p class="fs-suave">
        De 1 a 5 imágenes JPG o WebP de hasta 5 MB. Se ajustan a 1600 px de ancho. Sin imágenes, el
        hero usa el color primario.
      </p>

      <div
        cdkDropList
        class="imagenes__lista"
        (cdkDropListDropped)="soltar($event)"
        aria-label="Arrastra para cambiar el orden"
      >
        @for (fila of filas(); track fila.imagen.id; let i = $index; let ultima = $last) {
          <div cdkDrag class="imagenes__fila" data-testid="imagen-fila">
            <img
              [src]="fila.imagen.url"
              [alt]="fila.alt || 'Imagen ' + (i + 1) + ' de la landing'"
            />
            <div class="imagenes__campos">
              <label class="fs-campo__etiqueta" [for]="'alt-' + fila.imagen.id"
                >Texto alternativo</label
              >
              <input
                [id]="'alt-' + fila.imagen.id"
                class="fs-input"
                data-testid="imagen-alt"
                maxlength="150"
                [value]="fila.alt"
                (input)="editar(fila, 'alt', $any($event.target).value)"
              />
              <label class="fs-campo__etiqueta" [for]="'enlace-' + fila.imagen.id"
                >Enlace (opcional)</label
              >
              <input
                [id]="'enlace-' + fila.imagen.id"
                class="fs-input"
                data-testid="imagen-enlace"
                placeholder="/productos o https://…"
                [attr.aria-invalid]="fila.errorEnlace ? 'true' : null"
                [value]="fila.enlace"
                (input)="editar(fila, 'enlace', $any($event.target).value)"
              />
              @if (fila.errorEnlace) {
                <span class="fs-campo__error"
                  >Usa una ruta de la tienda (/productos) o una URL https</span
                >
              }
            </div>
            <div class="imagenes__acciones">
              <button
                type="button"
                class="fs-btn fs-btn--texto fs-btn--sm"
                data-testid="subir"
                [disabled]="i === 0"
                [attr.aria-label]="'Subir la imagen ' + (i + 1)"
                (click)="mover(i, -1)"
              >
                ▲
              </button>
              <button
                type="button"
                class="fs-btn fs-btn--texto fs-btn--sm"
                data-testid="bajar"
                [disabled]="ultima"
                [attr.aria-label]="'Bajar la imagen ' + (i + 1)"
                (click)="mover(i, 1)"
              >
                ▼
              </button>
              <button
                type="button"
                class="fs-btn fs-btn--contorno fs-btn--sm"
                data-testid="guardar-imagen"
                (click)="guardarFila(fila)"
              >
                Guardar
              </button>
              <button
                type="button"
                class="fs-btn fs-btn--peligro fs-btn--sm"
                data-testid="eliminar-imagen"
                (click)="eliminar(fila)"
              >
                Eliminar
              </button>
            </div>
          </div>
        }
      </div>

      @if (error()) {
        <p class="imagenes__error" role="alert" data-testid="error-imagenes">{{ error() }}</p>
      }

      @if (cupo() > 0) {
        @for (clave of [reinicio()]; track clave) {
          <fs-subida-imagenes
            [tipos]="['image/jpeg', 'image/webp']"
            [tamanoMaximoMb]="5"
            [maximoArchivos]="cupo()"
            (archivosChange)="alElegir($event)"
          />
        }
      } @else {
        <p class="fs-suave">Ya tienes 5 imágenes: elimina una para subir otra.</p>
      }
      @if (subiendo()) {
        <p class="fs-suave" role="status">Subiendo imágenes…</p>
      }
    </section>
  `,
  styles: `
    .imagenes__lista {
      display: grid;
      gap: 12px;
      margin-block: 16px;
    }
    .imagenes__fila {
      display: grid;
      grid-template-columns: 160px 1fr auto;
      gap: 16px;
      align-items: start;
      padding: 12px;
      border: 1px solid var(--fs-color-linea);
      border-radius: 8px;
      background: var(--fs-color-superficie, #fff);
    }
    .imagenes__fila img {
      width: 160px;
      height: 100px;
      object-fit: cover;
      border-radius: 6px;
    }
    .imagenes__campos {
      display: grid;
      gap: 6px;
    }
    .imagenes__acciones {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      max-width: 220px;
    }
    .imagenes__error {
      color: var(--fs-color-error, #b3261e);
    }
    @media (max-width: 720px) {
      .imagenes__fila {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class ImagenesLanding {
  private readonly api = inject(settingsApi.ConfiguracionAdminService);
  private readonly dialogo = inject(DialogoService);

  readonly imagenes = input.required<settingsApi.ImagenTienda[]>();
  /** La lista completa y ordenada después de cada cambio guardado. */
  readonly cambio = output<settingsApi.ImagenTienda[]>();

  protected readonly filas = signal<Fila[]>([]);
  protected readonly error = signal('');
  protected readonly subiendo = signal(false);
  protected readonly reinicio = signal(0);
  protected readonly cupo = computed(() => MAXIMO_IMAGENES_LANDING - this.filas().length);

  private readonly sinAviso = new HttpContext().set(SIN_TOAST, true);

  constructor() {
    effect(() => {
      this.filas.set(
        [...this.imagenes()].sort((a, b) => a.orden - b.orden || a.id - b.id).map(aFila),
      );
    });
  }

  /** Sube de a una (así el servidor asigna el orden en secuencia). Termina cuando todas respondieron. */
  async subir(archivos: File[]): Promise<void> {
    this.error.set('');
    this.subiendo.set(true);
    try {
      for (const archivo of archivos) {
        if (this.filas().length >= MAXIMO_IMAGENES_LANDING) break;
        const respuesta = await firstValueFrom(
          this.api.subirImagenTienda(archivo, 'LANDING', undefined, undefined, 'body', false, {
            context: this.sinAviso,
          }),
        );
        this.filas.update((f) => [...f, aFila(respuesta.data!)]);
      }
      this.emitir();
    } catch (e) {
      this.error.set(comoApiError(e)?.mensaje ?? 'No pudimos subir la imagen. Inténtalo de nuevo.');
    } finally {
      this.subiendo.set(false);
    }
  }

  protected alElegir(archivos: File[]): void {
    if (archivos.length === 0) return;
    const pendiente = this.subir(archivos);
    // La zona de subida se vuelve a crear vacía: las imágenes subidas ya están en la lista de arriba
    void pendiente.finally(() => this.reinicio.update((n) => n + 1));
  }

  protected editar(fila: Fila, campo: 'alt' | 'enlace', valor: string): void {
    this.filas.update((lista) =>
      lista.map((f) =>
        f.imagen.id === fila.imagen.id
          ? { ...f, [campo]: valor, errorEnlace: campo === 'enlace' ? false : f.errorEnlace }
          : f,
      ),
    );
  }

  protected guardarFila(filaVista: Fila): void {
    // Siempre se guarda lo último escrito, aunque la vista aún no se haya vuelto a dibujar
    const fila = this.filas().find((f) => f.imagen.id === filaVista.imagen.id) ?? filaVista;
    const enlace = fila.enlace.trim();
    if (enlace.length > 0 && !ENLACE.test(enlace)) {
      this.filas.update((l) =>
        l.map((f) => (f.imagen.id === fila.imagen.id ? { ...f, errorEnlace: true } : f)),
      );
      return;
    }
    this.error.set('');
    this.api
      .actualizarImagenTienda(
        fila.imagen.id,
        {
          orden: fila.imagen.orden,
          textoAlternativo: fila.alt.trim() || null,
          enlace: enlace || null,
        },
        'body',
        false,
        { context: this.sinAviso },
      )
      .subscribe({
        next: (r) => {
          this.reemplazar(r.data!);
          this.emitir();
        },
        error: (e: unknown) =>
          this.error.set(comoApiError(e)?.mensaje ?? 'No pudimos guardar la imagen.'),
      });
  }

  protected soltar(evento: CdkDragDrop<Fila[]>): void {
    this.reordenar(evento.previousIndex, evento.currentIndex);
  }

  protected mover(indice: number, delta: -1 | 1): void {
    this.reordenar(indice, indice + delta);
  }

  protected eliminar(fila: Fila): void {
    this.dialogo
      .confirmar({
        titulo: '¿Eliminar esta imagen de la landing?',
        mensaje: 'Dejará de mostrarse en el carrusel.',
        accion: 'Eliminar imagen',
        peligro: true,
      })
      .subscribe((confirmado) => {
        if (!confirmado) return;
        this.api
          .eliminarImagenTienda(fila.imagen.id, 'body', false, { context: this.sinAviso })
          .subscribe({
            next: () => {
              this.filas.update((l) => l.filter((f) => f.imagen.id !== fila.imagen.id));
              this.emitir();
            },
            error: (e: unknown) =>
              this.error.set(comoApiError(e)?.mensaje ?? 'No pudimos eliminar la imagen.'),
          });
      });
  }

  private reordenar(desde: number, hasta: number): void {
    if (desde === hasta || hasta < 0 || hasta >= this.filas().length) return;
    const lista = [...this.filas()];
    moveItemInArray(lista, desde, hasta);
    const reordenada = lista.map((f, i) => ({ ...f, imagen: { ...f.imagen, orden: i } }));
    const antes = new Map(this.filas().map((f) => [f.imagen.id, f.imagen.orden]));
    const cambiadas = reordenada.filter((f) => antes.get(f.imagen.id) !== f.imagen.orden);
    this.filas.set(reordenada);
    this.error.set('');
    const peticiones = cambiadas.map((f) =>
      this.api.actualizarImagenTienda(
        f.imagen.id,
        {
          orden: f.imagen.orden,
          textoAlternativo: f.imagen.textoAlternativo ?? null,
          enlace: f.imagen.enlace ?? null,
        },
        'body',
        false,
        { context: this.sinAviso },
      ),
    );
    (peticiones.length > 0 ? forkJoin(peticiones) : of([])).subscribe({
      next: () => this.emitir(),
      error: (e: unknown) =>
        this.error.set(comoApiError(e)?.mensaje ?? 'No pudimos guardar el nuevo orden.'),
    });
  }

  private reemplazar(imagen: settingsApi.ImagenTienda): void {
    this.filas.update((l) => l.map((f) => (f.imagen.id === imagen.id ? aFila(imagen) : f)));
  }

  private emitir(): void {
    this.cambio.emit(this.filas().map((f, i) => ({ ...f.imagen, orden: i })));
  }
}
