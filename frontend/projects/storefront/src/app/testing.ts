import { authApi } from 'shared';

/** Sesiones de prueba de la tienda (no se usan en el código de producción). */
export const SESION_CLIENTE: authApi.Sesion = {
  accessToken: 'token-cliente-de-prueba',
  expiraEn: '2026-10-20T12:15:00Z',
  cuenta: {
    id: '5b1d7e22-9c40-4f1a-b7d2-3a8e6c0f4d55',
    tipo: 'CLIENTE',
    rol: 'CLIENTE',
    email: 'rosa@correo.pe',
    nombres: 'Rosa',
    apellidos: 'Huamán',
  },
};

export const SESION_STAFF: authApi.Sesion = {
  ...SESION_CLIENTE,
  accessToken: 'token-staff-de-prueba',
  cuenta: { ...SESION_CLIENTE.cuenta, tipo: 'STAFF', rol: 'ADMIN', email: 'admin@fitstore.pe' },
};
