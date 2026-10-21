/** Reglas de los formularios del backoffice: las mismas que valida auth (micro 4.7). */
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
  if (n === 0) return `Ingresa ${etiqueta}`;
  return n >= 2 && n <= 80 ? '' : 'Debe tener entre 2 y 80 caracteres';
}

export function errorPassword(valor: string): string {
  return POLITICA_PASSWORD.test(valor) ? '' : MENSAJE_PASSWORD;
}

/** Ruta a la que se vuelve tras ingresar (`?returnUrl=`): solo rutas del propio backoffice; si no, el dashboard. */
export function rutaDeRetorno(returnUrl: string | null | undefined): string {
  return returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//') ? returnUrl : '/';
}
