import { HttpContext } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { SIN_TOAST, catalogApi, settingsApi } from 'shared';
import { TiendaService } from 'shared/api/catalog-service';
import { ConfiguracionPublicaService } from 'shared/api/settings-service';
import { CarruselHero } from './carrusel-hero';
import { CategoriasRaiz } from './categorias-raiz';
import { Destacados } from './destacados';
import { FranjaBeneficios } from './franja-beneficios';
import { TiendaFisica } from './tienda-fisica';

/**
 * Landing de la tienda (P-3-01, `/`): hero con carrusel, beneficios, categorías raíz, destacados y tienda física.
 * La marca y los textos vienen de `/settings/public`, así que cambian sin recompilar (RNF-17). Si el catálogo no
 * responde, las categorías y los destacados se ocultan y el resto de la página se ve.
 */
@Component({
  selector: 'fs-inicio',
  imports: [CarruselHero, FranjaBeneficios, CategoriasRaiz, Destacados, TiendaFisica],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (config(); as c) {
      <fs-carrusel-hero
        [imagenes]="c.contenido.imagenesLanding"
        [titulo]="c.contenido.heroTitulo"
        [subtitulo]="c.contenido.heroSubtitulo"
        [ctaTexto]="c.contenido.heroCtaTexto"
        [ctaEnlace]="c.contenido.heroCtaEnlace"
      />
      <fs-franja-beneficios [beneficios]="c.contenido.beneficios" />
      <fs-categorias-raiz [categorias]="categorias()" />
      <fs-destacados [productos]="destacados()" />
      <fs-tienda-fisica
        [tienda]="c.contenido.tiendaFisica"
        [horario]="c.contenido.horario"
        [whatsapp]="c.contenido.whatsapp"
      />
    } @else if (falloLaConfiguracion()) {
      <section class="fs-pagina estado" data-testid="error-landing">
        <h1>No pudimos cargar la tienda</h1>
        <p>Inténtalo de nuevo en un momento.</p>
      </section>
      <fs-categorias-raiz [categorias]="categorias()" />
      <fs-destacados [productos]="destacados()" />
    } @else {
      <div class="fs-esqueleto cargando" data-testid="cargando" aria-busy="true"></div>
    }
  `,
  styles: `
    .cargando {
      min-height: 420px;
    }
    .estado {
      padding-block: 96px;
      text-align: center;
    }
  `,
})
export class InicioPage {
  private readonly ajustes = inject(ConfiguracionPublicaService);
  private readonly tienda = inject(TiendaService);

  protected readonly config = signal<settingsApi.ConfiguracionPublica | null>(null);
  protected readonly falloLaConfiguracion = signal(false);
  protected readonly categorias = signal<catalogApi.CategoriaNodo[]>([]);
  protected readonly destacados = signal<catalogApi.ProductoTarjeta[]>([]);

  constructor() {
    const sinAviso = new HttpContext().set(SIN_TOAST, true);

    this.ajustes
      .obtenerConfiguracionPublica('body', false, { context: sinAviso })
      .pipe(
        catchError(() => {
          this.falloLaConfiguracion.set(true);
          return of(null);
        }),
        takeUntilDestroyed(),
      )
      .subscribe((respuesta) => {
        if (respuesta?.data) this.config.set(respuesta.data);
      });

    // El catálogo es de otra spec y puede estar caído: sin él, estas secciones simplemente no se muestran
    this.tienda
      .obtenerArbolCategorias('body', false, { context: sinAviso })
      .pipe(
        catchError(() => of({ data: [] })),
        takeUntilDestroyed(),
      )
      .subscribe((respuesta) => this.categorias.set(respuesta.data ?? []));

    this.tienda
      .listarDestacados('body', false, { context: sinAviso })
      .pipe(
        catchError(() => of({ data: [] })),
        takeUntilDestroyed(),
      )
      .subscribe((respuesta) => this.destacados.set(respuesta.data ?? []));
  }
}
