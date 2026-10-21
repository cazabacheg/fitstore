-- Datos de demostración de order (SPEC-4 sección 6, perfiles dev y demo, SPEC-8 I7): 11 pedidos de los 4 clientes de
-- demostración, en todos los estados que usa la demo, con su historial. Repetible e idempotente.
--
-- Los ids de los clientes (auth) y los SKU de las variantes (catalog) son los de docs/demo/ids-demo.md:
--   Lucía Mendoza 35a11d4f… · Carlos Huamán 41f4b1c9… · Rosa Ccori a3d50456… · Miguel Paredes 652dcca9…
--   operadora Diana Flores 5e738102…
--   variantes: 10002 LEG01-NEG-M · 10014 LEG02-NEG-S · 10037 JOG01-NEG-L · 10052 SHO01-NEG-M
-- Los pedidos son una copia congelada: no dependen de que catalog o auth tengan esos datos cargados.

-- ---------- Pedidos ----------
-- (codigo, id, cliente, estado, modalidad, destinatario, dirección, envío, montos, expiración, pago, creación)

-- 1. Por expirar en 10 minutos: la demo del cronómetro y de "Pagar ahora" (Lucía, delivery en Miraflores)
INSERT INTO pedido (id, codigo, cliente_id, estado, modalidad, dest_nombres, dest_apellidos, dest_dni, dest_telefono,
                    dest_email, dir_ubigeo, dir_departamento, dir_provincia, dir_distrito, dir_direccion,
                    dir_referencia, plazo_dias_min, plazo_dias_max, subtotal, descuento_total, costo_envio, total,
                    expira_en, idempotency_key, creado_en, actualizado_en, version)
VALUES ('a0000000-0000-4000-8000-000000000105', 'FS-000105', '35a11d4f-054a-44a5-8ac4-cf0ed4020f8c',
        'PENDIENTE_PAGO', 'DELIVERY_LOCAL', 'Lucía', 'Mendoza Quispe', '70123401', '987000001',
        'lucia.mendoza@fitstore.demo', '150122', 'Lima', 'Lima', 'Miraflores', 'Av. Larco 345, dpto 501, Miraflores',
        'Frente al parque Kennedy', 1, 2, 189.70, 0.00, 10.00, 199.70,
        now() + interval '10 minutes', 'demo-pedido-1115', now() - interval '20 minutes', now() - interval '20 minutes', 0)
ON CONFLICT (id) DO NOTHING;

-- 2. Pagado, esperando que el operador lo prepare (Carlos, delivery en el Cercado)
INSERT INTO pedido (id, codigo, cliente_id, estado, modalidad, dest_nombres, dest_apellidos, dest_dni, dest_telefono,
                    dest_email, dir_ubigeo, dir_departamento, dir_provincia, dir_distrito, dir_direccion,
                    dir_referencia, plazo_dias_min, plazo_dias_max, subtotal, descuento_total, costo_envio, total,
                    expira_en, boleta_url, pago_proveedor, pago_medio, pagado_en, idempotency_key, creado_en,
                    actualizado_en, version)
VALUES ('a0000000-0000-4000-8000-000000000106', 'FS-000106', '41f4b1c9-00e0-4816-91b9-a1a143a69df9',
        'PAGADO', 'DELIVERY_LOCAL', 'Carlos', 'Huamán Torres', '70123402', '987000002',
        'carlos.huaman@fitstore.demo', '150101', 'Lima', 'Lima', 'Lima', 'Jr. Cusco 456, Cercado de Lima',
        'Cerca a la plaza Italia', 1, 2, 149.70, 0.00, 8.00, 157.70,
        now() - interval '3 hours', '/api/v1/pagos/FS-000106/boleta', 'SIMULADO', 'TARJETA',
        now() - interval '175 minutes', 'demo-pedido-1116', now() - interval '3 hours 30 minutes',
        now() - interval '175 minutes', 0)
ON CONFLICT (id) DO NOTHING;

