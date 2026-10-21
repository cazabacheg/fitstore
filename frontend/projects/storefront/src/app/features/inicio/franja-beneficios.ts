import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import type { settingsApi } from 'shared';

/** Franja de beneficios de la landing (de 0 a 3). Sin beneficios no se dibuja nada, ni siquiera el espacio. */
@Component({
  selector: 'fs-franja-beneficios',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (beneficios().length > 0) {
      <section class="fs-pagina franja-contenedor" data-testid="franja-beneficios">
        <ul class="franja">
          @for (b of beneficios(); track $index) {
            <li class="franja__item" data-testid="beneficio">
              @if (esNombreDeIcono(b.icono)) {
                <span class="fs-icono franja__icono" aria-hidden="true">{{ b.icono }}</span>
              } @else {
                <span class="franja__icono" aria-hidden="true">{{ b.icono }}</span>
              }
              <span class="franja__texto">{{ b.texto }}</span>
            </li>
          }
        </ul>
      </section>
    }
  `,
  styles: `
    .franja-contenedor {
      padding-block: 40px 16px;
    }
    .franja {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px;
      margin: 0;
      padding: 16px 24px;
      list-style: none;
      background: var(--fs-color-primario-suave);
      border-radius: var(--fs-radio-md, 8px);
    }
    .franja__item {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .franja__icono {
      color: var(--fs-color-primario-texto);
      font-size: 22px;
    }
    .franja__texto {
      font-weight: 500;
    }
  `,
})
export class FranjaBeneficios {
  readonly beneficios = input<settingsApi.Beneficio[]>([]);

  /** "local_shipping" es un icono por nombre; un emoji se muestra tal cual. */
  protected esNombreDeIcono(icono: string): boolean {
    return /^[a-z][a-z0-9_]*$/.test(icono);
  }
}
