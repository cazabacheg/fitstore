import type { ActivatedRouteSnapshot } from '@angular/router';

/** Migas de la ruta activa: el último `data.migas` que se encuentre bajajando por el árbol de rutas. */
export function migasDe(raiz: ActivatedRouteSnapshot): string[] {
  let migas: string[] = [];
  for (let ruta: ActivatedRouteSnapshot | null = raiz; ruta; ruta = ruta.firstChild) {
    const datos = ruta.data['migas'] as string[] | undefined;
    if (datos) migas = datos;
  }
  return migas;
}
