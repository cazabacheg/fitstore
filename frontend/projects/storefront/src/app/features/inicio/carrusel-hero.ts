import { ChangeDetectionStrategy, Component, computed, effect, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import type { settingsApi } from 'shared';

/** Cada cuánto avanza el carrusel (S4). */
export const INTERVALO_CARRUSEL_MS = 6000;

/** Las rutas internas se navegan con el router; las URL https salen como enlace normal. */
export function esRutaInterna(enlace: string): boolean {
  return enlace.startsWith('/');
}

/**
 * Hero de la landing (P-3-01) con el carrusel de imágenes de la tienda.
 * Avanza solo cada 6 s; se pausa al pasar el mouse o al enfocar un control; se maneja con flechas, puntos y teclado
 * (izquierda, derecha, Inicio y Fin); con `prefers-reduced-motion` no avanza solo (S4).
 * Sin imágenes, el hero es un bloque con el color primario y los textos, sin carrusel (CA-3-03).
 */
@Component({
  selector: 'fs-carrusel-hero',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section
      class="hero"
      role="region"
      tabindex="0"
      [class.hero--color]="!hayImagenes()"
      [attr.aria-roledescription]="hayImagenes() ? 'carrusel' : null"
      [attr.aria-label]="hayImagenes() ? 'Imágenes destacadas de la tienda' : null"
      (mouseenter)="sobre.set(true)"
      (mouseleave)="sobre.set(false)"
      (focusin)="enfocado.set(true)"
      (focusout)="enfocado.set(false)"
      (keydown)="alTeclear($event)"
    >
      @for (imagen of imagenes(); track imagen.id; let i = $index) {
        <div
          class="hero__diapositiva"
          data-testid="diapositiva"
          [class.hero__diapositiva--activa]="i === actual()"
          [attr.data-indice]="i"
          [attr.aria-hidden]="i !== actual()"
        >
          @if (imagen.enlace; as enlace) {
            @if (interna(enlace)) {
              <a [routerLink]="enlace" tabindex="-1" class="hero__enlace" aria-hidden="true">
                <img
                  data-testid="imagen-hero"
                  [src]="imagen.url"
                  [alt]="imagen.textoAlternativo ?? ''"
                />
              </a>
            } @else {
              <a [href]="enlace" tabindex="-1" class="hero__enlace" aria-hidden="true">
                <img
                  data-testid="imagen-hero"
                  [src]="imagen.url"
                  [alt]="imagen.textoAlternativo ?? ''"
                />
              </a>
            }
          } @else {
            <img
              data-testid="imagen-hero"
              [src]="imagen.url"
              [alt]="imagen.textoAlternativo ?? ''"
            />
          }
        </div>
      }
      @if (hayImagenes()) {
        <div class="hero__velo"></div>
      }
      <div class="fs-pagina hero__contenido">
        <h1 class="hero__titulo">{{ titulo() }}</h1>
        @if (subtitulo()) {
          <p class="hero__subtitulo">{{ subtitulo() }}</p>
        }
        <div class="hero__acciones">
          @if (ctaTexto() && ctaEnlace()) {
            @if (interna(ctaEnlace()!)) {
              <a
                data-testid="hero-cta"
                class="fs-btn fs-btn--lg"
                [class.fs-btn--primario]="hayImagenes()"
                [class.fs-btn--secundario]="!hayImagenes()"
                [routerLink]="ctaEnlace()"
                >{{ ctaTexto() }}</a
              >
            } @else {
              <a
                data-testid="hero-cta"
                class="fs-btn fs-btn--lg"
                [class.fs-btn--primario]="hayImagenes()"
                [class.fs-btn--secundario]="!hayImagenes()"
                [href]="ctaEnlace()"
                >{{ ctaTexto() }}</a
              >
            }
          }
          @if (variasImagenes()) {
            <div class="hero__controles">
              <button
                type="button"
                class="fs-icono-boton hero__flecha"
                aria-label="Imagen anterior"
                (click)="anterior()"
              >
                <span class="fs-icono" aria-hidden="true">chevron_left</span>
              </button>
              <button
                type="button"
                class="fs-icono-boton hero__flecha"
                aria-label="Imagen siguiente"
                (click)="siguiente()"
              >
                <span class="fs-icono" aria-hidden="true">chevron_right</span>
              </button>
              <div class="hero__puntos">
                @for (imagen of imagenes(); track imagen.id; let i = $index) {
                  <button
                    type="button"
                    data-testid="punto"
                    class="hero__punto"
                    [class.hero__punto--activo]="i === actual()"
                    [attr.aria-label]="'Ir a la imagen ' + (i + 1)"
                    [attr.aria-current]="i === actual() ? 'true' : null"
                    (click)="ir(i)"
                  ></button>
                }
              </div>
              <p class="hero__posicion" data-testid="posicion" aria-live="polite">
                Imagen {{ actual() + 1 }} de {{ imagenes().length }}
              </p>
            </div>
          }
        </div>
      </div>
    </section>
  `,
  styles: `
    .hero {
      position: relative;
      display: flex;
      align-items: center;
      min-height: 520px;
      overflow: hidden;
      background: var(--fs-color-secundario);
      color: var(--fs-color-en-secundario);
    }
    .hero--color {
      background: var(--fs-color-primario);
      color: var(--fs-color-en-primario);
      min-height: 420px;
    }
    .hero__diapositiva {
      position: absolute;
      inset: 0;
      opacity: 0;
      transition: opacity 600ms ease;
    }
    .hero__diapositiva--activa {
      opacity: 1;
    }
    .hero__diapositiva img,
    .hero__enlace {
      display: block;
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .hero__velo {
      position: absolute;
      inset: 0;
      background: linear-gradient(90deg, rgb(0 0 0 / 0.55), rgb(0 0 0 / 0.1) 70%);
    }
    .hero__contenido {
      position: relative;
      width: 100%;
      display: flex;
      flex-direction: column;
      gap: 20px;
      padding-block: 64px;
    }
    .hero__titulo {
      margin: 0;
      max-width: 640px;
      font-size: 64px;
      line-height: 64px;
      font-weight: 700;
      letter-spacing: -0.02em;
      text-wrap: balance;
    }
    .hero__subtitulo {
      margin: 0;
      max-width: 460px;
      font-size: 17px;
      line-height: 26px;
      opacity: 0.9;
    }
    .hero__acciones,
    .hero__controles {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 12px;
    }
    .hero__flecha {
      color: inherit;
      border: 1px solid rgb(255 255 255 / 0.4);
    }
    .hero__puntos {
      display: flex;
      gap: 8px;
      align-items: center;
    }
    .hero__punto {
      width: 28px;
      height: 3px;
      padding: 0;
      border: 0;
      cursor: pointer;
      background: var(--fs-color-en-secundario);
      opacity: 0.35;
    }
    .hero__punto--activo {
      opacity: 1;
    }
    .hero__punto:focus-visible,
    .hero__flecha:focus-visible {
      outline: 2px solid var(--fs-color-en-secundario);
      outline-offset: 3px;
    }
    .hero__posicion {
      position: absolute;
      width: 1px;
      height: 1px;
      margin: 0;
      overflow: hidden;
      clip-path: inset(50%);
    }
    @media (max-width: 720px) {
      .hero {
        min-height: 360px;
      }
      .hero__titulo {
        font-size: 36px;
        line-height: 40px;
      }
      .hero__contenido {
        padding-block: 40px;
      }
    }
    @media (prefers-reduced-motion: reduce) {
      .hero__diapositiva {
        transition: none;
      }
    }
  `,
})
export class CarruselHero {
  readonly imagenes = input<settingsApi.ImagenTienda[]>([]);
  readonly titulo = input.required<string>();
  readonly subtitulo = input<string | null | undefined>(null);
  readonly ctaTexto = input<string | null | undefined>(null);
  readonly ctaEnlace = input<string | null | undefined>(null);

  protected readonly actual = signal(0);
  protected readonly sobre = signal(false);
  protected readonly enfocado = signal(false);
  private readonly movimientoReducido = leerMovimientoReducido();

  protected readonly hayImagenes = computed(() => this.imagenes().length > 0);
  protected readonly variasImagenes = computed(() => this.imagenes().length > 1);
  private readonly avanzaSolo = computed(
    () => this.variasImagenes() && !this.sobre() && !this.enfocado() && !this.movimientoReducido,
  );

  constructor() {
    effect((alLimpiar) => {
      if (!this.avanzaSolo()) return;
      const temporizador = setInterval(() => this.siguiente(), INTERVALO_CARRUSEL_MS);
      alLimpiar(() => clearInterval(temporizador));
    });
    // Si la lista de imágenes se acorta, la actual no puede quedar fuera de rango
    effect(() => {
      if (this.actual() >= this.imagenes().length) this.actual.set(0);
    });
  }

  protected interna(enlace: string): boolean {
    return esRutaInterna(enlace);
  }

  protected siguiente(): void {
    const total = this.imagenes().length;
    if (total > 0) this.actual.update((i) => (i + 1) % total);
  }

  protected anterior(): void {
    const total = this.imagenes().length;
    if (total > 0) this.actual.update((i) => (i - 1 + total) % total);
  }

  protected ir(indice: number): void {
    this.actual.set(indice);
  }

  protected alTeclear(evento: KeyboardEvent): void {
    if (!this.variasImagenes()) return;
    switch (evento.key) {
      case 'ArrowRight':
        this.siguiente();
        break;
      case 'ArrowLeft':
        this.anterior();
        break;
      case 'Home':
        this.ir(0);
        break;
      case 'End':
        this.ir(this.imagenes().length - 1);
        break;
      default:
        return;
    }
    evento.preventDefault();
  }
}

function leerMovimientoReducido(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  );
}
