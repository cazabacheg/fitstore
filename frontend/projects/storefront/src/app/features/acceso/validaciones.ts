/** Reglas de los formularios de la tienda: las mismas que valida auth (micro 4.2 y 4.7). */
export const POLITICA_PASSWORD = /^(?=.*[A-Za-z])(?=.*[0-9]).{8,64}$/;
export const MENSAJE_PASSWORD =
  'La contraseña debe tener de 8 a 64 caracteres, con al menos una letra y un número';

const CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Cada función devuelve el mensaje de error, o `''` si el valor es válido. */
export function errorCorreo(valor: string): string {
  const correo = valor.trim();
  if (!correo) return 'Ingresa tu correo';
  return CORREO.test(correo) && correo.length <= 254 ? '' : 'Ingresa un correo válido';
}

export function errorNombre(valor: string, etiqueta: string): string {
  const n = valor.trim().length;
  if (n === 0) return `Ingresa tus ${etiqueta}`;
  return n >= 2 && n <= 80 ? '' : 'Debe tener entre 2 y 80 caracteres';
}

export function errorDni(valor: string): string {
  return /^[0-9]{8}$/.test(valor.trim()) ? '' : 'El DNI debe tener 8 dígitos';
}

export function errorCelular(valor: string): string {
  return /^9[0-9]{8}$/.test(valor.trim()) ? '' : 'El celular debe tener 9 dígitos y empezar con 9';
}

export function errorPassword(valor: string): string {
  return POLITICA_PASSWORD.test(valor) ? '' : MENSAJE_PASSWORD;
}

/** De 0 (muy débil) a 4 (fuerte): largo, letras y números, 12 caracteres o más, y mayúsculas, minúsculas o símbolos. */
export function nivelFortaleza(valor: string): number {
  let nivel = 0;
  if (valor.length >= 8) nivel++;
  if (/[A-Za-z]/.test(valor) && /[0-9]/.test(valor)) nivel++;
  if (valor.length >= 12) nivel++;
  if ((/[a-z]/.test(valor) && /[A-Z]/.test(valor)) || /[^A-Za-z0-9]/.test(valor)) nivel++;
  return nivel;
}

/**
 * Ruta a la que se vuelve tras ingresar (`?returnUrl=`, la que pone el guard de la 0b). Solo se aceptan rutas de la
 * propia tienda ("/…", no "//…"): cualquier otra cosa vuelve al inicio.
 */
export function rutaDeRetorno(returnUrl: string | null | undefined): string {
  return returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//') ? returnUrl : '/';
}
