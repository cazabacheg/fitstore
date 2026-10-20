import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { derivarColores, type settingsApi } from 'shared';

export type AppPrevia = 'tienda' | 'backoffice';
export type DispositivoPrevia = 'escritorio' | 'movil';

/**
 * Vista previa del tema (RF-SET-07, D5): aplica los colores como variables CSS solo en este recuadro, así que la
 * tienda y el backoffice reales no cambian hasta que se guarda. Se ve como tienda o backoffice, en escritorio o móvil.
 */
@Component({
  selector: 'fs-vista-previa-tema',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="vp"
      data-testid="vista-previa"
      [class.vp--tienda]="app() === 'tienda'"
      [class.vp--backoffice]="app() === 'backoffice'"
      [class.vp--escritorio]="dispositivo() === 'escritorio'"
      [class.vp--movil]="dispositivo() === 'movil'"
      [style.--fs-color-primario]="tema().colorPrimario"
      [style.--fs-color-secundario]="tema().colorSecundario"
      [style.--fs-color-acento]="tema().colorAcento"
      [style.--fs-color-fondo]="tema().colorFondo"
      [style.--fs-color-texto]="tema().colorTexto"
      [style.--fs-color-en-primario]="derivados().enPrimario"
      [style.--fs-color-en-secundario]="derivados().enSecundario"
      [style.--fs-color-en-acento]="derivados().enAcento"
      [style.--fs-color-primario-texto]="derivados().primarioTexto"
      [style.--fs-color-primario-suave]="derivados().primarioSuave"
      [style.--fs-color-superficie-alterna]="derivados().superficieAlterna"
      [style.--fs-color-texto-suave]="derivados().textoSuave"
      [style.--fs-color-linea]="derivados().linea"
      aria-label="Vista previa del tema"
      role="img"
    >
      @if (app() === 'tienda') {
        <div class="vp__cabecera">
          <strong>{{ nombre() }}</strong>
          <span class="vp__enlaces">Mujer · Hombre · Ofertas</span>
        </div>
        <div class="vp__hero">
          <p class="vp__titulo">Muévete con estilo</p>
          <span class="vp__boton">Ver novedades</span>
        </div>
        <div class="vp__tarjetas">
          <div class="vp__tarjeta">
            <span class="vp__imagen"></span><span>Legging Pro</span><strong>S/ 59.90</strong>
          </div>
          <div class="vp__tarjeta">
            <span class="vp__imagen"></span><span>Top Flex</span><strong>S/ 39.90</strong>
          </div>
        </div>
      } @else {
        <div class="vp__menu">
          <strong>{{ nombre() }}</strong
          ><span>Pedidos</span><span>Catálogo</span>
        </div>
        <div class="vp__panel">
          <p class="vp__titulo vp__titulo--panel">Pedidos</p>
          <span class="vp__boton">Nuevo</span>
          <div class="vp__fila"><span>FS-000124</span><span class="vp__insignia">Pagado</span></div>
          <div class="vp__fila">
            <span>FS-000125</span><span class="vp__insignia vp__insignia--acento">En camino</span>
          </div>
        </div>
      }
    </div>
  `,
  styles: `
    .vp {
      font-size: 13px;
      background: var(--fs-color-fondo);
      color: var(--fs-color-texto);
      border: 1px solid var(--fs-color-linea);
      border-radius: 8px;
      overflow: hidden;
      margin-inline: auto;
    }
    .vp--escritorio {
      max-width: 560px;
    }
    .vp--movil {
      max-width: 260px;
    }
    .vp__cabecera,
    .vp__menu {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      padding: 10px 14px;
      background: var(--fs-color-secundario);
      color: var(--fs-color-en-secundario);
    }
    .vp--movil .vp__enlaces {
      display: none;
    }
    .vp__hero {
      display: grid;
      gap: 10px;
      justify-items: start;
      padding: 22px 14px;
      background: var(--fs-color-primario);
      color: var(--fs-color-en-primario);
    }
    .vp__titulo {
      margin: 0;
      font-size: 20px;
      font-weight: 700;
    }
    .vp__titulo--panel {
      font-size: 16px;
      color: var(--fs-color-texto);
    }
    .vp__boton {
      display: inline-block;
      padding: 6px 12px;
      border-radius: 6px;
      background: var(--fs-color-acento);
      color: var(--fs-color-en-acento);
      font-weight: 600;
    }
    .vp__tarjetas {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 10px;
      padding: 14px;
    }
    .vp__tarjeta {
      display: grid;
      gap: 4px;
    }
    .vp__imagen {
      height: 54px;
      border-radius: 6px;
      background: var(--fs-color-superficie-alterna);
    }
    .vp__menu {
      justify-content: flex-start;
    }
    .vp__panel {
      display: grid;
      gap: 8px;
      padding: 14px;
    }
    .vp__fila {
      display: flex;
      justify-content: space-between;
      padding: 6px 0;
      border-top: 1px solid var(--fs-color-linea);
    }
    .vp__insignia {
      padding: 1px 8px;
      border-radius: 999px;
      background: var(--fs-color-primario-suave);
      color: var(--fs-color-primario-texto);
    }
    .vp__insignia--acento {
      background: var(--fs-color-acento);
      color: var(--fs-color-en-acento);
    }
  `,
})
export class VistaPreviaTema {
  readonly tema = input.required<settingsApi.Tema>();
  readonly nombre = input('Tu tienda');
  readonly app = input<AppPrevia>('tienda');
  readonly dispositivo = input<DispositivoPrevia>('escritorio');

  protected readonly derivados = computed(() => {
    try {
      return derivarColores(this.tema());
    } catch {
      // mientras se escribe un color incompleto se conservan los últimos derivados válidos
      return this.ultimos;
    }
  });
  private ultimos = derivarColores({
    colorPrimario: '#C9B28C',
    colorSecundario: '#2B2621',
    colorAcento: '#4E5B45',
    colorFondo: '#FAF9F7',
    colorTexto: '#26221D',
  });
}
