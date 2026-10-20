import type { authApi } from 'shared';

/** Sesiones de prueba del backoffice (no forman parte de la aplicación). */
type Sesion = authApi.Sesion;

export const SESION_ADMIN: Sesion = {
  accessToken: 'token-admin-de-prueba',
  expiraEn: '2026-10-20T12:15:00Z',
  cuenta: {
    id: '0d2f8c1e-6a39-4d3b-8a53-1f6c2b9e7a10',
    tipo: 'STAFF',
    rol: 'ADMIN',
    email: 'admin@fitstore.pe',
    nombres: 'Carla',
    apellidos: 'Admin',
  },
};

export const SESION_OPERADOR: Sesion = {
  ...SESION_ADMIN,
  accessToken: 'token-operador-de-prueba',
  cuenta: { ...SESION_ADMIN.cuenta, rol: 'OPERADOR', email: 'operador@fitstore.pe', nombres: 'Omar', apellidos: 'Operador' },
};

export const SESION_CLIENTE: Sesion = {
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
