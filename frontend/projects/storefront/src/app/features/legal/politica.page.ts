import { HttpContext, HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, startWith, switchMap } from 'rxjs';
import { SIN_TOAST, settingsApi } from 'shared';
import { PoliticasService } from 'shared/api/settings-service';
import { FsBoton } from '../../ui/boton/boton';
import { ESTILOS_PANTALLA } from '../estilos-pantalla';
import { fechaLarga } from './formato-fecha';
import { renderizarMarkdownSeguro } from './markdown-seguro';

type TipoPolitica = 'terminos' | 'privacidad' | 'cambios-devoluciones' | 'envios';
const TIPOS: readonly string[] = ['terminos', 'privacidad', 'cambios-devoluciones', 'envios'];

type Estado =
  | { tipo: 'cargando' }
  | { tipo: 'ok'; politica: settingsApi.Politica; anterior: boolean }
  | { tipo: 'no-encontrada' }
  | { tipo: 'error' };

/**
 * Política de la tienda (P-3-02, `/politicas/:tipo`): la versión vigente con su número y fecha, o una anterior con
 * `?version=N`. El Markdown se muestra sanitizado (sin HTML ni scripts, S3).
 */
@Component({
  selector: 'fs-politica',
  imports: [FsBoton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article class="fs-pagina politica">
      @switch (estado().tipo) {
        @case ('cargando') {
          <div
            class="fs-esqueleto cargando"
            role="status"
            aria-busy="true"
            aria-label="Cargando"
          ></div>
        }
        @case ('no-encontrada') {
          <div data-testid="politica-no-encontrada" class="mensaje">
            <h1 class="fs-titulo-pagina">No encontramos esta política</h1>
            <p class="fs-suave">
              Revisa el enlace o vuelve a las políticas desde el pie de página.
            </p>
          </div>
        }
        @case ('error') {
          <div class="mensaje" role="alert">
            <h2 class="fs-titulo-seccion">No pudimos cargar la política</h2>
            <p class="fs-suave">Inténtalo de nuevo en un momento.</p>
            <button
              type="button"
              fsBoton
              variante="contorno"
              data-testid="reintentar"
              (click)="reintentar()"
            >
              Reintentar
            </button>
          </div>
        }
        @default {
          @if (vigenteActual(); as p) {
            <p class="politica__version" data-testid="version">
              Versión {{ p.version }} · publicada el {{ fecha(p.publicadaEn) }}
            </p>
            @if (esAnterior()) {
              <p class="politica__aviso" data-testid="aviso-anterior" role="note">
                Estás viendo una versión anterior de esta política.
              </p>
            }
            <div class="politica__texto" [innerHTML]="html()"></div>
          }
        }
      }
    </article>
  `,
  styles: [
    ESTILOS_PANTALLA,
    `
    .politica {
      max-width: 820px;
      padding-block: 48px 96px;
    }
    .politica__version {
      margin: 0 0 16px;
      color: var(--fs-color-texto-suave);
      font-size: 14px;
    }
    .politica__aviso {
      margin: 0 0 24px;
      padding: 12px 16px;
      border-radius: var(--fs-radio-md, 8px);
      background: var(--fs-color-primario-suave);
    }
    /* El HTML viene de [innerHTML]: no lleva el atributo de encapsulación, por eso se acota con ::ng-deep al contenedor. */
    :host ::ng-deep .politica__texto {
      line-height: 1.7;
      color: var(--fs-color-texto);
    }
    :host ::ng-deep .politica__texto :is(h1, h2, h3, h4) {
      line-height: 1.25;
      font-weight: 700;
      margin-block: 2em 0.6em;
    }
    :host ::ng-deep .politica__texto h1 {
      margin-top: 0;
      font-size: 28px;
      line-height: 34px;
      letter-spacing: -0.01em;
    }
    :host ::ng-deep .politica__texto h2 {
      font-size: 20px;
      font-weight: 600;
    }
    :host ::ng-deep .politica__texto h3 {
      font-size: 17px;
      font-weight: 600;
    }
    :host ::ng-deep .politica__texto p {
      margin: 0 0 1em;
    }
    :host ::ng-deep .politica__texto :is(ul, ol) {
      margin: 0 0 1em;
      padding-left: 1.5em;
    }
    :host ::ng-deep .politica__texto ul {
      list-style: disc;
    }
    :host ::ng-deep .politica__texto ol {
      list-style: decimal;
    }
    :host ::ng-deep .politica__texto li {
      margin-block: 0.35em;
    }
    :host ::ng-deep .politica__texto strong {
      font-weight: 700;
    }
    :host ::ng-deep .politica__texto a {
      color: var(--fs-color-primario-texto);
    }
    :host ::ng-deep .politica__texto hr {
      border: 0;
      border-top: 1px solid var(--fs-color-linea);
      margin-block: 2em;
    }
    :host ::ng-deep .politica__texto table {
      border-collapse: collapse;
      margin-block: 1em;
    }
    :host ::ng-deep .politica__texto :is(th, td) {
      border: 1px solid var(--fs-color-linea);
      padding: 8px 12px;
      text-align: left;
    }
    :host ::ng-deep .politica__texto blockquote {
      margin-inline: 0;
      padding: 8px 16px;
      border-left: 4px solid var(--fs-color-primario);
      background: var(--fs-color-superficie-alterna);
    }
    .cargando {
      min-height: 320px;
    }
    .mensaje {
      padding-block: 64px;
      text-align: center;
    }
  `,
  ],
})
export class PoliticaPage {
  private readonly ruta = inject(ActivatedRoute);
  private readonly api = inject(PoliticasService);

  private readonly intento = new BehaviorSubject(0);
  protected readonly estado = signal<Estado>({ tipo: 'cargando' });

  protected readonly vigenteActual = computed(() => {
    const e = this.estado();
    return e.tipo === 'ok' ? e.politica : null;
  });
  protected readonly esAnterior = computed(() => {
    const e = this.estado();
    return e.tipo === 'ok' && e.anterior;
  });
  protected readonly html = computed(() => {
    const p = this.vigenteActual();
    return p ? renderizarMarkdownSeguro(p.contenido) : '';
  });

  constructor() {
    combineLatest([this.ruta.paramMap, this.ruta.queryParamMap, this.intento])
      .pipe(
        switchMap(([params, consulta]) => {
          const tipo = params.get('tipo') ?? '';
          if (!TIPOS.includes(tipo)) return of<Estado>({ tipo: 'no-encontrada' });
          const version = consulta.get('version');
          const numero = version !== null && /^\d+$/.test(version) ? Number(version) : undefined;
          return this.api
            .obtenerPolitica(tipo as TipoPolitica, numero, 'body', false, {
              context: new HttpContext().set(SIN_TOAST, true),
            })
            .pipe(
              map((r): Estado => ({
                tipo: 'ok',
                politica: r.data!,
                anterior: numero !== undefined,
              })),
              catchError((e: unknown) =>
                of<Estado>(
                  e instanceof HttpErrorResponse && e.status === 404
                    ? { tipo: 'no-encontrada' }
                    : { tipo: 'error' },
                ),
              ),
              startWith<Estado>({ tipo: 'cargando' }),
            );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((e) => this.estado.set(e));
  }

  protected reintentar(): void {
    this.intento.next(this.intento.value + 1);
  }

  protected fecha(valor: string): string {
    return fechaLarga(valor);
  }
}
