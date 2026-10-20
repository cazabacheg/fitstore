import { HttpErrorResponse } from '@angular/common/http';
import type { ApiError, ErrorCampo } from '../models';

/** Devuelve el cuerpo del error si tiene el formato `ApiError` (K5), o `null`. */
export function comoApiError(error: unknown): ApiError | null {
  if (!(error instanceof HttpErrorResponse)) return null;
  const cuerpo: unknown = error.error;
  if (typeof cuerpo !== 'object' || cuerpo === null) return null;
  const candidato = cuerpo as Partial<ApiError>;
  return candidato.success === false &&
    typeof candidato.mensaje === 'string' &&
    candidato.mensaje !== ''
    ? (candidato as ApiError)
    : null;
}

/** Errores por campo de un 400 de validación, para mostrarlos en el formulario. Vacío si no hay. */
export function camposConError(error: unknown): ErrorCampo[] {
  return comoApiError(error)?.errores ?? [];
}

/** Rutas del cliente que viven en la API. Las demás peticiones (assets, fuentes…) no pasan por los interceptores. */
export function esPeticionApi(url: string): boolean {
  return url.startsWith('/api/');
}