-- 3. En preparación, con descuento por volumen (Rosa, delivery en el Callao)
INSERT INTO pedido (id, codigo, cliente_id, estado, modalidad, dest_nombres, dest_apellidos, dest_dni, dest_telefono,
                    dest_email, dir_ubigeo, dir_departamento, dir_provincia, dir_distrito, dir_direccion,
                    dir_referencia, plazo_dias_min, plazo_dias_max, subtotal, descuento_total, costo_envio, total,
                    expira_en, boleta_url, pago_proveedor, pago_medio, pagado_en, idempotency_key, creado_en,
                    actualizado_en, version)
VALUES ('a0000000-0000-4000-8000-000000000107', 'FS-000107', 'a3d50456-6d33-4e6e-80fc-62563fc46808',
        'EN_PREPARACION', 'DELIVERY_LOCAL', 'Rosa', 'Ccori Vargas', '70123403', '987000003',
        'rosa.ccori@fitstore.demo', '070101', 'Callao', 'Callao', 'Callao', 'Av. Colonial 1234, Callao',
        'Portón verde', 1, 2, 179.70, 8.99, 8.00, 178.71,
        now() - interval '1 day', '/api/v1/pagos/FS-000107/boleta', 'SIMULADO', 'TARJETA',
        now() - interval '23 hours', 'demo-pedido-1117', now() - interval '1 day 1 hour', now() - interval '20 hours', 0)
ON CONFLICT (id) DO NOTHING;

-- 4. Listo para despacho: delivery lo recoge (Rosa, entrega en el Callao)
INSERT INTO pedido (id, codigo, cliente_id, estado, modalidad, dest_nombres, dest_apellidos, dest_dni, dest_telefono,
                    dest_email, dir_ubigeo, dir_departamento, dir_provincia, dir_distrito, dir_direccion,
                    dir_referencia, plazo_dias_min, plazo_dias_max, subtotal, descuento_total, costo_envio, total,
                    expira_en, boleta_url, pago_proveedor, pago_medio, pagado_en, idempotency_key, creado_en,
                    actualizado_en, version)
VALUES ('b7655951-8c1f-4f51-9a90-39534aa106d9', 'FS-000104', 'a3d50456-6d33-4e6e-80fc-62563fc46808',
        'LISTO_DESPACHO', 'DELIVERY_LOCAL', 'Rosa', 'Ccori Vargas', '70123403', '987000003',
        'rosa.ccori@fitstore.demo', '070101', 'Callao', 'Callao', 'Callao', 'Av. Colonial 1234, Callao',
        'Portón verde', 1, 2, 139.80, 0.00, 8.00, 147.80,
        now() - interval '2 days', '/api/v1/pagos/FS-000104/boleta', 'SIMULADO', 'TARJETA',
        now() - interval '47 hours', 'demo-pedido-1114', now() - interval '2 days 1 hour', now() - interval '5 hours', 0)
ON CONFLICT (id) DO NOTHING;

-- 5. Recojo en tienda, listo para recoger (Lucía)
INSERT INTO pedido (id, codigo, cliente_id, estado, modalidad, dest_nombres, dest_apellidos, dest_dni, dest_telefono,
                    dest_email, subtotal, descuento_total, costo_envio, total, expira_en, boleta_url, pago_proveedor,
                    pago_medio, pagado_en, idempotency_key, creado_en, actualizado_en, version)
VALUES ('a0000000-0000-4000-8000-000000000108', 'FS-000108', '35a11d4f-054a-44a5-8ac4-cf0ed4020f8c',
        'LISTO_RECOJO', 'RECOJO', 'Lucía', 'Mendoza Quispe', '70123401', '987000001',
        'lucia.mendoza@fitstore.demo', 79.90, 0.00, 0.00, 79.90,
        now() - interval '3 days', '/api/v1/pagos/FS-000108/boleta', 'SIMULADO', 'TARJETA',
        now() - interval '71 hours', 'demo-pedido-1118', now() - interval '3 days 1 hour', now() - interval '1 day', 0)
ON CONFLICT (id) DO NOTHING;

