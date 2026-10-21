import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  input,
  output,
  signal,
} from '@angular/core';
import type { settingsApi } from 'shared';

export type TipoLogo = 'LOGO_CLARO' | 'LOGO_OSCURO' | 'FAVICON';

export interface DatosIdentidad {
  nombre: string;
  eslogan: string | null;
  razonSocial: string;
  ruc: string;
  direccionFiscal: string;
}

const DOS_MB = 2 * 1024 * 1024;

const LOGOS: {
  tipo: TipoLogo;
  id: string;
  etiqueta: string;
  tipos: string[];
  nombres: string;
  url: (i: settingsApi.Identidad) => string | null | undefined;
}[] = [
  {
    tipo: 'LOGO_CLARO',
    id: 'logo-claro',
    etiqueta: 'Logo claro',
    tipos: ['image/png', 'image/webp'],
    nombres: 'PNG o WebP',
    url: (i) => i.logoClaroUrl,
  },
  {
    tipo: 'LOGO_OSCURO',
    id: 'logo-oscuro',
    etiqueta: 'Logo oscuro',
    tipos: ['image/png', 'image/webp'],
    nombres: 'PNG o WebP',
    url: (i) => i.logoOscuroUrl,
  },
  {
    tipo: 'FAVICON',
    id: 'favicon',
    etiqueta: 'Favicon',
    tipos: ['image/png', 'image/webp', 'image/x-icon', 'image/vnd.microsoft.icon'],
    nombres: 'PNG, WebP o ICO',
    url: (i) => i.faviconUrl,
  },
];

/**
 * Pestaña Identidad (HU-3-02): nombre, eslogan, datos legales (razón social, RUC de 11 dígitos, dirección fiscal) y
 * logos. El navegador valida el tipo y los 2 MB; el servidor vuelve a validar el tipo real. Nunca se acepta SVG.
 */
