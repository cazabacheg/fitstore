import type { ParamMap, Params } from '@angular/router';

export const TAMANO_PAGINA = 24;
export const ORDENES = [
  { valor: 'novedad', etiqueta: 'Novedades' },
  { valor: 'precio,asc', etiqueta: 'Precio: menor a mayor' },
  { valor: 'precio,desc', etiqueta: 'Precio: mayor a menor' },
  { valor: 'nombre', etiqueta: 'Nombre' },
] as const;
export type Orden = (typeof ORDENES)[number]['valor'];

/** Filtros del listado (C3): viven en la URL, así se conservan al recargar o compartir el enlace. */
export interface Filtros {
  categoria: string | null;
  q: string;
  tallas: string[];
  colores: number[];
  min: number | null;
  max: number | null;
  disponible: boolean;
  orden: Orden;
  /** Página desde 0 (en la URL va desde 1). */
  pagina: number;
}

const numero = (v: string | null): number | null => {
  if (v === null || v.trim() === '') return null;
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
};

export function leerFiltros(ruta: ParamMap, consulta: ParamMap): Filtros {
  const orden = consulta.get('orden');
  const pagina = numero(consulta.get('pagina'));
  return {
    categoria: ruta.get('slug'),
    q: consulta.get('q') ?? '',
    tallas: consulta.getAll('talla'),
    colores: consulta
      .getAll('color')
      .map(Number)
      .filter((n) => Number.isFinite(n)),
    min: numero(consulta.get('min')),
    max: numero(consulta.get('max')),
    disponible: consulta.get('disponible') === 'true',
    orden: ORDENES.some((o) => o.valor === orden) ? (orden as Orden) : 'novedad',
    pagina: pagina && pagina > 1 ? Math.trunc(pagina) - 1 : 0,
  };
}

/** Parámetros de la URL; lo que está en su valor por defecto no se escribe. */
export function aQuery(f: Filtros): Params {
  return {
    q: f.q || null,
    talla: f.tallas.length ? f.tallas : null,
    color: f.colores.length ? f.colores.map(String) : null,
    min: f.min,
    max: f.max,
    disponible: f.disponible ? 'true' : null,
    orden: f.orden === 'novedad' ? null : f.orden,
    pagina: f.pagina > 0 ? f.pagina + 1 : null,
  };
}

export const hayFiltros = (f: Filtros): boolean =>
  !!f.q ||
  f.tallas.length > 0 ||
  f.colores.length > 0 ||
  f.min !== null ||
  f.max !== null ||
  f.disponible;
