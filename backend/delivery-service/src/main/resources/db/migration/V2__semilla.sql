-- delivery_db: semilla (SPEC-0a I6, micro delivery-service sección 3.1 y "Datos semilla").
-- Los costos y plazos son VALORES DE DEMOSTRACIÓN, editables desde el backoffice (CRUD de tarifas y agencias).
-- Las URLs de seguimiento son de ejemplo (dominio rastreo.ejemplo.pe): las agencias son datos mock, sin integración.

INSERT INTO agencia (nombre, url_seguimiento, activa, orden) VALUES
    ('Shalom',             'https://rastreo.ejemplo.pe/shalom?codigo={codigo}',     true, 1),
    ('Olva Courier',       'https://rastreo.ejemplo.pe/olva?codigo={codigo}',       true, 2),
    ('Marvisur',           'https://rastreo.ejemplo.pe/marvisur?codigo={codigo}',   true, 3),
    ('Cruz del Sur Cargo', 'https://rastreo.ejemplo.pe/cruzdelsur?codigo={codigo}', true, 4);

-- Delivery local (1 a 2 días): Lima Metropolitana, Callao y dos distritos con tarifa propia
-- (Cercado de Lima y La Victoria, donde está Gamarra)
INSERT INTO tarifa_envio (modalidad, ubigeo, costo, plazo_dias_min, plazo_dias_max, activa, creado_en, actualizado_en) VALUES
    ('DELIVERY_LOCAL', '1501',   10.00, 1, 2, true, now(), now()),
    ('DELIVERY_LOCAL', '07',     12.00, 1, 2, true, now(), now()),
    ('DELIVERY_LOCAL', '150101',  7.00, 1, 2, true, now(), now()),
    ('DELIVERY_LOCAL', '150115',  7.00, 1, 2, true, now(), now());

-- Agencia: una tarifa por departamento (los 23 distintos de Lima y Callao). No hay tarifa '15': cubriría también a
-- Lima Metropolitana, que se atiende con delivery local. Escalonado: S/ 15 cerca de Lima, S/ 18 sur, centro y norte
-- medio (Arequipa, 2 a 3 días, es el ejemplo del micro 4.2), S/ 25 selva y extremo norte.
INSERT INTO tarifa_envio (modalidad, ubigeo, costo, plazo_dias_min, plazo_dias_max, activa, creado_en, actualizado_en) VALUES
    ('AGENCIA', '01', 25.00, 4, 5, true, now(), now()), -- Amazonas
    ('AGENCIA', '02', 15.00, 2, 3, true, now(), now()), -- Áncash
    ('AGENCIA', '03', 18.00, 3, 4, true, now(), now()), -- Apurímac
    ('AGENCIA', '04', 18.00, 2, 3, true, now(), now()), -- Arequipa
    ('AGENCIA', '05', 18.00, 3, 4, true, now(), now()), -- Ayacucho
    ('AGENCIA', '06', 18.00, 3, 4, true, now(), now()), -- Cajamarca
    ('AGENCIA', '08', 18.00, 3, 4, true, now(), now()), -- Cusco
    ('AGENCIA', '09', 15.00, 2, 3, true, now(), now()), -- Huancavelica
    ('AGENCIA', '10', 18.00, 3, 4, true, now(), now()), -- Huánuco
    ('AGENCIA', '11', 15.00, 2, 3, true, now(), now()), -- Ica
    ('AGENCIA', '12', 15.00, 2, 3, true, now(), now()), -- Junín
    ('AGENCIA', '13', 18.00, 3, 4, true, now(), now()), -- La Libertad
    ('AGENCIA', '14', 18.00, 3, 4, true, now(), now()), -- Lambayeque
    ('AGENCIA', '16', 25.00, 4, 5, true, now(), now()), -- Loreto
    ('AGENCIA', '17', 25.00, 4, 5, true, now(), now()), -- Madre de Dios
    ('AGENCIA', '18', 18.00, 3, 4, true, now(), now()), -- Moquegua
    ('AGENCIA', '19', 15.00, 2, 3, true, now(), now()), -- Pasco
    ('AGENCIA', '20', 25.00, 4, 5, true, now(), now()), -- Piura
    ('AGENCIA', '21', 18.00, 3, 4, true, now(), now()), -- Puno
    ('AGENCIA', '22', 25.00, 4, 5, true, now(), now()), -- San Martín
    ('AGENCIA', '23', 18.00, 3, 4, true, now(), now()), -- Tacna
    ('AGENCIA', '24', 25.00, 4, 5, true, now(), now()), -- Tumbes
    ('AGENCIA', '25', 25.00, 4, 5, true, now(), now()); -- Ucayali

-- Agencia: las 9 provincias del departamento de Lima fuera de Lima Metropolitana
INSERT INTO tarifa_envio (modalidad, ubigeo, costo, plazo_dias_min, plazo_dias_max, activa, creado_en, actualizado_en) VALUES
    ('AGENCIA', '1502', 15.00, 1, 3, true, now(), now()), -- Barranca
    ('AGENCIA', '1503', 15.00, 1, 3, true, now(), now()), -- Cajatambo
    ('AGENCIA', '1504', 15.00, 1, 3, true, now(), now()), -- Canta
    ('AGENCIA', '1505', 15.00, 1, 3, true, now(), now()), -- Cañete
    ('AGENCIA', '1506', 15.00, 1, 3, true, now(), now()), -- Huaral
    ('AGENCIA', '1507', 15.00, 1, 3, true, now(), now()), -- Huarochirí
    ('AGENCIA', '1508', 15.00, 1, 3, true, now(), now()), -- Huaura
    ('AGENCIA', '1509', 15.00, 1, 3, true, now(), now()), -- Oyón
    ('AGENCIA', '1510', 15.00, 1, 3, true, now(), now()); -- Yauyos
