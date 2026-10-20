-- catalog_db: semillas aprobadas (SPEC-0a I6; micro catalog-service, "Datos semilla").
-- El color "Variado", las categorías y los productos de demostración son de db/dev (SPEC-2 sección 6), no de aquí.

INSERT INTO talla (codigo, orden) VALUES
    ('XS', 1), ('S', 2), ('M', 3), ('L', 4), ('XL', 5), ('XXL', 6);

INSERT INTO color (nombre, hex) VALUES
    ('negro',       '#1D1D1F'),
    ('blanco',      '#FFFFFF'),
    ('gris',        '#9A9A9A'),
    ('azul marino', '#1F2A44'),
    ('azul',        '#2563EB'),
    ('rojo',        '#DC2626'),
    ('rosado',      '#F472B6'),
    ('verde',       '#16A34A'),
    ('morado',      '#7C3AED'),
    ('beige',       '#D8C7A8');

-- Política global de descuentos por volumen (editable): 3 un. -> 5 %, 6 un. -> 10 %, 12 un. -> 15 %
INSERT INTO tramo_descuento (producto_id, unidades_minimas, porcentaje) VALUES
    (NULL, 3, 5), (NULL, 6, 10), (NULL, 12, 15);

INSERT INTO configuracion_descuento (id, activo) VALUES (1, true);

-- Stock mínimo por defecto 5 (0 a 999) y margen de reserva de 5 minutos (0 a 30)
INSERT INTO configuracion_inventario (id, stock_minimo_por_defecto, margen_reserva_minutos) VALUES (1, 5, 5);