-- 6. Por agencia, ya enviado con código de seguimiento (Miguel, Arequipa por Shalom)
INSERT INTO pedido (id, codigo, cliente_id, estado, modalidad, dest_nombres, dest_apellidos, dest_dni, dest_telefono,
                    dest_email, dir_ubigeo, dir_departamento, dir_provincia, dir_distrito, dir_direccion,
                    dir_referencia, agencia_id, agencia_nombre, plazo_dias_min, plazo_dias_max, codigo_seguimiento,
                    subtotal, descuento_total, costo_envio, total, expira_en, boleta_url, pago_proveedor, pago_medio,
                    pagado_en, idempotency_key, creado_en, actualizado_en, version)
VALUES ('5928f8f8-ed74-413e-91f4-d3a6115f6afc', 'FS-000103', '652dcca9-9c40-434a-9d2d-33e44d1dcc16',
        'ENVIADO', 'AGENCIA', 'Miguel', 'Paredes Soto', '70123404', '987000004',
        'miguel.paredes@fitstore.demo', '040101', 'Arequipa', 'Arequipa', 'Arequipa', 'Calle Mercaderes 210, Arequipa',
        'Segundo piso', 1, 'Shalom', 2, 3, 'SH-2026-48121', 94.80, 0.00, 18.00, 112.80,
        now() - interval '4 days', '/api/v1/pagos/FS-000103/boleta', 'SIMULADO', 'TARJETA',
        now() - interval '95 hours', 'demo-pedido-1113', now() - interval '4 days 1 hour', now() - interval '2 days', 0)
ON CONFLICT (id) DO NOTHING;

-- 101. Enviado por delivery local; la entrega ya existe en delivery (Lucía, Miraflores)
INSERT INTO pedido (id, codigo, cliente_id, estado, modalidad, dest_nombres, dest_apellidos, dest_dni, dest_telefono,
                    dest_email, dir_ubigeo, dir_departamento, dir_provincia, dir_distrito, dir_direccion,
                    dir_referencia, plazo_dias_min, plazo_dias_max, subtotal, descuento_total, costo_envio, total,
                    expira_en, boleta_url, pago_proveedor, pago_medio, pagado_en, idempotency_key, creado_en,
                    actualizado_en, version)
VALUES ('3e13f37f-db57-44c2-8ac1-66d7ce378e0b', 'FS-000101', '35a11d4f-054a-44a5-8ac4-cf0ed4020f8c',
        'ENVIADO', 'DELIVERY_LOCAL', 'Lucía', 'Mendoza Quispe', '70123401', '987000001',
        'lucia.mendoza@fitstore.demo', '150122', 'Lima', 'Lima', 'Miraflores', 'Av. Larco 345, dpto 501, Miraflores',
        'Frente al parque Kennedy', 1, 2, 94.80, 0.00, 10.00, 104.80,
        now() - interval '5 days', '/api/v1/pagos/FS-000101/boleta', 'SIMULADO', 'TARJETA',
        now() - interval '5 days', 'demo-pedido-1111', now() - interval '5 days 1 hour', now() - interval '2 days', 0)
ON CONFLICT (id) DO NOTHING;

-- 7. Entregado hace unos días (Carlos, Cercado de Lima)
INSERT INTO pedido (id, codigo, cliente_id, estado, modalidad, dest_nombres, dest_apellidos, dest_dni, dest_telefono,
                    dest_email, dir_ubigeo, dir_departamento, dir_provincia, dir_distrito, dir_direccion,
                    dir_referencia, plazo_dias_min, plazo_dias_max, subtotal, descuento_total, costo_envio, total,
                    expira_en, boleta_url, pago_proveedor, pago_medio, pagado_en, idempotency_key, creado_en,
                    actualizado_en, version)
VALUES ('33900490-eace-44ab-a6b3-391a28202b57', 'FS-000102', '41f4b1c9-00e0-4816-91b9-a1a143a69df9',
        'ENTREGADO', 'DELIVERY_LOCAL', 'Carlos', 'Huamán Torres', '70123402', '987000002',
        'carlos.huaman@fitstore.demo', '150101', 'Lima', 'Lima', 'Lima', 'Jr. Cusco 456, Cercado de Lima',
        'Cerca a la plaza Italia', 1, 2, 94.80, 0.00, 8.00, 102.80,
        now() - interval '9 days', '/api/v1/pagos/FS-000102/boleta', 'SIMULADO', 'TARJETA',
        now() - interval '9 days', 'demo-pedido-1112', now() - interval '9 days 1 hour', now() - interval '7 days', 0)
