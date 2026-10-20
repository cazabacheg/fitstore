import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { CONTADOR_MENU, SesionStore, TemaService } from 'shared';
import { hojasDelMenu, menuParaRol, type ItemMenu } from './menu';

/** Menú lateral del backoffice (P-0b-02): ítems por rol (R3), ítem activo, subítems y contadores. */
@Component({
  selector: 'fs-menu-lateral',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './menu-lateral.html',
  styleUrl: './menu-lateral.css',
})
export class MenuLateral {
  private readonly router = inject(Router);
  private readonly contadores = inject(CONTADOR_MENU, { optional: true }) ?? [];
  protected readonly tema = inject(TemaService);

  private readonly sesion = inject(SesionStore);
  protected readonly items = computed(() => menuParaRol(this.sesion.rol()));
  /** Cambia en cada navegación: el menú vuelve a pedir los contadores. */
  private readonly navegaciones = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  );
  private readonly abiertosAMano = signal<Record<string, boolean>>({});

  /** La hoja activa es la de ruta más larga que coincide con la URL (así `/catalogo/productos/carga-masiva` no marca Productos). */
  protected readonly activa = computed(() => {
    const url = this.navegaciones().split(/[?#]/)[0] || '/';
    let mejor: { clave: string; largo: number } | null = null;
    for (const hoja of hojasDelMenu(this.items())) {
      const coincide =
        hoja.ruta === '/' ? url === '/' : url === hoja.ruta || url.startsWith(hoja.ruta + '/');
      if (coincide && (!mejor || hoja.ruta.length > mejor.largo))
        mejor = { clave: hoja.clave, largo: hoja.ruta.length };
    }
    return mejor?.clave ?? null;
  });

  private readonly numeros = computed(() => {
    this.navegaciones();
    const mapa = new Map<string, number>();
    for (const c of this.contadores) mapa.set(c.clave, c.contar());
    return mapa;
  });

  constructor() {
    // Al navegar, los grupos vuelven a abrirse solos según la ruta activa.
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => this.abiertosAMano.set({}));
  }

  protected numero(clave: string, item?: ItemMenu): number {
    const propio = this.numeros().get(clave) ?? 0;
    const hijos = (item?.subitems ?? []).reduce(
      (suma, s) => suma + (this.numeros().get(s.clave) ?? 0),
      0,
    );
    return propio + hijos;
  }

  protected contieneActiva(item: ItemMenu): boolean {
    return (item.subitems ?? []).some((s) => s.clave === this.activa());
  }

  protected abierto(item: ItemMenu): boolean {
    return this.abiertosAMano()[item.clave] ?? this.contieneActiva(item);
  }

  protected alternar(item: ItemMenu): void {
    this.abiertosAMano.update((a) => ({ ...a, [item.clave]: !this.abierto(item) }));
  }
}
