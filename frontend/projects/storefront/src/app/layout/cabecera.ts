import { Component, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { CarritoContador, TemaService } from 'shared';
import type { catalogApi } from 'shared';
import { MenuMovil } from './menu-movil';

/** Máximo de categorías raíz en la barra (CA-0b-20); las demás se ven en la landing y en los filtros. */
const MAX_CATEGORIAS = 4;

/** Cabecera clara de la tienda: logo, categorías, buscador, cuenta y carrito (P-0b-01, TiendaCabecera). */
@Component({
  selector: 'fs-cabecera',
  imports: [RouterLink, RouterLinkActive, MenuMovil],
  template: `
    <header class="fs-cabecera">
      <div class="fs-pagina fs-cabecera__fila">
        <button
          type="button"
          class="fs-solo-movil fs-icono-boton"
          aria-label="Abrir menú"
          [attr.aria-expanded]="menuAbierto()"
          (click)="menuAbierto.set(true)"
        >
          <span class="fs-icono" style="font-size: 24px" aria-hidden="true">menu</span>
        </button>
        <a routerLink="/" class="fs-logo">
          @if (tema.logoClaroUrl(); as logo) {
            <img [src]="logo" [alt]="nombre()" />
          } @else {
            {{ nombre() }}
          }
        </a>
        <nav class="fs-nav fs-solo-escritorio" aria-label="Categorías">
          @for (c of visibles(); track c.id) {
            <a
              [routerLink]="['/categoria', c.slug]"
              routerLinkActive
              ariaCurrentWhenActive="page"
              >{{ c.nombre }}</a
            >
          }
        </nav>
        <div style="flex: 1 1 auto"></div>
        <div class="fs-cabecera__acciones">
          <div class="fs-busqueda" role="search" [class.fs-busqueda--abierta]="buscando()">
            <input
              class="fs-input"
              type="search"
              aria-label="Buscar productos"
              placeholder="Buscar productos"
              (input)="texto.set($any($event.target).value)"
              (keydown.enter)="buscar()"
            />
          </div>
          <button
            type="button"
            class="fs-solo-movil fs-icono-boton"
            aria-label="Buscar"
            [attr.aria-expanded]="buscando()"
            (click)="buscando.set(!buscando())"
          >
            <span class="fs-icono" style="font-size: 22px" aria-hidden="true">search</span>
          </button>
          <a
            class="fs-icono-boton fs-solo-escritorio"
            routerLink="/cuenta/pedidos"
            aria-label="Mi cuenta"
          >
            <span class="fs-icono" style="font-size: 22px" aria-hidden="true">person</span>
          </a>
          <a class="fs-icono-boton" routerLink="/carrito" [attr.aria-label]="etiquetaCarrito()">
            <span class="fs-icono" style="font-size: 22px" aria-hidden="true">shopping_bag</span>
            @if (carrito.cantidad() > 0) {
              <span data-contador class="fs-contador">{{ carrito.cantidad() }}</span>
            }
          </a>
        </div>
      </div>
      @if (menuAbierto()) {
        <fs-menu-movil [categorias]="categorias()" (cerrar)="menuAbierto.set(false)" />
      }
    </header>
  `,
})
export class Cabecera {
  protected readonly tema = inject(TemaService);
  protected readonly carrito = inject(CarritoContador);
  private readonly router = inject(Router);

  readonly categorias = input<catalogApi.CategoriaNodo[]>([]);

  protected readonly menuAbierto = signal(false);
  protected readonly buscando = signal(false);
  protected readonly texto = signal('');

  protected readonly nombre = computed(() => this.tema.nombre() ?? 'FitStore');
  protected readonly visibles = computed(() => this.categorias().slice(0, MAX_CATEGORIAS));
  protected readonly etiquetaCarrito = computed(() => {
    const n = this.carrito.cantidad();
    if (n === 0) return 'Carrito';
    return `Carrito, ${n} ${n === 1 ? 'producto' : 'productos'}`;
  });

  protected buscar(): void {
    const q = this.texto().trim();
    if (!q) return;
    this.buscando.set(false);
    void this.router.navigate(['/productos'], { queryParams: { q } });
  }
}