ON CONFLICT (id) DO NOTHING;

-- 8. Entregado, con un reembolso postventa parcial (Lucía devolvió el legging por talla)
INSERT INTO pedido (id, codigo, cliente_id, estado, modalidad, dest_nombres, dest_apellidos, dest_dni, dest_telefono,
                    dest_email, dir_ubigeo, dir_departamento, dir_provincia, dir_distrito, dir_direccion,
                    dir_referencia, plazo_dias_min, plazo_dias_max, subtotal, descuento_total, costo_envio, total,
                    monto_reembolsado, expira_en, boleta_url, pago_proveedor, pago_medio, pagado_en, idempotency_key,
                    creado_en, actualizado_en, version)
VALUES ('a0000000-0000-4000-8000-000000000109', 'FS-000109', '35a11d4f-054a-44a5-8ac4-cf0ed4020f8c',
        'ENTREGADO', 'DELIVERY_LOCAL', 'Lucía', 'Mendoza Quispe', '70123401', '987000001',
        'lucia.mendoza@fitstore.demo', '150122', 'Lima', 'Lima', 'Miraflores', 'Av. Larco 345, dpto 501, Miraflores',
        'Frente al parque Kennedy', 1, 2, 149.80, 0.00, 10.00, 159.80, 69.90,
        now() - interval '12 days', '/api/v1/pagos/FS-000109/boleta', 'SIMULADO', 'TARJETA',
        now() - interval '12 days', 'demo-pedido-1119', now() - interval '12 days 1 hour', now() - interval '2 days', 0)
ON CONFLICT (id) DO NOTHING;

-- 9. Cancelado después de pagar: payment lo reembolsó (Carlos, recojo)
INSERT INTO pedido (id, codigo, cliente_id, estado, modalidad, dest_nombres, dest_apellidos, dest_dni, dest_telefono,
                    dest_email, subtotal, descuento_total, costo_envio, total, expira_en, pago_proveedor, pago_medio,
                    pagado_en, idempotency_key, creado_en, actualizado_en, version)
VALUES ('a0000000-0000-4000-8000-000000000110', 'FS-000110', '41f4b1c9-00e0-4816-91b9-a1a143a69df9',
        'CANCELADO', 'RECOJO', 'Carlos', 'Huamán Torres', '70123402', '987000002',
        'carlos.huaman@fitstore.demo', 34.90, 0.00, 0.00, 34.90,
        now() - interval '6 days', 'SIMULADO', 'TARJETA', now() - interval '6 days',
        'demo-pedido-110', now() - interval '6 days 1 hour', now() - interval '5 days', 0)
ON CONFLICT (id) DO NOTHING;

-- 10. Expirado: nadie pagó a tiempo (Miguel, por agencia)
INSERT INTO pedido (id, codigo, cliente_id, estado, modalidad, dest_nombres, dest_apellidos, dest_dni, dest_telefono,
                    dest_email, dir_ubigeo, dir_departamento, dir_provincia, dir_distrito, dir_direccion,
                    dir_referencia, agencia_id, agencia_nombre, plazo_dias_min, plazo_dias_max, subtotal,
                    descuento_total, costo_envio, total, expira_en, idempotency_key, creado_en, actualizado_en, version)
VALUES ('a0000000-0000-4000-8000-000000000111', 'FS-000111', '652dcca9-9c40-434a-9d2d-33e44d1dcc16',
        'EXPIRADO', 'AGENCIA', 'Miguel', 'Paredes Soto', '70123404', '987000004',
        'miguel.paredes@fitstore.demo', '040101', 'Arequipa', 'Arequipa', 'Arequipa', 'Calle Mercaderes 210, Arequipa',
        'Segundo piso', 1, 'Shalom', 2, 3, 59.90, 0.00, 18.00, 77.90,
        now() - interval '1 day 23 hours', 'demo-pedido-111', now() - interval '2 days 1 hour',
        now() - interval '1 day 23 hours', 0)
