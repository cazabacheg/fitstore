import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormularioRegistro } from './formulario-registro';
import { ESTILOS_PANTALLA } from '../../estilos-pantalla';

/** Registro (P-1-02), pública. */
@Component({
  selector: 'fs-registro-page',
  imports: [FormularioRegistro, RouterLink],
  styles: [
    ESTILOS_PANTALLA,
    `
      .acceso {
        padding-block: 48px 80px;
        max-width: 620px;
      }
      .acceso__tarjeta h1.fs-titulo-pagina {
        margin-bottom: 20px;
      }
      @media (max-width: 720px) {
        .acceso {
          padding-block: 24px 48px;
        }
      }
    `,
  ],
  template: `
    <section class="fs-pagina acceso">
      <div class="fs-tarjeta acceso__tarjeta">
      <h1 class="fs-titulo-pagina">Crea tu cuenta</h1>
      <fs-formulario-registro [returnUrl]="returnUrl" />
      <p class="fs-suave" style="margin: var(--espacio-4) 0 0; text-align: center">
        ¿Ya tienes cuenta?
        <a routerLink="/ingresar" queryParamsHandling="preserve">Ingresa</a>
      </p>
      </div>
    </section>
  `,
})
export class RegistroPage {
  protected readonly returnUrl = inject(ActivatedRoute).snapshot.queryParamMap.get('returnUrl');
}
