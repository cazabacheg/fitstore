/** Fecha y hora para las tablas del backoffice, o "—" si no hay. */
export function fechaHora(iso: string | null | undefined): string {
  return iso
    ? new Date(iso).toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' })
    : '—';
}

export const ETIQUETA_ROL: Record<string, string> = { ADMIN: 'Admin', OPERADOR: 'Operador' };

export const ETIQUETA_ESTADO_STAFF: Record<string, string> = {
  ACTIVO: 'Activo',
  INACTIVO: 'Inactivo',
  INVITACION_PENDIENTE: 'Invitación pendiente',
};
