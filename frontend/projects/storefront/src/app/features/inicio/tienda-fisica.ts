import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import type { settingsApi } from 'shared';

/** Bloque "Tienda física": dirección, horario y enlaces a cómo llegar y a WhatsApp. Sin tienda no se dibuja. */
@Component({
  selector: 'fs-tienda-fisica',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (tienda(); as t) {
      <section class="fs-pagina seccion">
        <div class="tienda" data-testid="tienda-fisica">
          <span class="tienda__etiqueta">Tienda física</span>
          <h2 class="tienda__titulo">Visítanos</h2>
          <p class="tienda__texto">
            {{ t.direccion }}
            @if (t.ubigeoNombre) {
              , {{ t.ubigeoNombre }}
            }
          </p>
          @if (t.referencia) {
            <p class="tienda__suave">{{ t.referencia }}</p>
          }
          @if (horario()) {
            <p class="tienda__suave">{{ horario() }}</p>
          }
          <div class="tienda__acciones">
            <a class="fs-btn fs-btn--contorno" [href]="mapa()" target="_blank" rel="noopener"
              >Cómo llegar</a
            >
            @if (enlaceWhatsapp(); as wa) {
              <a class="fs-btn fs-btn--texto" [href]="wa" target="_blank" rel="noopener"
                >Escríbenos por WhatsApp</a
              >
            }
          </div>
        </div>
      </section>
    }
  `,
  styles: `
    .seccion {
      padding-block: 32px 96px;
    }
    .tienda {
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding: 32px;
      background: var(--fs-color-superficie);
      border: 1px solid var(--fs-color-linea);
      border-radius: var(--fs-radio-lg, 12px);
    }
    .tienda__etiqueta {
      font-size: 12px;
      line-height: 16px;
      font-weight: 600;
      letter-spacing: 0.06em;
      text-transform: uppercase;
      color: var(--fs-color-texto-suave);
    }
    .tienda__titulo {
      margin: 0;
      font-size: 22px;
      line-height: 28px;
      font-weight: 600;
    }
    .tienda__texto,
    .tienda__suave {
      margin: 0;
    }
    .tienda__suave {
      color: var(--fs-color-texto-suave);
    }
    .tienda__acciones {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
    }
  `,
})
export class TiendaFisica {
  readonly tienda = input<settingsApi.TiendaFisica | null | undefined>(null);
  readonly horario = input<string | null | undefined>(null);
  readonly whatsapp = input<string | null | undefined>(null);

  protected readonly mapa = computed(() => {
    const t = this.tienda();
    const consulta = t ? `${t.direccion}, ${t.ubigeoNombre ?? ''}` : '';
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(consulta)}`;
  });

  protected readonly enlaceWhatsapp = computed(() => {
    const numero = this.whatsapp();
    return numero ? `https://wa.me/51${numero}` : null;
  });
}
