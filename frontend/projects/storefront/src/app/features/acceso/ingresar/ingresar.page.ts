import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormularioIngreso } from './formulario-ingreso';
import { ESTILOS_PANTALLA } from '../../estilos-pantalla';

/** Ingresar (P-1-01), pública; si ya hay sesión, `sinSesionGuard` redirige a `/cuenta/perfil`. */
@Component({
  selector: 'fs-ingresar-page',
  imports: [FormularioIngreso, RouterLink],
  styles: [
    ESTILOS_PANTALLA,
    `
      .acceso {
        padding-block: 48px 80px;
        max-width: 440px;
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
      <h1 class="fs-titulo-pagina">Ingresa a tu cuenta</h1>
      <fs-formulario-ingreso [returnUrl]="returnUrl" />
      <p style="margin: var(--espacio-4) 0 0">
        <a routerLink="/recuperar">¿Olvidaste tu contraseña?</a>
      </p>
      <p class="fs-suave" style="margin: var(--espacio-3) 0 0">
        ¿Aún no tienes cuenta?
        <a routerLink="/registro" queryParamsHandling="preserve">Crea tu cuenta</a>
      </p>
      </div>
    </section>
  `,
})
export class IngresarPage {
  protected readonly returnUrl = inject(ActivatedRoute).snapshot.queryParamMap.get('returnUrl');
}
