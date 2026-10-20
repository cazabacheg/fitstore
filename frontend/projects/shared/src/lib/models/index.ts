/**
 * Tipos comunes de la API, idénticos a `contracts/openapi/comun.yaml` (K4 a K6).
 * Son los que usa el código de `shared` y de las apps; los clientes generados traen los suyos por servicio.
 */

/** Envoltorio de toda respuesta exitosa (K4, ADR-015). */
export interface ApiResponse<T = unknown> {
  success: boolean;
  mensaje: string;
  data?: T;
}

/** Un campo inválido en un 400 de validación. */
export interface ErrorCampo {
  campo: string;
  mensaje: string;
}

/** Respuesta de error (K5). `codigo` es estable y en mayúsculas (`CORREO_DUPLICADO`, `SIN_STOCK`…). */
export interface ApiError {
  success: false;
  mensaje: string;
  codigo: string;
  /** Solo en 400 de validación. */
  errores?: ErrorCampo[];
  requestId?: string;
  /** Datos estructurados según el `codigo` (p. ej. `SIN_STOCK` → `faltantes[]`). */
  detalles?: Record<string, unknown>;
}

/** Página de resultados (K6). Los listados con contadores agregan `contadores` (estado → número). */
export interface Pagina<T> {
  contenido: T[];
  /** Desde 0. */
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
  contadores?: Record<string, number>;
}

/** Parámetros de paginación de la API (K6, ADR-015): `page` desde 0, `size` hasta 100, `sort` como `campo,asc|desc`. */
export interface ConsultaPagina {
  page: number;
  size: number;
  sort?: string[];
}
