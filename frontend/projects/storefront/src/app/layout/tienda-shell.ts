import { HttpContext } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SIN_TOAST, catalogApi } from 'shared';
import { BannerServicioNoDisponible } from '../ui/banner/banner';
import { FsToast } from '../ui/toast/toast';
import { BarraAnuncio } from './barra-anuncio';
import { Cabecera } from './cabecera';
import { Pie } from './pie';

/**
 * Layout de la tienda (P-0b-01): anuncio, cabecera, contenido y pie.
 * Si `/categorias` falla la cabecera se muestra sin categorías: el shell no se bloquea.
 */
@Component({
  selector: 'fs-tienda-shell',
  imports: [RouterOutlet, BarraAnuncio, BannerServicioNoDisponible, Cabecera, Pie, FsToast],
  host: { style: 'display: block' },
  template: `
    <fs-barra-anuncio />
    <fs-banner-servicio-no-disponible />
    <fs-cabecera [categorias]="categorias()" />
    <main class="fs-contenido" id="contenido"><router-outlet /></main>
    <fs-pie />
    <fs-toast />
  `,
})
export class TiendaShell {
  private readonly api = inject(catalogApi.TiendaService);

  protected readonly categorias = signal<catalogApi.CategoriaNodo[]>([]);

  constructor() {
    const contexto = new HttpContext().set(SIN_TOAST, true);
    this.api.obtenerArbolCategorias('body', false, { context: contexto }).subscribe({
      next: (r) => this.categorias.set(r.data ?? []),
      error: () => this.categorias.set([]),
    });
  }
}
