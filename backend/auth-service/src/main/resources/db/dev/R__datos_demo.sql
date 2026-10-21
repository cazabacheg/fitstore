-- Datos de demostración de auth (SPEC-1 sección 6). Solo se aplica con los perfiles dev y demo (db/dev).
-- Los UUID son los de docs/demo/ids-demo.md: los clientes son los dueños de los pedidos de demostración de la spec 4.
-- Es repetible e idempotente: si una cuenta o dirección ya existe no se toca.
--
-- Contraseña común de DESARROLLO de todas las cuentas: FitStoreDemo2026. Es de demostración y pública (está en el
-- README del servicio), no es un secreto real. En la nube (perfil demo) DatosDemoNube la reemplaza por DEMO_PASSWORD
-- o desactiva las cuentas. El ADMIN no se siembra: lo crea AdminInicial desde variables de entorno.

INSERT INTO cliente (id, email, password_hash, nombres, apellidos, dni, telefono, activo,
                     credenciales_actualizadas_en, creado_en, actualizado_en, version)
VALUES ('35a11d4f-054a-44a5-8ac4-cf0ed4020f8c', 'lucia.mendoza@fitstore.demo', '$2a$10$w7ld0Xz.lYUKeVGAx122Ee5NMUnj/qGMCGE/w6UjvJqTdizhKF3AG',
        'Lucía', 'Mendoza Quispe', '70123401', '987000001', true, now(), now(), now(), 0)
ON CONFLICT DO NOTHING;

INSERT INTO cliente (id, email, password_hash, nombres, apellidos, dni, telefono, activo,
                     credenciales_actualizadas_en, creado_en, actualizado_en, version)
VALUES ('41f4b1c9-00e0-4816-91b9-a1a143a69df9', 'carlos.huaman@fitstore.demo', '$2a$10$w7ld0Xz.lYUKeVGAx122Ee5NMUnj/qGMCGE/w6UjvJqTdizhKF3AG',
        'Carlos', 'Huamán Torres', '70123402', '987000002', true, now(), now(), now(), 0)
ON CONFLICT DO NOTHING;

INSERT INTO cliente (id, email, password_hash, nombres, apellidos, dni, telefono, activo,
                     credenciales_actualizadas_en, creado_en, actualizado_en, version)
VALUES ('a3d50456-6d33-4e6e-80fc-62563fc46808', 'rosa.ccori@fitstore.demo', '$2a$10$w7ld0Xz.lYUKeVGAx122Ee5NMUnj/qGMCGE/w6UjvJqTdizhKF3AG',
        'Rosa', 'Ccori Vargas', '70123403', '987000003', true, now(), now(), now(), 0)
ON CONFLICT DO NOTHING;

INSERT INTO cliente (id, email, password_hash, nombres, apellidos, dni, telefono, activo,
                     credenciales_actualizadas_en, creado_en, actualizado_en, version)
VALUES ('652dcca9-9c40-434a-9d2d-33e44d1dcc16', 'miguel.paredes@fitstore.demo', '$2a$10$w7ld0Xz.lYUKeVGAx122Ee5NMUnj/qGMCGE/w6UjvJqTdizhKF3AG',
        'Miguel', 'Paredes Soto', '70123404', '987000004', true, now(), now(), now(), 0)
ON CONFLICT DO NOTHING;

-- Dos consentimientos por cliente, con la versión 1 de cada política
INSERT INTO consentimiento (cliente_id, tipo, version_politica, aceptado_en, ip)
SELECT c.id, t.tipo, '1', now(), '127.0.0.1'
FROM cliente c
CROSS JOIN (VALUES ('TERMINOS'), ('PRIVACIDAD')) AS t (tipo)
WHERE c.id IN ('35a11d4f-054a-44a5-8ac4-cf0ed4020f8c', '41f4b1c9-00e0-4816-91b9-a1a143a69df9',
               'a3d50456-6d33-4e6e-80fc-62563fc46808', '652dcca9-9c40-434a-9d2d-33e44d1dcc16')
  AND NOT EXISTS (SELECT 1 FROM consentimiento x WHERE x.cliente_id = c.id AND x.tipo = t.tipo);