ON CONFLICT (id) DO NOTHING;

-- ---------- Ítems (copia congelada de catalog) ----------
INSERT INTO pedido_item (pedido_id, variante_id, producto_id, sku, producto_nombre, talla, color, imagen_url,
                         precio_unitario, cantidad, descuento_porcentaje, descuento_monto, total)
SELECT p.id, i.variante_id, i.producto_id, i.sku, i.nombre, i.talla, i.color,
       'http://fitstore-media.web.localhost:3902/productos/' || i.producto_id || '/demo-negro',
       i.precio, i.cantidad, i.pct, i.descuento, i.precio * i.cantidad - i.descuento
FROM (VALUES
    ('FS-000105', 10002, 1001, 'LEG01-NEG-M', 'Legging Pro Compresión', 'M', 'negro', 59.90, 2, 0.00, 0.00),
    ('FS-000105', 10014, 1002, 'LEG02-NEG-S', 'Legging Seamless Tiro Alto', 'S', 'negro', 69.90, 1, 0.00, 0.00),
    ('FS-000106', 10037, 1004, 'JOG01-NEG-L', 'Jogger Algodón Premium', 'L', 'negro', 79.90, 1, 0.00, 0.00),
    ('FS-000106', 10052, 1006, 'SHO01-NEG-M', 'Short Running Ligero', 'M', 'negro', 34.90, 2, 0.00, 0.00),
    ('FS-000107', 10002, 1001, 'LEG01-NEG-M', 'Legging Pro Compresión', 'M', 'negro', 59.90, 3, 5.00, 8.99),
    ('FS-000104', 10014, 1002, 'LEG02-NEG-S', 'Legging Seamless Tiro Alto', 'S', 'negro', 69.90, 2, 0.00, 0.00),
    ('FS-000108', 10037, 1004, 'JOG01-NEG-L', 'Jogger Algodón Premium', 'L', 'negro', 79.90, 1, 0.00, 0.00),
    ('FS-000103', 10052, 1006, 'SHO01-NEG-M', 'Short Running Ligero', 'M', 'negro', 34.90, 1, 0.00, 0.00),
    ('FS-000103', 10002, 1001, 'LEG01-NEG-M', 'Legging Pro Compresión', 'M', 'negro', 59.90, 1, 0.00, 0.00),
    ('FS-000101', 10002, 1001, 'LEG01-NEG-M', 'Legging Pro Compresión', 'M', 'negro', 59.90, 1, 0.00, 0.00),
    ('FS-000101', 10052, 1006, 'SHO01-NEG-M', 'Short Running Ligero', 'M', 'negro', 34.90, 1, 0.00, 0.00),
    ('FS-000102', 10002, 1001, 'LEG01-NEG-M', 'Legging Pro Compresión', 'M', 'negro', 59.90, 1, 0.00, 0.00),
    ('FS-000102', 10052, 1006, 'SHO01-NEG-M', 'Short Running Ligero', 'M', 'negro', 34.90, 1, 0.00, 0.00),
    ('FS-000109', 10014, 1002, 'LEG02-NEG-S', 'Legging Seamless Tiro Alto', 'S', 'negro', 69.90, 1, 0.00, 0.00),
    ('FS-000109', 10037, 1004, 'JOG01-NEG-L', 'Jogger Algodón Premium', 'L', 'negro', 79.90, 1, 0.00, 0.00),
    ('FS-000110', 10052, 1006, 'SHO01-NEG-M', 'Short Running Ligero', 'M', 'negro', 34.90, 1, 0.00, 0.00),
    ('FS-000111', 10002, 1001, 'LEG01-NEG-M', 'Legging Pro Compresión', 'M', 'negro', 59.90, 1, 0.00, 0.00)
) AS i(codigo, variante_id, producto_id, sku, nombre, talla, color, precio, cantidad, pct, descuento)
JOIN pedido p ON p.codigo = i.codigo
WHERE NOT EXISTS (SELECT 1 FROM pedido_item x WHERE x.pedido_id = p.id);