@Component({
  selector: 'fs-identidad-tab',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="identidad" novalidate (submit)="guardarDatos($event)">
      <div class="fs-campo" [class.fs-campo--error]="!!error('nombre')">
        <label class="fs-campo__etiqueta" for="id-nombre">Nombre de la tienda</label>
        <input
          id="id-nombre"
          class="fs-input"
          data-testid="nombre"
          [value]="nombre()"
          [attr.aria-invalid]="error('nombre') ? 'true' : null"
          (input)="nombre.set($any($event.target).value)"
        />
        @if (error('nombre')) {
          <span class="fs-campo__error">{{ error('nombre') }}</span>
        }
      </div>
      <div class="fs-campo">
        <label class="fs-campo__etiqueta" for="id-eslogan">Eslogan (opcional)</label>
        <input
          id="id-eslogan"
          class="fs-input"
          data-testid="eslogan"
          [value]="eslogan()"
          (input)="eslogan.set($any($event.target).value)"
        />
      </div>
      <div class="fs-campo" [class.fs-campo--error]="!!error('razonSocial')">
        <label class="fs-campo__etiqueta" for="id-razon">Razón social</label>
        <input
          id="id-razon"
          class="fs-input"
          data-testid="razon-social"
          [value]="razonSocial()"
          [attr.aria-invalid]="error('razonSocial') ? 'true' : null"
          (input)="razonSocial.set($any($event.target).value)"
        />
        @if (error('razonSocial')) {
          <span class="fs-campo__error">{{ error('razonSocial') }}</span>
        }
      </div>
      <div class="fs-campo" [class.fs-campo--error]="!!error('ruc')">
        <label class="fs-campo__etiqueta" for="id-ruc">RUC</label>
        <input
          id="id-ruc"
          class="fs-input"
          data-testid="ruc"
          inputmode="numeric"
          maxlength="11"
          [value]="ruc()"
          [attr.aria-invalid]="error('ruc') ? 'true' : null"
          (input)="ruc.set($any($event.target).value)"
        />
        @if (error('ruc')) {
          <span class="fs-campo__error">{{ error('ruc') }}</span>
        }
      </div>
      <div class="fs-campo identidad__ancho" [class.fs-campo--error]="!!error('direccionFiscal')">
        <label class="fs-campo__etiqueta" for="id-direccion">Dirección fiscal</label>
        <input
          id="id-direccion"
          class="fs-input"
          data-testid="direccion-fiscal"
          [value]="direccionFiscal()"
          [attr.aria-invalid]="error('direccionFiscal') ? 'true' : null"
          (input)="direccionFiscal.set($any($event.target).value)"
        />
        @if (error('direccionFiscal')) {
          <span class="fs-campo__error">{{ error('direccionFiscal') }}</span>
        }
      </div>
      <div class="identidad__acciones identidad__ancho">
        <button
          type="submit"
          class="fs-btn fs-btn--primario"
          data-testid="guardar-identidad"
          [disabled]="guardando()"
        >
          {{ guardando() ? 'Guardando…' : 'Guardar identidad' }}
        </button>
      </div>
    </form>

    <section class="logos" aria-label="Logos y favicon">
      <h3>Logos y favicon</h3>
      <p class="fs-suave">
        Logos en PNG o WebP y favicon en PNG, WebP o ICO, de hasta 2 MB. No se aceptan SVG.
      </p>
      <div class="logos__rejilla">
        @for (logo of logos; track logo.tipo) {
          <div class="logos__item">
            <strong>{{ logo.etiqueta }}</strong>
            @if (logo.url(identidad()); as url) {
              <img
                class="logos__actual"
                [attr.data-testid]="logo.id + '-actual'"
                [src]="url"
                [alt]="logo.etiqueta + ' actual'"
              />
            } @else {
              <span class="fs-suave">Sin imagen</span>
            }
            <label class="fs-btn fs-btn--contorno fs-btn--sm">
              {{ logo.url(identidad()) ? 'Reemplazar' : 'Subir' }}
              <input
                type="file"
                hidden
                [attr.data-testid]="'archivo-' + logo.id"
                [accept]="logo.tipos.join(',')"
                (change)="elegir(logo, $any($event.target))"
              />
            </label>
            @if (rechazos()[logo.tipo]; as motivo) {
              <p class="logos__rechazo" role="alert" [attr.data-testid]="'rechazo-' + logo.id">
                {{ motivo }}
              </p>
            }
          </div>
        }
      </div>
    </section>
  `,
  styles: `
    .identidad {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: 16px;
      max-width: 820px;
    }
    .identidad__ancho {
      grid-column: 1 / -1;
    }
    .logos {
      margin-top: 32px;
      max-width: 820px;
    }
    .logos__rejilla {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px;
    }
    .logos__item {
      display: grid;
      gap: 8px;
      justify-items: start;
      padding: 16px;
      border: 1px solid var(--fs-color-linea);
      border-radius: 8px;
    }
    .logos__actual {
      max-width: 160px;
      max-height: 64px;
      object-fit: contain;
      background: var(--fs-color-superficie-alterna);
    }
    .logos__rechazo {
      margin: 0;
      color: var(--fs-color-error, #b3261e);
    }
  `,
})
export class IdentidadTab {
  readonly identidad = input.required<settingsApi.Identidad>();
  readonly guardando = input(false);
  /** Errores del servidor por campo (400), p. ej. `{ ruc: 'El RUC debe tener 11 dígitos' }`. */
  readonly erroresServidor = input<Record<string, string>>({});

  readonly guardar = output<DatosIdentidad>();
  readonly subirImagen = output<{ tipo: TipoLogo; archivo: File }>();
  readonly sinGuardar = output<boolean>();

  protected readonly logos = LOGOS;
  protected readonly nombre = signal('');
  protected readonly eslogan = signal('');
  protected readonly razonSocial = signal('');
  protected readonly ruc = signal('');
  protected readonly direccionFiscal = signal('');
  protected readonly rechazos = signal<Partial<Record<TipoLogo, string>>>({});
  private readonly intentado = signal(false);

  private readonly errores = computed<Record<string, string>>(() => {
    const e: Record<string, string> = {};
    const nombre = this.nombre().trim();
    if (nombre.length < 2 || nombre.length > 60)
      e['nombre'] = 'El nombre admite de 2 a 60 caracteres';
    const razon = this.razonSocial().trim();
    if (razon.length < 2 || razon.length > 150)
      e['razonSocial'] = 'La razón social admite de 2 a 150 caracteres';
    if (!/^\d{11}$/.test(this.ruc().trim())) e['ruc'] = 'El RUC debe tener 11 dígitos';
    const direccion = this.direccionFiscal().trim();
    if (direccion.length < 5 || direccion.length > 200)
      e['direccionFiscal'] = 'La dirección fiscal admite de 5 a 200 caracteres';
    if (this.eslogan().trim().length > 120) e['eslogan'] = 'El eslogan admite hasta 120 caracteres';
    return e;
  });

  /** Solo los datos de texto: si cambia un logo, lo que la persona está escribiendo no se pierde. */
  private readonly base = computed(
    () => {
      const i = this.identidad();
      return {
        nombre: i.nombre,
        eslogan: i.eslogan ?? '',
        razonSocial: i.razonSocial,
        ruc: i.ruc,
        direccionFiscal: i.direccionFiscal,
      };
    },
    { equal: (a, b) => JSON.stringify(a) === JSON.stringify(b) },
  );

  constructor() {
    effect(() => {
      const i = this.base();
      this.nombre.set(i.nombre);
      this.eslogan.set(i.eslogan);
      this.razonSocial.set(i.razonSocial);
      this.ruc.set(i.ruc);
      this.direccionFiscal.set(i.direccionFiscal);
      this.intentado.set(false);
    });
    effect(() => {
      const i = this.base();
      this.sinGuardar.emit(
        this.nombre() !== i.nombre ||
          this.eslogan() !== i.eslogan ||
          this.razonSocial() !== i.razonSocial ||
          this.ruc() !== i.ruc ||
          this.direccionFiscal() !== i.direccionFiscal,
      );
    });
  }

  protected error(campo: string): string {
    return this.erroresServidor()[campo] ?? (this.intentado() ? (this.errores()[campo] ?? '') : '');
  }

  protected guardarDatos(evento: Event): void {
    evento.preventDefault();
    this.intentado.set(true);
    if (Object.keys(this.errores()).length > 0) return;
    const eslogan = this.eslogan().trim();
    this.guardar.emit({
      nombre: this.nombre().trim(),
      eslogan: eslogan.length > 0 ? eslogan : null,
      razonSocial: this.razonSocial().trim(),
      ruc: this.ruc().trim(),
      direccionFiscal: this.direccionFiscal().trim(),
    });
  }

  protected elegir(logo: (typeof LOGOS)[number], entrada: HTMLInputElement): void {
    const archivo = entrada.files?.[0];
    entrada.value = '';
    if (!archivo) return;
    let motivo = '';
    if (archivo.type === 'image/svg+xml' || !logo.tipos.includes(archivo.type)) {
      motivo = `${logo.etiqueta}: solo se aceptan imágenes ${logo.nombres}.`;
    } else if (archivo.size > DOS_MB) {
      motivo = `${logo.etiqueta}: el archivo pesa más de 2 MB.`;
    }
    this.rechazos.update((r) => ({ ...r, [logo.tipo]: motivo }));
    if (!motivo) this.subirImagen.emit({ tipo: logo.tipo, archivo });
  }
}