INSERT INTO direccion (cliente_id, alias, ubigeo, direccion, referencia, predeterminada, creado_en, actualizado_en)
SELECT '35a11d4f-054a-44a5-8ac4-cf0ed4020f8c', 'Casa', '150122', 'Av. Larco 345, dpto 501, Miraflores', 'Frente al parque Kennedy', true, now(), now()
WHERE EXISTS (SELECT 1 FROM cliente WHERE id = '35a11d4f-054a-44a5-8ac4-cf0ed4020f8c')
  AND NOT EXISTS (SELECT 1 FROM direccion WHERE cliente_id = '35a11d4f-054a-44a5-8ac4-cf0ed4020f8c' AND alias = 'Casa');

INSERT INTO direccion (cliente_id, alias, ubigeo, direccion, referencia, predeterminada, creado_en, actualizado_en)
SELECT '41f4b1c9-00e0-4816-91b9-a1a143a69df9', 'Casa', '150101', 'Jr. Cusco 456, Cercado de Lima', 'Cerca a la plaza Italia', true, now(), now()
WHERE EXISTS (SELECT 1 FROM cliente WHERE id = '41f4b1c9-00e0-4816-91b9-a1a143a69df9')
  AND NOT EXISTS (SELECT 1 FROM direccion WHERE cliente_id = '41f4b1c9-00e0-4816-91b9-a1a143a69df9' AND alias = 'Casa');

INSERT INTO direccion (cliente_id, alias, ubigeo, direccion, referencia, predeterminada, creado_en, actualizado_en)
SELECT '41f4b1c9-00e0-4816-91b9-a1a143a69df9', 'Trabajo', '070101', 'Av. Sáenz Peña 780, Callao', 'Al costado del banco', false, now(), now()
WHERE EXISTS (SELECT 1 FROM cliente WHERE id = '41f4b1c9-00e0-4816-91b9-a1a143a69df9')
  AND NOT EXISTS (SELECT 1 FROM direccion WHERE cliente_id = '41f4b1c9-00e0-4816-91b9-a1a143a69df9' AND alias = 'Trabajo');

INSERT INTO direccion (cliente_id, alias, ubigeo, direccion, referencia, predeterminada, creado_en, actualizado_en)
SELECT 'a3d50456-6d33-4e6e-80fc-62563fc46808', 'Casa', '070101', 'Av. Colonial 1234, Callao', 'Portón verde', true, now(), now()
WHERE EXISTS (SELECT 1 FROM cliente WHERE id = 'a3d50456-6d33-4e6e-80fc-62563fc46808')
  AND NOT EXISTS (SELECT 1 FROM direccion WHERE cliente_id = 'a3d50456-6d33-4e6e-80fc-62563fc46808' AND alias = 'Casa');

INSERT INTO direccion (cliente_id, alias, ubigeo, direccion, referencia, predeterminada, creado_en, actualizado_en)
SELECT '652dcca9-9c40-434a-9d2d-33e44d1dcc16', 'Casa', '040101', 'Calle Mercaderes 210, Arequipa', 'Segundo piso', true, now(), now()
WHERE EXISTS (SELECT 1 FROM cliente WHERE id = '652dcca9-9c40-434a-9d2d-33e44d1dcc16')
  AND NOT EXISTS (SELECT 1 FROM direccion WHERE cliente_id = '652dcca9-9c40-434a-9d2d-33e44d1dcc16' AND alias = 'Casa');

-- Un OPERADOR de demostración
INSERT INTO staff (id, email, password_hash, nombres, apellidos, rol, activo,
                   credenciales_actualizadas_en, creado_en, actualizado_en, version)
VALUES ('5e738102-a490-469f-b0f6-5f1fc2dbf70c', 'diana.flores@fitstore.demo', '$2a$10$w7ld0Xz.lYUKeVGAx122Ee5NMUnj/qGMCGE/w6UjvJqTdizhKF3AG',
        'Diana', 'Flores Rivas', 'OPERADOR', true, now(), now(), now(), 0)
ON CONFLICT DO NOTHING;
