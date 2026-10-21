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
import { SelectorUbigeo } from '../../ui/selector-ubigeo/selector-ubigeo';
import { ImagenesLanding } from './imagenes-landing';

/** Contenido que se guarda con `PUT /admin/settings/contenido` (sin la versión, que agrega la pantalla). */
export type DatosContenido = Omit<settingsApi.ContenidoRequest, 'version'>;

interface BeneficioEditable {
  icono: string;
  texto: string;
}

const MAXIMO_BENEFICIOS = 3;
const ENLACE = /^(\/(?!\/)\S*|https:\/\/\S+)$/;
const CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const WHATSAPP = /^9\d{8}$/;
const HTTPS = /^https:\/\/\S+$/;

const nulo = (valor: string): string | null => (valor.trim().length > 0 ? valor.trim() : null);

/**
 * Pestaña Contenido (HU-3-04): hero, imágenes de la landing, beneficios (de 0 a 3), contacto, redes, horario y tienda
 * física. Valida lo mismo que el servidor; los campos opcionales vacíos se envían como `null`.
 */
@Component({
  selector: 'fs-contenido-tab',
  imports: [SelectorUbigeo, ImagenesLanding],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fs-imagenes-landing
      [imagenes]="contenido().imagenesLanding"
      (cambio)="imagenesCambiaron.emit($event)"
    />

    <form class="contenido" novalidate (submit)="guardarDatos($event)">
      <fieldset>
        <legend>Hero de la landing</legend>
        <div class="fs-campo" [class.fs-campo--error]="!!error('heroTitulo')">
          <label class="fs-campo__etiqueta" for="c-hero-titulo">Título</label>
          <input
            id="c-hero-titulo"
            class="fs-input"
            data-testid="hero-titulo"
            maxlength="80"
            [value]="heroTitulo()"
            [attr.aria-invalid]="error('heroTitulo') ? 'true' : null"
            (input)="heroTitulo.set($any($event.target).value)"
          />
          @if (error('heroTitulo')) {
            <span class="fs-campo__error">{{ error('heroTitulo') }}</span>
          }
        </div>
        <div class="fs-campo">
          <label class="fs-campo__etiqueta" for="c-hero-subtitulo">Subtítulo</label>
          <input
            id="c-hero-subtitulo"
            class="fs-input"
            data-testid="hero-subtitulo"
            maxlength="160"
            [value]="heroSubtitulo()"
            (input)="heroSubtitulo.set($any($event.target).value)"
          />
        </div>
        <div class="contenido__fila">
          <div class="fs-campo">
            <label class="fs-campo__etiqueta" for="c-hero-cta">Texto del botón</label>
            <input
              id="c-hero-cta"
              class="fs-input"
              data-testid="hero-cta-texto"
              maxlength="30"
              [value]="heroCtaTexto()"
              (input)="heroCtaTexto.set($any($event.target).value)"
            />
          </div>
          <div class="fs-campo" [class.fs-campo--error]="!!error('heroCtaEnlace')">
            <label class="fs-campo__etiqueta" for="c-hero-enlace">Enlace del botón</label>
            <input
              id="c-hero-enlace"
              class="fs-input"
              data-testid="hero-cta-enlace"
              placeholder="/productos o https://…"
              [value]="heroCtaEnlace()"
              [attr.aria-invalid]="error('heroCtaEnlace') ? 'true' : null"
              (input)="heroCtaEnlace.set($any($event.target).value)"
            />
            @if (error('heroCtaEnlace')) {
              <span class="fs-campo__error">{{ error('heroCtaEnlace') }}</span>
            }
          </div>
        </div>
      </fieldset>

      <fieldset>
        <legend>Franja de beneficios</legend>
        <p class="fs-suave">
          Opcionales, de 0 a 3. Sin beneficios, la franja no se mostrará en la landing.
        </p>
        @for (b of beneficios(); track $index; let i = $index) {
          <div class="contenido__fila contenido__beneficio" data-testid="beneficio-fila">
            <div class="fs-campo">
              <label class="fs-campo__etiqueta" [for]="'b-icono-' + i">Icono</label>
              <input
                [id]="'b-icono-' + i"
                class="fs-input"
                data-testid="beneficio-icono"
                maxlength="8"
                [value]="b.icono"
                (input)="cambiarBeneficio(i, 'icono', $any($event.target).value)"
              />
            </div>
            <div class="fs-campo" [class.fs-campo--error]="!!errorBeneficio(i)">
              <label class="fs-campo__etiqueta" [for]="'b-texto-' + i"
                >Texto corto ({{ b.texto.length }}/60)</label
              >
              <input
                [id]="'b-texto-' + i"
                class="fs-input"
                data-testid="beneficio-texto"
                [value]="b.texto"
                [attr.aria-invalid]="errorBeneficio(i) ? 'true' : null"
                (input)="cambiarBeneficio(i, 'texto', $any($event.target).value)"
              />
              @if (errorBeneficio(i)) {
                <span class="fs-campo__error">{{ errorBeneficio(i) }}</span>
              }
            </div>
            <button
              type="button"
              class="fs-btn fs-btn--texto fs-btn--sm"
              data-testid="quitar-beneficio"
              [attr.aria-label]="'Quitar el beneficio ' + (i + 1)"
              (click)="quitarBeneficio(i)"
            >
              Quitar
            </button>
          </div>
        }
        @if (beneficios().length === 0) {
          <p class="fs-suave">Sin beneficios: la franja no se mostrará.</p>
        }
        <button
          type="button"
          class="fs-btn fs-btn--contorno fs-btn--sm"
          data-testid="agregar-beneficio"
          [disabled]="beneficios().length >= maximoBeneficios"
          (click)="agregarBeneficio()"
        >
          Agregar beneficio
        </button>
      </fieldset>

      <fieldset>
        <legend>Contacto y redes</legend>
        <div class="contenido__fila">
          <div class="fs-campo" [class.fs-campo--error]="!!error('email')">
            <label class="fs-campo__etiqueta" for="c-email">Correo de contacto</label>
            <input
              id="c-email"
              class="fs-input"
              type="email"
              data-testid="email"
              [value]="email()"
              [attr.aria-invalid]="error('email') ? 'true' : null"
              (input)="email.set($any($event.target).value)"
            />
            @if (error('email')) {
              <span class="fs-campo__error">{{ error('email') }}</span>
            }
          </div>
          <div class="fs-campo">
            <label class="fs-campo__etiqueta" for="c-telefono">Teléfono</label>
            <input
              id="c-telefono"
              class="fs-input"
              data-testid="telefono"
              maxlength="15"
              [value]="telefono()"
              (input)="telefono.set($any($event.target).value)"
            />
          </div>
          <div class="fs-campo" [class.fs-campo--error]="!!error('whatsapp')">
            <label class="fs-campo__etiqueta" for="c-whatsapp">WhatsApp</label>
            <input
              id="c-whatsapp"
              class="fs-input"
              data-testid="whatsapp"
              inputmode="numeric"
              maxlength="9"
              [value]="whatsapp()"
              [attr.aria-invalid]="error('whatsapp') ? 'true' : null"
              (input)="whatsapp.set($any($event.target).value)"
            />
            @if (error('whatsapp')) {
              <span class="fs-campo__error">{{ error('whatsapp') }}</span>
            }
          </div>
        </div>
        <div class="contenido__fila">
          @for (red of redes; track red.id) {
            <div class="fs-campo" [class.fs-campo--error]="!!error(red.campo)">
              <label class="fs-campo__etiqueta" [for]="'c-' + red.id">{{ red.etiqueta }}</label>
              <input
                [id]="'c-' + red.id"
                class="fs-input"
                placeholder="https://…"
                [attr.data-testid]="red.id"
                [value]="valorRed(red.id)"
                [attr.aria-invalid]="error(red.campo) ? 'true' : null"
                (input)="cambiarRed(red.id, $any($event.target).value)"
              />
              @if (error(red.campo)) {
                <span class="fs-campo__error">{{ error(red.campo) }}</span>
              }
            </div>
          }
        </div>
        <div class="fs-campo">
          <label class="fs-campo__etiqueta" for="c-horario">Horario de atención</label>
          <input
            id="c-horario"
            class="fs-input"
            data-testid="horario"
            maxlength="120"
            [value]="horario()"
            (input)="horario.set($any($event.target).value)"
          />
        </div>
      </fieldset>

      <fieldset>
        <legend>Tienda física</legend>
        <p class="fs-suave">Déjala vacía si no tienes tienda a la que pueda ir el cliente.</p>
        <div class="fs-campo" [class.fs-campo--error]="!!error('tiendaDireccion')">
          <label class="fs-campo__etiqueta" for="c-t-direccion">Dirección</label>
          <input
            id="c-t-direccion"
            class="fs-input"
            data-testid="tienda-direccion"
            maxlength="200"
            [value]="tiendaDireccion()"
            [attr.aria-invalid]="error('tiendaDireccion') ? 'true' : null"
            (input)="tiendaDireccion.set($any($event.target).value)"
          />
          @if (error('tiendaDireccion')) {
            <span class="fs-campo__error">{{ error('tiendaDireccion') }}</span>
          }
        </div>
        <fs-selector-ubigeo [(valor)]="tiendaUbigeo" hasta="distrito" />
        @if (error('tiendaUbigeo')) {
          <span class="fs-campo__error">{{ error('tiendaUbigeo') }}</span>
        }
        <div class="fs-campo">
          <label class="fs-campo__etiqueta" for="c-t-referencia">Referencia (opcional)</label>
          <input
            id="c-t-referencia"
            class="fs-input"
            data-testid="tienda-referencia"
            maxlength="200"
            [value]="tiendaReferencia()"
            (input)="tiendaReferencia.set($any($event.target).value)"
          />
        </div>
      </fieldset>

      <div class="contenido__acciones">
        <button
          type="submit"
          class="fs-btn fs-btn--primario"
          data-testid="guardar-contenido"
          [disabled]="guardando()"
        >
          {{ guardando() ? 'Guardando…' : 'Guardar contenido' }}
        </button>
      </div>
    </form>
  `,
  styles: `
    .contenido {
      display: grid;
      gap: 24px;
      max-width: 900px;
      margin-top: 24px;
    }
    .contenido fieldset {
      display: grid;
      gap: 12px;
      margin: 0;
      padding: 16px 20px;
      border: 1px solid var(--fs-color-linea);
      border-radius: 8px;
    }
    .contenido legend {
      padding-inline: 8px;
      font-weight: 600;
    }
    .contenido__fila {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 12px;
      align-items: end;
    }
    .contenido__beneficio {
      grid-template-columns: 100px 1fr auto;
    }
  `,
})
export class ContenidoTab {
  readonly contenido = input.required<settingsApi.Contenido>();
  readonly guardando = input(false);
  readonly erroresServidor = input<Record<string, string>>({});

  readonly guardar = output<DatosContenido>();
  readonly imagenesCambiaron = output<settingsApi.ImagenTienda[]>();
  readonly sinGuardar = output<boolean>();

  protected readonly maximoBeneficios = MAXIMO_BENEFICIOS;
  protected readonly redes = [
    { id: 'instagram', etiqueta: 'Instagram', campo: 'instagramUrl' },
    { id: 'facebook', etiqueta: 'Facebook', campo: 'facebookUrl' },
    { id: 'tiktok', etiqueta: 'TikTok', campo: 'tiktokUrl' },
  ] as const;

  protected readonly heroTitulo = signal('');
  protected readonly heroSubtitulo = signal('');
  protected readonly heroCtaTexto = signal('');
  protected readonly heroCtaEnlace = signal('');
  protected readonly beneficios = signal<BeneficioEditable[]>([]);
  protected readonly email = signal('');
  protected readonly telefono = signal('');
  protected readonly whatsapp = signal('');
  protected readonly instagram = signal('');
  protected readonly facebook = signal('');
  protected readonly tiktok = signal('');
  protected readonly horario = signal('');
  protected readonly tiendaDireccion = signal('');
  protected readonly tiendaUbigeo = signal<string | null>(null);
  protected readonly tiendaReferencia = signal('');
  private readonly intentado = signal(false);

  private readonly errores = computed<Record<string, string>>(() => {
    const e: Record<string, string> = {};
    const titulo = this.heroTitulo().trim();
    if (titulo.length < 2 || titulo.length > 80)
      e['heroTitulo'] = 'El título admite de 2 a 80 caracteres';
    const enlace = this.heroCtaEnlace().trim();
    if (enlace.length > 0 && !ENLACE.test(enlace))
      e['heroCtaEnlace'] = 'Usa una ruta de la tienda (/productos) o una URL https';
    const correo = this.email().trim();
    if (correo.length > 0 && !CORREO.test(correo)) e['email'] = 'Correo inválido';
    const wa = this.whatsapp().trim();
    if (wa.length > 0 && !WHATSAPP.test(wa))
      e['whatsapp'] = 'El WhatsApp debe tener 9 dígitos y empezar con 9';
    for (const red of this.redes) {
      const url = this.valorRed(red.id).trim();
      if (url.length > 0 && !HTTPS.test(url)) e[red.campo] = 'La URL debe empezar con https://';
    }
    const direccion = this.tiendaDireccion().trim();
    if (direccion.length > 0 && this.tiendaUbigeo() === null)
      e['tiendaUbigeo'] = 'Elige el distrito';
    if (direccion.length === 0 && this.tiendaUbigeo() !== null)
      e['tiendaDireccion'] = 'Indica la dirección';
    return e;
  });

  /** Todo menos las imágenes: subir o reordenar una imagen no borra lo que se está escribiendo. */
  private readonly base = computed(
    () => {
      return Object.fromEntries(
        Object.entries(this.contenido()).filter(([clave]) => clave !== 'imagenesLanding'),
      ) as Omit<settingsApi.Contenido, 'imagenesLanding'>;
    },
    { equal: (a, b) => JSON.stringify(a) === JSON.stringify(b) },
  );

  constructor() {
    effect(() => {
      const c = this.base();
      this.heroTitulo.set(c.heroTitulo);
      this.heroSubtitulo.set(c.heroSubtitulo ?? '');
      this.heroCtaTexto.set(c.heroCtaTexto ?? '');
      this.heroCtaEnlace.set(c.heroCtaEnlace ?? '');
      this.beneficios.set(c.beneficios.map((b) => ({ icono: b.icono, texto: b.texto })));
      this.email.set(c.email ?? '');
      this.telefono.set(c.telefono ?? '');
      this.whatsapp.set(c.whatsapp ?? '');
      this.instagram.set(c.instagramUrl ?? '');
      this.facebook.set(c.facebookUrl ?? '');
      this.tiktok.set(c.tiktokUrl ?? '');
      this.horario.set(c.horario ?? '');
      this.tiendaDireccion.set(c.tiendaFisica?.direccion ?? '');
      this.tiendaUbigeo.set(c.tiendaFisica?.ubigeo ?? null);
      this.tiendaReferencia.set(c.tiendaFisica?.referencia ?? '');
      this.intentado.set(false);
    });
    effect(() => {
      const actual = JSON.stringify(this.datos());
      const original = JSON.stringify(this.datosDe(this.base()));
      this.sinGuardar.emit(actual !== original);
    });
  }

  protected error(campo: string): string {
    return this.erroresServidor()[campo] ?? (this.intentado() ? (this.errores()[campo] ?? '') : '');
  }

  protected errorBeneficio(i: number): string {
    if (this.erroresServidor()['beneficios']) return this.erroresServidor()['beneficios'];
    if (!this.intentado()) return '';
    const texto = this.beneficios()[i]?.texto.trim() ?? '';
    if (texto.length === 0) return 'Escribe el texto del beneficio';
    if (texto.length > 60) return 'El texto admite hasta 60 caracteres';
    return '';
  }

  protected valorRed(id: 'instagram' | 'facebook' | 'tiktok'): string {
    return this[id]();
  }

  protected cambiarRed(id: 'instagram' | 'facebook' | 'tiktok', valor: string): void {
    this[id].set(valor);
  }

  protected agregarBeneficio(): void {
    if (this.beneficios().length < MAXIMO_BENEFICIOS)
      this.beneficios.update((l) => [...l, { icono: '', texto: '' }]);
  }

  protected quitarBeneficio(i: number): void {
    this.beneficios.update((l) => l.filter((_, k) => k !== i));
  }

  protected cambiarBeneficio(i: number, campo: 'icono' | 'texto', valor: string): void {
    this.beneficios.update((l) => l.map((b, k) => (k === i ? { ...b, [campo]: valor } : b)));
  }

  protected guardarDatos(evento: Event): void {
    evento.preventDefault();
    this.intentado.set(true);
    const hayErrorEnBeneficios = this.beneficios().some((_, i) => this.errorBeneficio(i) !== '');
    if (Object.keys(this.errores()).length > 0 || hayErrorEnBeneficios) return;
    this.guardar.emit(this.datos());
  }

  private datos(): DatosContenido {
    const direccion = this.tiendaDireccion().trim();
    const ubigeo = this.tiendaUbigeo();
    return {
      heroTitulo: this.heroTitulo().trim(),
      heroSubtitulo: nulo(this.heroSubtitulo()),
      heroCtaTexto: nulo(this.heroCtaTexto()),
      heroCtaEnlace: nulo(this.heroCtaEnlace()),
      beneficios: this.beneficios().map((b) => ({ icono: b.icono.trim(), texto: b.texto.trim() })),
      email: nulo(this.email()),
      telefono: nulo(this.telefono()),
      whatsapp: nulo(this.whatsapp()),
      instagramUrl: nulo(this.instagram()),
      facebookUrl: nulo(this.facebook()),
      tiktokUrl: nulo(this.tiktok()),
      horario: nulo(this.horario()),
      tiendaFisica:
        direccion.length > 0 && ubigeo !== null
          ? { direccion, ubigeo, referencia: nulo(this.tiendaReferencia()) }
          : null,
    };
  }

  private datosDe(c: Omit<settingsApi.Contenido, 'imagenesLanding'>): DatosContenido {
    return {
      heroTitulo: c.heroTitulo,
      heroSubtitulo: nulo(c.heroSubtitulo ?? ''),
      heroCtaTexto: nulo(c.heroCtaTexto ?? ''),
      heroCtaEnlace: nulo(c.heroCtaEnlace ?? ''),
      beneficios: c.beneficios.map((b) => ({ icono: b.icono, texto: b.texto })),
      email: nulo(c.email ?? ''),
      telefono: nulo(c.telefono ?? ''),
      whatsapp: nulo(c.whatsapp ?? ''),
      instagramUrl: nulo(c.instagramUrl ?? ''),
      facebookUrl: nulo(c.facebookUrl ?? ''),
      tiktokUrl: nulo(c.tiktokUrl ?? ''),
      horario: nulo(c.horario ?? ''),
      tiendaFisica: c.tiendaFisica
        ? {
            direccion: c.tiendaFisica.direccion,
            ubigeo: c.tiendaFisica.ubigeo,
            referencia: nulo(c.tiendaFisica.referencia ?? ''),
          }
        : null,
    };
  }
}
