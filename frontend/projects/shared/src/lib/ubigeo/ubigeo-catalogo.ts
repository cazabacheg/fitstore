import { Injectable, inject } from '@angular/core';
import { Observable, map, of, shareReplay, tap } from 'rxjs';
import {
  UbigeoService,
  type DistritoEncontrado,
  type UbigeoItem,
  type UbigeoNombre,
} from '../api/settings-service';

/** Clave de localStorage con las listas de ubigeo ya consultadas (caché en el navegador, CA-0b-26). */
export const CLAVE_CACHE_UBIGEO = 'fs_ubigeo_v1';

/**
 * Listas de ubigeo (departamento › provincia › distrito) con caché en memoria y en el navegador.
 * Los demás servicios solo guardan el código; aquí se obtienen los nombres (settings 4.4).
 */
@Injectable({ providedIn: 'root' })
export class UbigeoCatalogo {
  private readonly api = inject(UbigeoService);
  private readonly guardado: Record<string, unknown> = this.leer();
  private readonly enCurso = new Map<string, Observable<unknown>>();

  departamentos(): Observable<UbigeoItem[]> {
    return this.consultar('departamentos', () => this.api.listarDepartamentos());
  }

  provincias(codigoDepartamento: string): Observable<UbigeoItem[]> {
    return this.consultar(`provincias:${codigoDepartamento}`, () =>
      this.api.listarProvincias(codigoDepartamento),
    );
  }

  distritos(codigoProvincia: string): Observable<UbigeoItem[]> {
    return this.consultar(`distritos:${codigoProvincia}`, () =>
      this.api.listarDistritos(codigoProvincia),
    );
  }

  /** Buscador por nombre (sin importar tildes), máximo 20 resultados. */
  buscarDistritos(texto: string): Observable<DistritoEncontrado[]> {
    return this.consultar(`buscar:${texto.trim().toLowerCase()}`, () =>
      this.api.buscarDistritos(texto.trim()),
    );
  }

  /** Nombre completo de un código de 2, 4 o 6 dígitos ("Miraflores, Lima, Lima"). */
  nombre(codigo: string): Observable<UbigeoNombre> {
    return this.consultar(`nombre:${codigo}`, () => this.api.obtenerUbigeo(codigo));
  }

  private consultar<T>(clave: string, pedir: () => Observable<{ data?: T }>): Observable<T> {
    if (clave in this.guardado) return of(this.guardado[clave] as T);
    let pendiente = this.enCurso.get(clave) as Observable<T> | undefined;
    if (!pendiente) {
      pendiente = pedir().pipe(
        map((respuesta) => respuesta.data as T),
        tap({
          next: (datos) => {
            this.guardado[clave] = datos;
            this.enCurso.delete(clave);
            this.escribir();
          },
          error: () => this.enCurso.delete(clave), // un error no se guarda: la próxima consulta reintenta
        }),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
      this.enCurso.set(clave, pendiente);
    }
    return pendiente;
  }

  private leer(): Record<string, unknown> {
    try {
      const texto = localStorage.getItem(CLAVE_CACHE_UBIGEO);
      const datos: unknown = texto ? JSON.parse(texto) : {};
      return typeof datos === 'object' && datos !== null && !Array.isArray(datos)
        ? (datos as Record<string, unknown>)
        : {};
    } catch {
      return {};
    }
  }

  private escribir(): void {
    try {
      localStorage.setItem(CLAVE_CACHE_UBIGEO, JSON.stringify(this.guardado));
    } catch {
      // Sin almacenamiento (modo privado o cuota llena): queda la caché en memoria.
    }
  }
}
