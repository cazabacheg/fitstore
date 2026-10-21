-- Datos de demostración de delivery (perfiles dev y demo, SPEC-6 sección 6). Idempotente: se puede repetir.
-- Los pedidos son los de demostración compartidos con order (docs/demo/ids-demo.md); los repartidores usan ids 101-103.

INSERT INTO repartidor (id, nombres, telefono, vehiculo, placa, activo, creado_en, actualizado_en)
OVERRIDING SYSTEM VALUE
VALUES
    (101, 'Juan Pérez Soto',   '987654321', 'MOTO',      'ABC-123', true,  now(), now()),
    (102, 'Rosa Quispe Ramos', '912345678', 'BICICLETA', NULL,      true,  now(), now()),
    (103, 'Luis Castro Vega',  '956781234', 'AUTO',      'XYZ-987', false, now(), now())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('repartidor', 'id'), GREATEST((SELECT max(id) FROM repartidor), 1000));

-- FS-000101: delivery local en ruta (Miraflores), asignada hoy a Juan
-- FS-000102: delivery local entregada ayer (Cercado de Lima), de Rosa
-- FS-000103: agencia enviada con código de seguimiento (Arequipa), por Shalom
-- FS-000104: delivery local pendiente de asignar (Callao)
INSERT INTO entrega (id, pedido_id, codigo_pedido, modalidad, estado, dest_nombres, dest_apellidos, dest_dni, dest_telefono,
    dest_email, dir_ubigeo, dir_departamento, dir_provincia, dir_distrito, dir_direccion, dir_referencia, repartidor_id,
    fecha_programada, intentos, agencia_id, codigo_seguimiento, clave_recojo, resumen_items, bultos, creado_en, actualizado_en)
VALUES
    ('0b7a9d52-6c1e-4f55-8a10-0a1b2c3d4101', '3e13f37f-db57-44c2-8ac1-66d7ce378e0b', 'FS-000101', 'DELIVERY_LOCAL', 'EN_RUTA',
     'Lucía', 'Mendoza', '45678912', '955111222', 'lucia.mendoza@demo.fitstore.pe', '150122', 'Lima', 'Lima', 'Miraflores',
     'Av. Larco 345 dpto 201', 'Frente al parque Kennedy', 101, current_date, 1, NULL, NULL, NULL,
     'Legging Pro Compresión M Negro × 2 · Top Flex S Blanco × 1', 2, now() - interval '1 day', now()),
    ('0b7a9d52-6c1e-4f55-8a10-0a1b2c3d4102', '33900490-eace-44ab-a6b3-391a28202b57', 'FS-000102', 'DELIVERY_LOCAL', 'ENTREGADA',
     'Carlos', 'Huamán', '40123456', '966222333', 'carlos.huaman@demo.fitstore.pe', '150101', 'Lima', 'Lima', 'Lima',
     'Jr. Cusco 456', NULL, 102, current_date - 1, 1, NULL, NULL, NULL,
     'Short Running M Negro × 1', 1, now() - interval '3 days', now() - interval '1 day'),
    ('0b7a9d52-6c1e-4f55-8a10-0a1b2c3d4103', '5928f8f8-ed74-413e-91f4-d3a6115f6afc', 'FS-000103', 'AGENCIA', 'ENVIADO_AGENCIA',
     'Miguel', 'Paredes', '41234567', '977333444', 'miguel.paredes@demo.fitstore.pe', '040101', 'Arequipa', 'Arequipa',
     'Arequipa', 'Calle Mercaderes 120', NULL, NULL, NULL, 0,
     (SELECT id FROM agencia WHERE nombre = 'Shalom'), 'SH-778899', '4821',
     'Polo Dry Fit L Azul × 3', 1, now() - interval '2 days', now() - interval '1 day'),
    ('0b7a9d52-6c1e-4f55-8a10-0a1b2c3d4104', 'b7655951-8c1f-4f51-9a90-39534aa106d9', 'FS-000104', 'DELIVERY_LOCAL',
     'PENDIENTE_ASIGNACION', 'Rosa', 'Ccori', '42345678', '988444555', 'rosa.ccori@demo.fitstore.pe', '070101', 'Callao',
     'Callao', 'Callao', 'Av. Sáenz Peña 789', 'Al costado de la farmacia', NULL, NULL, 0, NULL, NULL, NULL,
     'Top Flex S Blanco × 2', 1, now(), now())
ON CONFLICT (pedido_id) DO NOTHING;

INSERT INTO entrega_evento (entrega_id, estado, observacion, actor_id, actor_nombre, intento, creado_en)
SELECT v.entrega_id::uuid, v.estado, NULL, v.actor_id::uuid, v.actor_nombre, v.intento, v.creado_en
FROM (VALUES
    ('0b7a9d52-6c1e-4f55-8a10-0a1b2c3d4101', 'PENDIENTE_ASIGNACION', NULL,                                   NULL,            0, now() - interval '1 day'),
    ('0b7a9d52-6c1e-4f55-8a10-0a1b2c3d4101', 'ASIGNADA',             '5e738102-a490-469f-b0f6-5f1fc2dbf70c', 'Operador demo', 1, now() - interval '3 hours'),
    ('0b7a9d52-6c1e-4f55-8a10-0a1b2c3d4101', 'EN_RUTA',              '5e738102-a490-469f-b0f6-5f1fc2dbf70c', 'Operador demo', 1, now() - interval '1 hour'),
    ('0b7a9d52-6c1e-4f55-8a10-0a1b2c3d4102', 'PENDIENTE_ASIGNACION', NULL,                                   NULL,            0, now() - interval '3 days'),
    ('0b7a9d52-6c1e-4f55-8a10-0a1b2c3d4102', 'ASIGNADA',             '5e738102-a490-469f-b0f6-5f1fc2dbf70c', 'Operador demo', 1, now() - interval '2 days'),
    ('0b7a9d52-6c1e-4f55-8a10-0a1b2c3d4102', 'EN_RUTA',              '5e738102-a490-469f-b0f6-5f1fc2dbf70c', 'Operador demo', 1, now() - interval '30 hours'),
    ('0b7a9d52-6c1e-4f55-8a10-0a1b2c3d4102', 'ENTREGADA',            '5e738102-a490-469f-b0f6-5f1fc2dbf70c', 'Operador demo', 1, now() - interval '1 day'),
    ('0b7a9d52-6c1e-4f55-8a10-0a1b2c3d4103', 'PENDIENTE_ENVIO',      NULL,                                   NULL,            0, now() - interval '2 days'),
    ('0b7a9d52-6c1e-4f55-8a10-0a1b2c3d4103', 'ENVIADO_AGENCIA',      '5e738102-a490-469f-b0f6-5f1fc2dbf70c', 'Operador demo', 0, now() - interval '1 day'),
    ('0b7a9d52-6c1e-4f55-8a10-0a1b2c3d4104', 'PENDIENTE_ASIGNACION', NULL,                                   NULL,            0, now())
) AS v(entrega_id, estado, actor_id, actor_nombre, intento, creado_en)
WHERE NOT EXISTS (SELECT 1 FROM entrega_evento e WHERE e.entrega_id = v.entrega_id::uuid);
