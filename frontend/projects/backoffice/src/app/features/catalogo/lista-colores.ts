import { HttpContext } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { SIN_TOAST, ToastService, catalogApi } from 'shared';
import { MaestrosAdminService } from 'shared/api/catalog-service';
import { DialogoService } from '../../ui/dialogo/dialogo.service';

const HEX = /^#[0-9a-fA-F]{6}$/;

/** Lista editable de colores con selector de color; el hex debe ser #RRGGBB. */
@Component({
  selector: 'fs-lista-colores',
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
    li .nombre {
      flex: 1;
    }
    .muestra {
      width: 22px;
      height: 22px;
      border-radius: 50%;
      border: 1px solid var(--fs-color-linea);
    }
    .alta {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      align-items: center;
    }
    input[type='color'] {
      width: 44px;
      height: 38px;
      padding: 2px;
    }
  `,
  template: `
    <h2>Colores</h2>
    <ul>
      @for (c of colores(); track c.id) {
        <li [attr.data-color]="c.id">
          <span class="muestra" [style.background]="c.hex"></span>
          <span class="nombre">{{ c.nombre }}</span>
          <code>{{ c.hex }}</code>
          <button
            type="button"
            class="fs-btn fs-btn--texto fs-btn--sm"
            [attr.aria-label]="'Eliminar color ' + c.nombre"
            (click)="eliminar(c)"
          >
            Eliminar
          </button>
        </li>
      }
    </ul>
    @if (enUso(); as nombre) {
      <p class="fs-campo__error" role="alert">
        No se puede eliminar el color {{ nombre }}: lo usa alguna variante.
      </p>
    }
    <div class="alta">
      <input
        class="fs-input"
        name="nuevoColor"
        placeholder="Nombre del color"
        [value]="nombre()"
        (input)="nombre.set($any($event.target).value)"
      />
      <input
        type="color"
        class="fs-input"
        aria-label="Elegir color"
        [value]="hexValido() ? hex() : '#000000'"
        (input)="hex.set($any($event.target).value)"
      />
      <input
        class="fs-input"
        name="hexColor"
        placeholder="#RRGGBB"
        maxlength="7"
        [value]="hex()"
        (input)="hex.set($any($event.target).value)"
      />
      <button
        type="button"
        class="fs-btn fs-btn--primario"
        [disabled]="!puedeAgregar()"
        (click)="agregar()"
      >
        Agregar color
      </button>
    </div>
    @if (hex() !== '' && !hexValido()) {
      <p class="fs-campo__error">Usa el formato #RRGGBB.</p>
    }
    @if (duplicado(); as c) {
      <p class="fs-campo__error">Ya existe el color {{ c.nombre }}.</p>
    }
  `,
})
export class ListaColores {
  private readonly api = inject(MaestrosAdminService);
  private readonly dialogo = inject(DialogoService);
  private readonly toasts = inject(ToastService);

  protected readonly colores = signal<catalogApi.Color[]>([]);
  protected readonly nombre = signal('');
  protected readonly hex = signal('#000000');
  protected readonly enUso = signal<string | null>(null);
  protected readonly hexValido = computed(() => HEX.test(this.hex()));
  protected readonly duplicado = computed(() => {
    const n = this.nombre().trim().toLowerCase();
    return n ? this.colores().find((c) => c.nombre.toLowerCase() === n) : undefined;
  });
  protected readonly puedeAgregar = computed(
    () => this.nombre().trim() !== '' && this.hexValido() && !this.duplicado(),
  );

  constructor() {
    this.api.listarColoresAdmin().subscribe((r) => this.colores.set(r.data ?? []));
  }

  protected agregar(): void {
    this.api.crearColor({ nombre: this.nombre().trim(), hex: this.hex() }).subscribe((r) => {
      if (r.data) this.colores.update((l) => [...l, r.data as catalogApi.Color]);
      this.nombre.set('');
    });
  }

  protected eliminar(c: catalogApi.Color): void {
    this.enUso.set(null);
    this.dialogo
      .confirmar({ titulo: `¿Eliminar el color ${c.nombre}?`, accion: 'Eliminar', peligro: true })
      .subscribe((si) => {
        if (!si) return;
        this.api
          .eliminarColor(c.id, 'body', false, { context: new HttpContext().set(SIN_TOAST, true) })
          .subscribe({
            next: () => this.colores.update((l) => l.filter((x) => x.id !== c.id)),
            error: (e: { status?: number }) => {
              if (e.status === 409) this.enUso.set(c.nombre);
              else this.toasts.mostrar('error', 'No pudimos eliminar el color.');
            },
          });
      });
  }
}