-- ---------- Historial ----------
-- actor: C = el cliente, S = el sistema (pago, delivery), O = la operadora Diana Flores
INSERT INTO pedido_historial (pedido_id, estado_anterior, estado_nuevo, actor_tipo, actor_id, actor_nombre, comentario,
                              creado_en)
SELECT p.id, h.anterior, h.nuevo,
       CASE h.actor WHEN 'C' THEN 'CLIENTE' WHEN 'O' THEN 'STAFF' ELSE 'SISTEMA' END,
       CASE h.actor WHEN 'C' THEN p.cliente_id WHEN 'O' THEN '5e738102-a490-469f-b0f6-5f1fc2dbf70c'::uuid END,
       CASE h.actor WHEN 'O' THEN 'Diana Flores Rivas' END,
       h.comentario, now() - h.hace
FROM (VALUES
    ('FS-000105', NULL, 'PENDIENTE_PAGO', 'C', 'Pedido creado', interval '20 minutes'),
    ('FS-000105', 'PENDIENTE_PAGO', 'PENDIENTE_PAGO', 'S', 'Pago rechazado: Fondos insuficientes', interval '12 minutes'),
    ('FS-000106', NULL, 'PENDIENTE_PAGO', 'C', 'Pedido creado', interval '3 hours 30 minutes'),
    ('FS-000106', 'PENDIENTE_PAGO', 'PAGADO', 'S', 'Pago confirmado', interval '175 minutes'),
    ('FS-000107', NULL, 'PENDIENTE_PAGO', 'C', 'Pedido creado', interval '1 day 1 hour'),
    ('FS-000107', 'PENDIENTE_PAGO', 'PAGADO', 'S', 'Pago confirmado', interval '23 hours'),
    ('FS-000107', 'PAGADO', 'EN_PREPARACION', 'O', NULL, interval '20 hours'),
    ('FS-000104', NULL, 'PENDIENTE_PAGO', 'C', 'Pedido creado', interval '2 days 1 hour'),
    ('FS-000104', 'PENDIENTE_PAGO', 'PAGADO', 'S', 'Pago confirmado', interval '47 hours'),
    ('FS-000104', 'PAGADO', 'EN_PREPARACION', 'O', NULL, interval '30 hours'),
    ('FS-000104', 'EN_PREPARACION', 'LISTO_DESPACHO', 'O', '1 bulto', interval '5 hours'),
    ('FS-000108', NULL, 'PENDIENTE_PAGO', 'C', 'Pedido creado', interval '3 days 1 hour'),
    ('FS-000108', 'PENDIENTE_PAGO', 'PAGADO', 'S', 'Pago confirmado', interval '71 hours'),
    ('FS-000108', 'PAGADO', 'EN_PREPARACION', 'O', NULL, interval '2 days'),
    ('FS-000108', 'EN_PREPARACION', 'LISTO_RECOJO', 'O', NULL, interval '1 day'),
    ('FS-000103', NULL, 'PENDIENTE_PAGO', 'C', 'Pedido creado', interval '4 days 1 hour'),
    ('FS-000103', 'PENDIENTE_PAGO', 'PAGADO', 'S', 'Pago confirmado', interval '95 hours'),
    ('FS-000103', 'PAGADO', 'EN_PREPARACION', 'O', NULL, interval '3 days 12 hours'),
    ('FS-000103', 'EN_PREPARACION', 'LISTO_DESPACHO', 'O', NULL, interval '3 days'),
    ('FS-000103', 'LISTO_DESPACHO', 'ENVIADO', 'S', 'Enviado por Shalom, código SH-2026-48121', interval '2 days'),
    ('FS-000101', NULL, 'PENDIENTE_PAGO', 'C', 'Pedido creado', interval '5 days 1 hour'),
    ('FS-000101', 'PENDIENTE_PAGO', 'PAGADO', 'S', 'Pago confirmado', interval '5 days'),
    ('FS-000101', 'PAGADO', 'EN_PREPARACION', 'O', NULL, interval '4 days 20 hours'),
    ('FS-000101', 'EN_PREPARACION', 'LISTO_DESPACHO', 'O', NULL, interval '4 days 12 hours'),
    ('FS-000101', 'LISTO_DESPACHO', 'ENVIADO', 'S', 'En camino', interval '1 day'),
    ('FS-000102', NULL, 'PENDIENTE_PAGO', 'C', 'Pedido creado', interval '9 days 1 hour'),
    ('FS-000102', 'PENDIENTE_PAGO', 'PAGADO', 'S', 'Pago confirmado', interval '9 days'),
    ('FS-000102', 'PAGADO', 'EN_PREPARACION', 'O', NULL, interval '8 days 20 hours'),
    ('FS-000102', 'EN_PREPARACION', 'LISTO_DESPACHO', 'O', NULL, interval '8 days 12 hours'),
    ('FS-000102', 'LISTO_DESPACHO', 'ENVIADO', 'S', 'En camino', interval '8 days'),
    ('FS-000102', 'ENVIADO', 'ENTREGADO', 'S', NULL, interval '7 days'),
    ('FS-000109', NULL, 'PENDIENTE_PAGO', 'C', 'Pedido creado', interval '12 days 1 hour'),
    ('FS-000109', 'PENDIENTE_PAGO', 'PAGADO', 'S', 'Pago confirmado', interval '12 days'),
    ('FS-000109', 'PAGADO', 'EN_PREPARACION', 'O', NULL, interval '11 days 20 hours'),
    ('FS-000109', 'EN_PREPARACION', 'LISTO_DESPACHO', 'O', NULL, interval '11 days 12 hours'),
    ('FS-000109', 'LISTO_DESPACHO', 'ENVIADO', 'S', 'En camino', interval '11 days'),
    ('FS-000109', 'ENVIADO', 'ENTREGADO', 'S', NULL, interval '10 days'),
    ('FS-000109', 'ENTREGADO', 'ENTREGADO', 'S', 'Reembolso postventa solicitado: S/ 69.90, TALLA_MODELO_EQUIVOCADO', interval '3 days'),
    ('FS-000109', 'ENTREGADO', 'ENTREGADO', 'S', 'Reembolso postventa completado: S/ 69.90', interval '2 days'),
    ('FS-000110', NULL, 'PENDIENTE_PAGO', 'C', 'Pedido creado', interval '6 days 1 hour'),
    ('FS-000110', 'PENDIENTE_PAGO', 'PAGADO', 'S', 'Pago confirmado', interval '6 days'),
    ('FS-000110', 'PAGADO', 'CANCELADO', 'C', 'Cancelado por el cliente', interval '5 days'),
    ('FS-000111', NULL, 'PENDIENTE_PAGO', 'C', 'Pedido creado', interval '2 days 1 hour'),
    ('FS-000111', 'PENDIENTE_PAGO', 'EXPIRADO', 'S', 'No se recibió el pago a tiempo', interval '1 day 23 hours')
) AS h(codigo, anterior, nuevo, actor, comentario, hace)
JOIN pedido p ON p.codigo = h.codigo
WHERE NOT EXISTS (SELECT 1 FROM pedido_historial x WHERE x.pedido_id = p.id);

-- ---------- Reembolso postventa parcial del pedido 8 (extensión de alcance, solo como dato) ----------
INSERT INTO pedido_reembolso (id, pedido_id, monto, motivo, nota, items, estado, solicitado_por, creado_en, completado_en)
VALUES ('b0000000-0000-4000-8000-000000000109', 'a0000000-0000-4000-8000-000000000109', 69.90,
        'TALLA_MODELO_EQUIVOCADO', 'La talla S le quedó pequeña',
        '[{"varianteId": 10014, "sku": "LEG02-NEG-S", "cantidad": 1}]'::jsonb, 'COMPLETADO',
        '5e738102-a490-469f-b0f6-5f1fc2dbf70c', now() - interval '3 days', now() - interval '2 days')
ON CONFLICT (id) DO NOTHING;

-- El próximo pedido que cree un cliente sigue a la demo: FS-000112
SELECT setval('pedido_codigo_seq', GREATEST((SELECT last_value FROM pedido_codigo_seq), 111));
