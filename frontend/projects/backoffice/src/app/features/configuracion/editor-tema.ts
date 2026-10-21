import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  input,
  output,
  signal,
} from '@angular/core';
import { TEMA_FITSTORE, contraste, type settingsApi } from 'shared';
import { VistaPreviaTema, type AppPrevia, type DispositivoPrevia } from './vista-previa-tema';

type ClaveColor = keyof settingsApi.Tema;

const COLORES: { clave: ClaveColor; sufijo: string; etiqueta: string; ayuda: string }[] = [
  {
    clave: 'colorPrimario',
    sufijo: 'primario',
    etiqueta: 'Color primario',
    ayuda: 'Botones y detalles de marca.',
  },
  {
    clave: 'colorSecundario',
    sufijo: 'secundario',
    etiqueta: 'Color secundario',
    ayuda: 'Cabecera y pie.',
  },
  {
    clave: 'colorAcento',
    sufijo: 'acento',
    etiqueta: 'Color de acento',
    ayuda: 'Insignias y llamados secundarios.',
  },
  {
    clave: 'colorFondo',
    sufijo: 'fondo',
    etiqueta: 'Color de fondo',
    ayuda: 'Fondo de las páginas.',
  },
  { clave: 'colorTexto', sufijo: 'texto', etiqueta: 'Color de texto', ayuda: 'Texto principal.' },
];

const HEX = /^#[0-9A-Fa-f]{6}$/;
const MINIMO_AA = 4.5;
const BLANCO = '#FFFFFF';

/**
 * Pestaña Tema de la configuración (HU-3-03): cinco colores con vista previa en vivo, advertencias de contraste que
 * nunca bloquean el guardado y "Restaurar tema por defecto" (solo rellena el formulario; no guarda).
 */
