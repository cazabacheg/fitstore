import type { authApi } from '../../public-api';

/** Datos de prueba compartidos por las pruebas de `shared` (no forman parte de la API pública). */
type Sesion = authApi.Sesion;

export const SESION_STAFF: Sesion = {
  accessToken: 'token-staff-de-prueba',
  expiraEn: '2026-10-20T12:15:00Z',
  cuenta: {
    id: '0d2f8c1e-6a39-4d3b-8a53-1f6c2b9e7a10',
    tipo: 'STAFF',
    rol: 'ADMIN',
    email: 'admin@fitstore.pe',
    nombres: 'Lucía',
    apellidos: 'Quispe',
  },
};

export const SESION_OPERADOR: Sesion = {
  ...SESION_STAFF,
  accessToken: 'token-operador-de-prueba',
  cuenta: { ...SESION_STAFF.cuenta, rol: 'OPERADOR', email: 'operador@fitstore.pe' },
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

export const SESION_NUEVA: Sesion = { ...SESION_STAFF, accessToken: 'token-renovado' };
