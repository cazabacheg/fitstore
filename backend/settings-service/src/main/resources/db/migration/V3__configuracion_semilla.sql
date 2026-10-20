-- Configuración inicial de la tienda (micro settings 2.2, invariante 1): la marca de demostración de los diseños.
-- La tienda real la reemplaza desde el backoffice.

INSERT INTO configuracion_tienda (
    id,
    nombre, eslogan, razon_social, ruc, direccion_fiscal,
    color_primario, color_secundario, color_acento, color_fondo, color_texto,
    hero_titulo, hero_subtitulo, hero_cta_texto, hero_cta_enlace, beneficios,
    email, telefono, whatsapp, instagram_url, facebook_url, tiktok_url, horario,
    tienda_direccion, tienda_ubigeo, tienda_referencia,
    metodos_pago, modalidades_entrega, expiracion_pedido_minutos, dias_reembolso_postventa, correos_alertas,
    version, actualizado_en, actualizado_por
) VALUES (
    1,
    'FitStore', 'Ropa deportiva hecha en Gamarra.', 'Moda Activa S.A.C.', '20123456789',
    'Jr. Gamarra 700, tienda 210, La Victoria, Lima',
    '#C9B28C', '#2B2621', '#4E5B45', '#FAF9F7', '#26221D',
    'Muévete con estilo',
    'Leggings, tops y casacas hechos en Gamarra, con tela de compresión y secado rápido.',
    'Ver novedades', '/productos', '[]'::jsonb,
    'ventas@tienda.pe', '987654321', '987654321', NULL, NULL, NULL, 'Lunes a sábado de 9:00 a 19:00',
    'Galería Gamarra, Jr. Gamarra 700, tienda 210', '150115', NULL,
    ARRAY['CULQI', 'YAPE', 'MERCADO_PAGO'], ARRAY['DELIVERY_LOCAL', 'AGENCIA', 'RECOJO'], 30, 30,
    ARRAY['ventas@tienda.pe'],
    0, now(), NULL
);

-- Versión 1 de cada política (invariante 5: siempre hay una vigente). Texto de ejemplo.
INSERT INTO politica (tipo, version, contenido, publicada_en) VALUES
    ('TERMINOS', 1, E'# Términos y condiciones\n\nEste es contenido de ejemplo. La tienda debe reemplazarlo desde el backoffice (Configuración de tienda, Políticas).', now()),
    ('PRIVACIDAD', 1, E'# Política de privacidad\n\nEste es contenido de ejemplo. La tienda debe reemplazarlo desde el backoffice (Configuración de tienda, Políticas).', now()),
    ('CAMBIOS_DEVOLUCIONES', 1, E'# Cambios y devoluciones\n\nEste es contenido de ejemplo. La tienda debe reemplazarlo desde el backoffice (Configuración de tienda, Políticas).', now()),
    ('ENVIOS', 1, E'# Envíos\n\nEste es contenido de ejemplo. La tienda debe reemplazarlo desde el backoffice (Configuración de tienda, Políticas).', now());
