-- Datos de demostración de settings (perfiles dev y demo). SPEC-3 sección 6.
-- Es una migración repetible: Flyway la vuelve a correr solo si cambia este archivo, así que las fechas de los
-- reclamos quedan relativas al momento de la carga (en la nube, el despliegue).
-- Las imágenes de la landing las sube DemoImagenesInicializador a Garage al arrancar.

-- Beneficios, contacto, redes y tienda física en Gamarra (La Victoria, Lima)
UPDATE configuracion_tienda SET
    beneficios = '[{"icono": "🚚", "texto": "Envíos a todo el Perú"}, {"icono": "↩", "texto": "Cambios hasta 30 días"}, {"icono": "🔒", "texto": "Pago seguro"}]'::jsonb,
    email = 'ventas@fitstore.pe',
    telefono = '014567890',
    whatsapp = '987654321',
    instagram_url = 'https://instagram.com/fitstore.pe',
    facebook_url = 'https://facebook.com/fitstore.pe',
    tiktok_url = 'https://tiktok.com/@fitstore.pe',
    horario = 'Lunes a sábado de 9:00 a 19:00',
    tienda_direccion = 'Galería Gamarra, Jr. Gamarra 700, tienda 210',
    tienda_ubigeo = '150115',
    tienda_referencia = 'A una cuadra del Mercado Mayorista',
    correos_alertas = ARRAY['ventas@fitstore.pe']
WHERE id = 1;

-- Tres reclamos para mostrar la bandeja: uno vencido, uno por vencer y uno respondido.
-- Las fechas límite caen en día hábil (el sábado y el domingo se mueven al viernes anterior).
WITH hoy AS (
    SELECT (now() AT TIME ZONE 'America/Lima')::date AS dia,
           extract(year FROM now() AT TIME ZONE 'America/Lima')::int AS anio
), datos AS (
    SELECT anio, 1 AS correlativo, 'RECLAMO' AS tipo, 'EN_ATENCION' AS estado,
           'Lucía' AS nombres, 'Ramos Huamán' AS apellidos, 'DNI' AS tipo_documento, '70112233' AS numero_documento,
           'Av. Arequipa 1234, Lince, Lima' AS domicilio, '987111222' AS telefono, 'lucia.ramos@correo.pe' AS email,
           'PRODUCTO' AS bien_tipo, 'Legging Compresión Pro talla M negro' AS bien_descripcion,
           89.90::numeric AS monto, 'FS-000101' AS pedido_codigo,
           'El legging llegó con la costura del tiro abierta.' AS detalle,
           'Quiero que me envíen uno nuevo.' AS pedido_consumidor,
           dia - 5 AS limite_base, now() - interval '22 days' AS creado_en,
           NULL::text AS respuesta, NULL::timestamptz AS respondido_en
    FROM hoy
    UNION ALL
    SELECT anio, 2, 'QUEJA', 'REGISTRADO',
           'Jorge', 'Salas Quispe', 'DNI', '41223344',
           'Jr. Cusco 456, Cercado, Arequipa', '987333444', 'jorge.salas@correo.pe',
           'SERVICIO', 'Atención por WhatsApp', 0::numeric, NULL,
           'Escribí tres veces al WhatsApp de la tienda y no me respondieron.',
           'Que me atiendan y me expliquen el estado de mi pedido.',
           dia + 2, now() - interval '12 days', NULL, NULL
    FROM hoy
    UNION ALL
    SELECT anio, 3, 'RECLAMO', 'RESPONDIDO',
           'Marisol', 'Torres Vega', 'DNI', '72334455',
           'Calle Los Pinos 88, Miraflores, Lima', '987555666', 'marisol.torres@correo.pe',
           'PRODUCTO', 'Top Soporte Medio talla S rosado', 54.90::numeric, 'FS-000099',
           'Me llegó una talla distinta a la que compré.',
           'Cambio por la talla S.',
           dia + 5, now() - interval '10 days',
           'Lamentamos el error. Coordinamos el recojo del top y te enviamos la talla S sin costo.',
           now() - interval '3 days'
    FROM hoy
)
INSERT INTO reclamo (numero, anio, correlativo, tipo, estado, nombres, apellidos, tipo_documento, numero_documento,
                     domicilio, telefono, email, es_menor, bien_tipo, bien_descripcion, monto_reclamado, pedido_codigo,
                     detalle, pedido_consumidor, fecha_limite, respuesta, respondido_en, respondido_por, ip, creado_en)
SELECT 'RC-' || anio || '-' || lpad(correlativo::text, 6, '0'), anio, correlativo, tipo, estado, nombres, apellidos,
       tipo_documento, numero_documento, domicilio, telefono, email, false, bien_tipo, bien_descripcion, monto,
       pedido_codigo, detalle, pedido_consumidor,
       CASE extract(isodow FROM limite_base) WHEN 6 THEN limite_base - 1 WHEN 7 THEN limite_base - 2 ELSE limite_base END,
       respuesta, respondido_en, CASE WHEN respuesta IS NULL THEN NULL ELSE 'admin-demo' END, '127.0.0.1', creado_en
FROM datos
ON CONFLICT (numero) DO UPDATE SET
    estado = EXCLUDED.estado, fecha_limite = EXCLUDED.fecha_limite, creado_en = EXCLUDED.creado_en,
    respuesta = EXCLUDED.respuesta, respondido_en = EXCLUDED.respondido_en, respondido_por = EXCLUDED.respondido_por;

-- El siguiente reclamo real continúa después de los de la demostración
INSERT INTO correlativo_reclamo (anio, ultimo)
VALUES (extract(year FROM now() AT TIME ZONE 'America/Lima')::int, 3)
ON CONFLICT (anio) DO UPDATE SET ultimo = GREATEST(correlativo_reclamo.ultimo, 3);