@Component({
  selector: 'fs-editor-tema',
  imports: [VistaPreviaTema],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="tema">
      <form class="tema__form" (submit)="guardarTema($event)">
        @for (c of colores; track c.clave) {
          <div class="tema__color">
            <label class="fs-campo__etiqueta" [for]="'selector-' + c.sufijo">{{
              c.etiqueta
            }}</label>
            <div class="tema__controles">
              <input
                type="color"
                class="tema__selector"
                [id]="'selector-' + c.sufijo"
                [attr.data-testid]="'selector-' + c.sufijo"
                [value]="valorColor(c.clave)"
                (input)="cambiar(c.clave, $any($event.target).value)"
              />
              <input
                type="text"
                class="fs-input tema__hex"
                maxlength="7"
                [attr.data-testid]="'color-' + c.sufijo"
                [attr.aria-label]="c.etiqueta + ' (código hexadecimal)'"
                [attr.aria-invalid]="esValido(c.clave) ? null : 'true'"
                [value]="valores()[c.clave]"
                (input)="cambiar(c.clave, $any($event.target).value)"
              />
            </div>
            <span class="fs-campo__ayuda">{{ c.ayuda }}</span>
            @if (!esValido(c.clave)) {
              <span class="fs-campo__error">Usa el formato #RRGGBB</span>
            }
          </div>
        }
        @if (advertencias().length > 0) {
          <ul class="fs-aviso fs-aviso--info tema__avisos" aria-label="Advertencias de contraste">
            @for (a of advertencias(); track a) {
              <li data-testid="advertencia">{{ a }}</li>
            }
            <li class="fs-suave">
              Puedes guardar igual: el contraste bajo es una advertencia, no un error.
            </li>
          </ul>
        }
        <div class="tema__acciones">
          <button
            type="button"
            class="fs-btn fs-btn--contorno"
            data-testid="restaurar"
            (click)="restaurar()"
          >
            Restaurar tema por defecto
          </button>
          <button
            type="submit"
            class="fs-btn fs-btn--primario"
            data-testid="guardar-tema"
            [disabled]="guardando() || !todoValido()"
          >
            {{ guardando() ? 'Guardando…' : 'Guardar tema' }}
          </button>
        </div>
        @if (restaurado()) {
          <p class="fs-suave" role="status">
            Valores de FitStore cargados, sin guardar hasta que confirmes.
          </p>
        }
      </form>

      <section class="tema__previa" aria-label="Vista previa">
        <div class="tema__opciones" role="group" aria-label="Tipo de vista previa">
          <button
            type="button"
            class="fs-chip"
            data-testid="app-tienda"
            [attr.aria-pressed]="app() === 'tienda'"
            (click)="app.set('tienda')"
          >
            Tienda
          </button>
          <button
            type="button"
            class="fs-chip"
            data-testid="app-backoffice"
            [attr.aria-pressed]="app() === 'backoffice'"
            (click)="app.set('backoffice')"
          >
            Backoffice
          </button>
          <button
            type="button"
            class="fs-chip"
            data-testid="disp-escritorio"
            [attr.aria-pressed]="dispositivo() === 'escritorio'"
            (click)="dispositivo.set('escritorio')"
          >
            Escritorio
          </button>
          <button
            type="button"
            class="fs-chip"
            data-testid="disp-movil"
            [attr.aria-pressed]="dispositivo() === 'movil'"
            (click)="dispositivo.set('movil')"
          >
            Móvil
          </button>
        </div>
        <fs-vista-previa-tema
          [tema]="valores()"
          [app]="app()"
          [dispositivo]="dispositivo()"
          [nombre]="nombreTienda()"
        />
      </section>
    </div>
  `,
  styles: `
    .tema {
      display: grid;
      grid-template-columns: minmax(280px, 380px) 1fr;
      gap: 32px;
      align-items: start;
    }
    .tema__form {
      display: grid;
      gap: 16px;
    }
    .tema__color {
      display: grid;
      gap: 4px;
    }
    .tema__controles {
      display: flex;
      gap: 8px;
      align-items: center;
    }
    .tema__selector {
      width: 44px;
      height: 36px;
      padding: 0;
      border: 1px solid var(--fs-color-borde-control, #8a8478);
      border-radius: 6px;
      background: none;
    }
    .tema__hex {
      width: 120px;
      font-family: monospace;
      text-transform: uppercase;
    }
    .tema__avisos {
      margin: 0;
      padding: 12px 16px 12px 32px;
    }
    .tema__acciones {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
    }
    .tema__opciones {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-bottom: 12px;
    }
    .tema__opciones button[aria-pressed='true'] {
      background: var(--fs-color-primario-suave);
      border-color: var(--fs-color-primario-texto);
    }
    @media (max-width: 900px) {
      .tema {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class EditorTema {
  readonly tema = input.required<settingsApi.Tema>();
  readonly nombreTienda = input('Tu tienda');
  readonly guardando = input(false);
  /** Advertencias que devolvió el servidor al guardar. */
  readonly advertenciasServidor = input<string[]>([]);

  readonly guardar = output<settingsApi.Tema>();
  /** Hay colores cambiados que todavía no se guardaron. */
  readonly sinGuardar = output<boolean>();

  protected readonly colores = COLORES;
  protected readonly valores = signal<settingsApi.Tema>(TEMA_FITSTORE);
  protected readonly app = signal<AppPrevia>('tienda');
  protected readonly dispositivo = signal<DispositivoPrevia>('escritorio');
  protected readonly restaurado = signal(false);

  protected readonly todoValido = computed(() => COLORES.every((c) => this.esValido(c.clave)));

  /** Advertencias calculadas al instante con los colores escritos (más las del servidor, sin repetir). */
  protected readonly advertencias = computed(() => {
    const v = this.valores();
    const avisos: string[] = [];
    if (COLORES.every((c) => HEX.test(v[c.clave]))) {
      const textoFondo = contraste(v.colorTexto, v.colorFondo);
      if (textoFondo < MINIMO_AA) {
        avisos.push(`Contraste texto/fondo ${textoFondo.toFixed(1)}:1 (recomendado ≥ 4.5:1)`);
      }
      const sobreBlanco = contraste(v.colorPrimario, BLANCO);
      const sobreTexto = contraste(v.colorPrimario, v.colorTexto);
      if (sobreBlanco < MINIMO_AA && sobreTexto < MINIMO_AA) {
        avisos.push(
          `Sobre el color primario no se lee ni el texto blanco ni el color de texto (mejor contraste ${Math.max(sobreBlanco, sobreTexto).toFixed(1)}:1, recomendado ≥ 4.5:1)`,
        );
      }
    }
    for (const a of this.advertenciasServidor()) {
      if (!avisos.includes(a)) avisos.push(a);
    }
    return avisos;
  });

  constructor() {
    effect(() => {
      this.valores.set({ ...this.tema() });
      this.restaurado.set(false);
    });
    effect(() => {
      const original = this.tema();
      const actual = this.valores();
      this.sinGuardar.emit(
        COLORES.some((c) => actual[c.clave].toUpperCase() !== original[c.clave].toUpperCase()),
      );
    });
  }

  protected esValido(clave: ClaveColor): boolean {
    return HEX.test(this.valores()[clave]);
  }

  protected valorColor(clave: ClaveColor): string {
    const valor = this.valores()[clave];
    return HEX.test(valor) ? valor.toLowerCase() : '#000000';
  }

  protected cambiar(clave: ClaveColor, valor: string): void {
    this.restaurado.set(false);
    this.valores.update((v) => ({ ...v, [clave]: valor.trim() }));
  }

  protected restaurar(): void {
    this.valores.set({ ...TEMA_FITSTORE });
    this.restaurado.set(true);
  }

  protected guardarTema(evento: Event): void {
    evento.preventDefault();
    if (this.guardando() || !this.todoValido()) return;
    this.guardar.emit({ ...this.valores() });
  }
}
