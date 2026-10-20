import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

/** Marcador de las rutas cuya spec dueña aún no las implementó (R2). */
@Component({
  selector: 'fs-proximamente',
  template: `
    <section class="fs-pagina" style="padding-block: 48px">
      <div class="fs-estado">
        <span class="fs-icono" aria-hidden="true">construction</span>
        <h1 class="fs-estado__titulo">Próximamente</h1>
        <p class="fs-suave">{{ seccion }} estará disponible muy pronto.</p>
      </div>
    </section>
  `,
})
export class Proximamente {
  protected readonly seccion =
    (inject(ActivatedRoute).snapshot.data['seccion'] as string) ?? 'Esta sección';
}
