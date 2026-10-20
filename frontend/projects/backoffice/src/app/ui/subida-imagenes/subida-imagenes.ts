import { CdkDrag, CdkDropList, moveItemInArray, type CdkDragDrop } from '@angular/cdk/drag-drop';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';

interface ArchivoSubido {
  archivo: File;
  vistaPrevia: string;
}

const NOMBRES_TIPO: Record<string, string> = {
  'image/jpeg': 'JPG',
  'image/png': 'PNG',
  'image/webp': 'WebP',
  'image/avif': 'AVIF',
};

/**
 * Zona de subida de imágenes (ficha `SubidaImagenes`): arrastrar y soltar, validación de tipo y tamaño en el navegador
 * (el backend vuelve a validar el tipo real), vista previa, reordenar arrastrando y quitar. Nunca acepta SVG.
 * Emite la lista actual de archivos en `archivosChange`; la subida al servidor la hace la pantalla que lo usa.
 */
@Component({
  selector: 'fs-subida-imagenes',
  imports: [CdkDropList, CdkDrag],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './subida-imagenes.html',
  styleUrl: './subida-imagenes.css',
})
export class SubidaImagenes {
  /** Tipos MIME aceptados (por ejemplo, `image/jpeg`). */
  readonly tipos = input.required<string[]>();
  readonly tamanoMaximoMb = input.required<number>();
  readonly maximoArchivos = input.required<number>();

  readonly archivosChange = output<File[]>();

  protected readonly archivos = signal<ArchivoSubido[]>([]);
  protected readonly rechazos = signal<string[]>([]);
  protected readonly arrastrando = signal(false);
  protected readonly aceptados = computed(() =>
    this.tipos()
      .filter((t) => t !== 'image/svg+xml')
      .join(','),
  );
  protected readonly nombresTipos = computed(() => {
    const nombres = this.tipos()
      .filter((t) => t !== 'image/svg+xml')
      .map((t) => NOMBRES_TIPO[t] ?? t);
    return nombres.length > 1
      ? `${nombres.slice(0, -1).join(', ')} o ${nombres.at(-1)}`
      : (nombres[0] ?? '');
  });

  constructor() {
    inject(DestroyRef).onDestroy(() =>
      this.archivos().forEach((a) => URL.revokeObjectURL?.(a.vistaPrevia)),
    );
  }

  protected sobre(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastrando.set(true);
  }

  protected soltar(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastrando.set(false);
    this.agregar(Array.from(evento.dataTransfer?.files ?? []));
  }

  protected elegidos(entrada: HTMLInputElement): void {
    this.agregar(Array.from(entrada.files ?? []));
    entrada.value = '';
  }

  protected quitar(indice: number): void {
    const [quitado] = this.archivos().splice(indice, 1);
    URL.revokeObjectURL?.(quitado.vistaPrevia);
    this.archivos.set([...this.archivos()]);
    this.emitir();
  }

  protected reordenar(evento: CdkDragDrop<ArchivoSubido[]>): void {
    const lista = [...this.archivos()];
    moveItemInArray(lista, evento.previousIndex, evento.currentIndex);
    this.archivos.set(lista);
    this.emitir();
  }

  private agregar(nuevos: File[]): void {
    const aceptados = [...this.archivos()];
    const rechazos: string[] = [];
    const maximoBytes = this.tamanoMaximoMb() * 1024 * 1024;
    for (const archivo of nuevos) {
      if (archivo.type === 'image/svg+xml' || !this.tipos().includes(archivo.type)) {
        rechazos.push(`${archivo.name}: solo se aceptan imágenes ${this.nombresTipos()}.`);
      } else if (archivo.size > maximoBytes) {
        rechazos.push(`${archivo.name}: pesa más de ${this.tamanoMaximoMb()} MB.`);
      } else if (aceptados.length >= this.maximoArchivos()) {
        rechazos.push(`${archivo.name}: el máximo es de ${this.maximoArchivos()} imágenes.`);
      } else {
        aceptados.push({ archivo, vistaPrevia: URL.createObjectURL(archivo) });
      }
    }
    this.rechazos.set(rechazos);
    this.archivos.set(aceptados);
    this.emitir();
  }

  private emitir(): void {
    this.archivosChange.emit(this.archivos().map((a) => a.archivo));
  }
}
