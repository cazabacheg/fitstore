import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormularioRegistro } from './formulario-registro';

/** Registro (P-1-02), pública. */
@Component({
  selector: 'fs-registro-page',
  imports: [FormularioRegistro, RouterLink],
  template: `
    <section class="fs-pagina" style="padding-block: 48px; max-width: 620px">
      <h1 style="margin: 0 0 var(--espacio-5)">Crea tu cuenta</h1>
      <fs-formulario-registro [returnUrl]="returnUrl" />
      <p class="fs-suave" style="margin: var(--espacio-4) 0 0; text-align: center">
        ¿Ya tienes cuenta?
        <a routerLink="/ingresar" queryParamsHandling="preserve">Ingresa</a>
      </p>
    </section>
  `,
})
export class RegistroPage {
  protected readonly returnUrl = inject(ActivatedRoute).snapshot.queryParamMap.get('returnUrl');
}
