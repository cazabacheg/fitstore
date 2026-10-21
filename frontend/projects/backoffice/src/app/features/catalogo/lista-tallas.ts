import { CdkDrag, CdkDropList } from '@angular/cdk/drag-drop';
import { HttpContext } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { SIN_TOAST, ToastService, catalogApi } from 'shared';
import { forkJoin } from 'rxjs';
import { DialogoService } from '../../ui/dialogo/dialogo.service';

/** Lista editable de tallas: alta sin duplicados, orden arrastrable y baja (409 si alguna variante la usa). */
@Component({
  selector: 'fs-lista-tallas',
  imports: [CdkDropList, CdkDrag],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: grid;
      gap: var(--espacio-3);
      align-content: start;
    }
    ul {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      gap: 6px;
    }
    li {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 6px 10px;
      border: 1px solid var(--fs-color-linea);
      border-radius: var(--radio-md);
      background: var(--fs-color-superficie);
    }
    li .codigo {
      flex: 1;
      font-weight: 600;
    }
    .asa {
      cursor: grab;
      color: var(--fs-color-texto-suave);
    }
    .alta {
      display: flex;
      gap: 8px;
    }
  `,
  template: `
    <h2>Tallas</h2>
    <ul cdkDropList (cdkDropListDropped)="soltar($event)">
      @for (t of tallas(); track t.id) {
        <li cdkDrag [attr.data-talla]="t.id">
          <span class="asa" aria-hidden="true">⠿</span>
          <span class="codigo">{{ t.codigo }}</span>
          <button
            type="button"
            class="fs-btn fs-btn--texto fs-btn--sm"
            [attr.aria-label]="'Eliminar talla ' + t.codigo"
            (click)="eliminar(t)"
          >
            Eliminar
          </button>
        </li>
      }
    </ul>
    @if (enUso(); as codigo) {
      <p class="fs-campo__error" role="alert">
        No se puede eliminar la talla {{ codigo }}: la usa alguna variante.
      </p>
    }
    <div class="alta">
      <input
        class="fs-input"
        name="nuevaTalla"
        placeholder="Nueva talla (XS, S, M…)"
        [value]="nueva()"
        (input)="nueva.set($any($event.target).value)"
      />
      <button
        type="button"
        class="fs-btn fs-btn--primario"
        [disabled]="!!duplicada() || nueva().trim() === ''"
        (click)="agregar()"
      >
        Agregar talla
      </button>
    </div>
    @if (duplicada(); as t) {
      <p class="fs-campo__error">Ya existe la talla {{ t.codigo }}.</p>
    }
  `,
})
export class ListaTallas {
  private readonly api = inject(catalogApi.MaestrosAdminService);
  private readonly dialogo = inject(DialogoService);
  private readonly toasts = inject(ToastService);

  protected readonly tallas = signal<catalogApi.Talla[]>([]);
  protected readonly nueva = signal('');
  protected readonly enUso = signal<string | null>(null);
  protected readonly duplicada = computed(() => {
    const codigo = this.nueva().trim().toLowerCase();
    return codigo ? this.tallas().find((t) => t.codigo.toLowerCase() === codigo) : undefined;
  });

  constructor() {
    this.api
      .listarTallasAdmin()
      .subscribe((r) => this.tallas.set([...(r.data ?? [])].sort((a, b) => a.orden - b.orden)));
  }

  protected agregar(): void {
    const orden = Math.max(0, ...this.tallas().map((t) => t.orden)) + 1;
    this.api.crearTalla({ codigo: this.nueva().trim(), orden }).subscribe((r) => {
      if (r.data) this.tallas.update((l) => [...l, r.data as catalogApi.Talla]);
      this.nueva.set('');
    });
  }

  /** Reordena y guarda solo las tallas cuyo orden cambió (CA-2-35). */
  soltar(evento: { previousIndex: number; currentIndex: number }): void {
    if (evento.previousIndex === evento.currentIndex) return;
    const nueva = [...this.tallas()];
    const [movida] = nueva.splice(evento.previousIndex, 1);
    nueva.splice(evento.currentIndex, 0, movida);
    const reordenada = nueva.map((t, i) => ({ ...t, orden: i + 1 }));
    const cambiadas = reordenada.filter(
      (t) => this.tallas().find((x) => x.id === t.id)?.orden !== t.orden,
    );
    this.tallas.set(reordenada);
    forkJoin(
      cambiadas.map((t) =>
        this.api.editarTalla(t.id, { codigo: t.codigo, orden: t.orden, activo: t.activo }),
      ),
    ).subscribe(() => this.toasts.mostrar('exito', 'Orden de tallas guardado.'));
  }

  protected eliminar(t: catalogApi.Talla): void {
    this.enUso.set(null);
    this.dialogo
      .confirmar({ titulo: `¿Eliminar la talla ${t.codigo}?`, accion: 'Eliminar', peligro: true })
      .subscribe((si) => {
        if (!si) return;
        this.api
          .eliminarTalla(t.id, 'body', false, { context: new HttpContext().set(SIN_TOAST, true) })
          .subscribe({
            next: () => this.tallas.update((l) => l.filter((x) => x.id !== t.id)),
            error: (e: { status?: number }) => {
              if (e.status === 409) this.enUso.set(t.codigo);
              else this.toasts.mostrar('error', 'No pudimos eliminar la talla.');
            },
          });
      });
  }
}
