-- Esquema de prueba equivalente al de las migraciones de los servicios (micro, sección 3.1):
-- outbox_evento (ADR-009) y evento_procesado. La librería no crea tablas.
CREATE TABLE IF NOT EXISTS outbox_evento (
    id           uuid         PRIMARY KEY,
    tipo         varchar(100) NOT NULL,
    agregado_id  varchar(64)  NOT NULL,
    payload      jsonb        NOT NULL,
    creado_en    timestamptz  NOT NULL,
    publicado_en timestamptz
);
CREATE INDEX IF NOT EXISTS ix_outbox_pendientes ON outbox_evento (creado_en) WHERE publicado_en IS NULL;

CREATE TABLE IF NOT EXISTS evento_procesado (
    evento_id    uuid        PRIMARY KEY,
    procesado_en timestamptz NOT NULL
);
