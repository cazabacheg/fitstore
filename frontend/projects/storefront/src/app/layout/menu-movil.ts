import { A11yModule } from '@angular/cdk/a11y';
import { Component, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import type { catalogApi } from 'shared';

/** Menú lateral (☰) del celular: todas las categorías raíz y la cuenta (CA-0b-21). */
@Component({
  selector: 'fs-menu-movil',
  imports: [A11yModule, RouterLink, RouterLinkActive],
  template: `
    <div
      class="fs-menu-movil"
      role="dialog"
      aria-modal="true"
      aria-label="Menú"
      cdkTrapFocus
      [cdkTrapFocusAutoCapture]="true"
      (keydown.escape)="cerrar.emit()"
    >
      <button
        type="button"
        class="fs-menu-movil__fondo"
        tabindex="-1"
        aria-hidden="true"
        (click)="cerrar.emit()"
      ></button>
      <nav class="fs-menu-movil__panel" aria-label="Menú principal">
        <button
          type="button"
          class="fs-icono-boton"
          aria-label="Cerrar menú"
          (click)="cerrar.emit()"
        >
          <span class="fs-icono" aria-hidden="true" style="font-size: 24px">close</span>
        </button>
        @for (c of categorias(); track c.id) {
          <a
            [routerLink]="['/categoria', c.slug]"
            routerLinkActive
            ariaCurrentWhenActive="page"
            (click)="cerrar.emit()"
          >
            {{ c.nombre }}
          </a>
        }
        <hr class="fs-linea" />
        <a routerLink="/cuenta/pedidos" (click)="cerrar.emit()">Mi cuenta</a>
      </nav>
    </div>
  `,
})
export class MenuMovil {
  readonly categorias = input<catalogApi.CategoriaNodo[]>([]);
  readonly cerrar = output<void>();
}
