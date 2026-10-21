import { HttpContext } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SIN_TOAST, SesionStore } from 'shared';
import { AutenticacionService } from 'shared/api/auth-service';

/** Marco de "Mi cuenta" (P-1-05): menú lateral con Perfil, Direcciones, Mis pedidos y Cerrar sesión (CA-1-10). */
@Component({
  selector: 'fs-cuenta-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <section class="fs-pagina cuenta">
      <nav class="cuenta__menu" aria-label="Mi cuenta">
        <h2 style="margin: 0 0 var(--espacio-3); font-size: 1rem">Mi cuenta</h2>
        <a routerLink="/cuenta/perfil" routerLinkActive="activo">Perfil</a>
        <a routerLink="/cuenta/direcciones" routerLinkActive="activo">Direcciones</a>
        <a routerLink="/cuenta/pedidos" routerLinkActive="activo">Mis pedidos</a>
      </nav>
      <div class="cuenta__contenido">
        <router-outlet />
        <p style="margin-top: var(--espacio-6)">
          <button type="button" class="fs-btn fs-btn--texto" (click)="cerrarSesion()">
            Cerrar sesión
          </button>
        </p>
      </div>
    </section>
  `,
  styles: `
    .cuenta {
      display: grid;
      grid-template-columns: 220px minmax(0, 1fr);
      gap: var(--espacio-6);
      padding-block: 32px;
    }
    .cuenta__menu {
      display: flex;
      flex-direction: column;
      gap: var(--espacio-2);
    }
    .cuenta__menu a {
      padding: 8px 12px;
      border-radius: var(--radius-sm);
      color: var(--color-texto);
      text-decoration: none;
    }
    .cuenta__menu a.activo {
      background: var(--color-primario-suave);
      font-weight: 600;
    }
    @media (max-width: 720px) {
      .cuenta {
        grid-template-columns: 1fr;
      }
      .cuenta__menu {
        flex-direction: row;
        flex-wrap: wrap;
      }
    }
  `,
})
export class CuentaLayout {
  private readonly api = inject(AutenticacionService);
  private readonly sesion = inject(SesionStore);
  private readonly router = inject(Router);

  /** Revoca el refresh en el servidor y borra la sesión local; aunque el servidor falle, la sesión local se cierra. */
  protected cerrarSesion(): void {
    const terminar = () => {
      this.sesion.cerrarSesion();
      void this.router.navigateByUrl('/');
    };
    this.api
      .cerrarSesion('body', false, { context: new HttpContext().set(SIN_TOAST, true) })
      .subscribe({ next: terminar, error: terminar });
  }
}
