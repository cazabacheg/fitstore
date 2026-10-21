import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormularioIngreso } from './formulario-ingreso';

/** Ingresar (P-1-01), pública; si ya hay sesión, `sinSesionGuard` redirige a `/cuenta/perfil`. */
@Component({
  selector: 'fs-ingresar-page',
  imports: [FormularioIngreso, RouterLink],
  template: `
    <section class="fs-pagina" style="padding-block: 48px; max-width: 440px">
      <h1 style="margin: 0 0 var(--espacio-5)">Ingresa a tu cuenta</h1>
      <fs-formulario-ingreso [returnUrl]="returnUrl" />
      <p style="margin: var(--espacio-4) 0 0">
        <a routerLink="/recuperar">¿Olvidaste tu contraseña?</a>
      </p>
      <p class="fs-suave" style="margin: var(--espacio-3) 0 0">
        ¿Aún no tienes cuenta?
        <a routerLink="/registro" queryParamsHandling="preserve">Crea tu cuenta</a>
      </p>
    </section>
  `,
})
export class IngresarPage {
  protected readonly returnUrl = inject(ActivatedRoute).snapshot.queryParamMap.get('returnUrl');
}
