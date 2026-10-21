import { ChangeDetectionStrategy, Component } from '@angular/core';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { ListaColores } from './lista-colores';
import { ListaTallas } from './lista-tallas';

/** P-2-06 (primera mitad): tallas y colores, listas editables en el sitio (R11 no aplica). */
@Component({
  selector: 'fs-maestros-page',
  imports: [EncabezadoPagina, ListaTallas, ListaColores],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: grid;
      gap: var(--espacio-5);
    }
    .columnas {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: var(--espacio-6);
    }
    @media (max-width: 800px) {
      .columnas {
        grid-template-columns: minmax(0, 1fr);
      }
    }
  `,
  template: `
    <fs-encabezado-pagina titulo="Tallas y colores" />
    <div class="columnas">
      <fs-lista-tallas />
      <fs-lista-colores />
    </div>
  `,
})
export class MaestrosPage {}
