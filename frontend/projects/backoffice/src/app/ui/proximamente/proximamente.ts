import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';

/** Marcador de cada ruta de R2 hasta que su spec dueña la implemente (CA-0b-15). */
@Component({
  selector: 'fs-proximamente',
  imports: [EncabezadoPagina],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (titulo) {
      <fs-encabezado-pagina [titulo]="titulo" />
    }
    <section class="fs-estado">
      <span class="fs-icono" aria-hidden="true">construction</span>
      <h2 class="fs-estado__titulo">Próximamente</h2>
      <p class="fs-suave">Esta pantalla estará disponible muy pronto.</p>
    </section>
  `,
})
export class Proximamente {
  protected readonly titulo = (inject(ActivatedRoute).snapshot.data['titulo'] as string | undefined) ?? '';
}
