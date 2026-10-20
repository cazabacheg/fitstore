-- payment_db: esquema inicial de payment-service (micro payment, sección 3.1) y tablas de eventos de fitstore-common (ADR-009).
-- Los pagos, transacciones, reembolsos y boletas nunca se eliminan (invariante 6): no hay borrados en cascada.
-- Tamaños de columna no definidos por el micro: propuestos en 0a.2 (ver el reporte de la entrega).

CREATE TABLE intencion_pago (
    id              uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    pedido_id       uuid          NOT NULL,
    codigo_pedido   varchar(20)   NOT NULL,
    cliente_id      uuid          NOT NULL,
    monto           numeric(12,2) NOT NULL,
    estado          varchar(30)   NOT NULL,
    expira_en       timestamptz   NOT NULL,
    items           jsonb         NOT NULL,   -- snapshot de los ítems para la boleta
    cliente         jsonb         NOT NULL,   -- nombre, DNI, email
    subtotal        numeric(12,2) NOT NULL,
    descuento_total numeric(12,2) NOT NULL,
    costo_envio     numeric(12,2) NOT NULL,
    creado_en       timestamptz   NOT NULL,
    actualizado_en  timestamptz   NOT NULL,
    version         bigint        NOT NULL DEFAULT 0,
    CONSTRAINT uq_intencion_pedido UNIQUE (pedido_id),
    CONSTRAINT ck_intencion_monto CHECK (monto > 0),
    CONSTRAINT ck_intencion_estado CHECK (estado IN
        ('PENDIENTE', 'PAGADA', 'ANULADA', 'REEMBOLSADA_PARCIAL', 'REEMBOLSADA'))
);
CREATE INDEX ix_intencion_cliente ON intencion_pago (cliente_id);

CREATE TABLE transaccion (
    id               uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    intencion_id     uuid          NOT NULL REFERENCES intencion_pago (id),
    proveedor        varchar(20)   NOT NULL,
    medio            varchar(20)   NOT NULL,
    estado           varchar(20)   NOT NULL,
    monto            numeric(12,2) NOT NULL,
    id_proveedor     varchar(100),
    codigo_respuesta varchar(100),
    mensaje          varchar(500),
    respuesta        jsonb,                   -- respuesta de la pasarela, sin datos sensibles
    idempotency_key  varchar(100)  NOT NULL,
    creado_en        timestamptz   NOT NULL,
    actualizado_en   timestamptz   NOT NULL,
    CONSTRAINT ck_transaccion_proveedor CHECK (proveedor IN ('CULQI', 'MERCADO_PAGO', 'SIMULADO')),
    CONSTRAINT ck_transaccion_medio CHECK (medio IN ('TARJETA', 'YAPE', 'SALDO_MP')),
    CONSTRAINT ck_transaccion_estado CHECK (estado IN ('PROCESANDO', 'APROBADA', 'RECHAZADA', 'ERROR')),
    CONSTRAINT uq_transaccion_idempotencia UNIQUE (intencion_id, idempotency_key)
);
-- Como máximo una transacción aprobada por intención: nunca se cobra dos veces un pedido (invariante 2)
CREATE UNIQUE INDEX ux_transaccion_aprobada ON transaccion (intencion_id) WHERE estado = 'APROBADA';
-- El verificador consulta a la pasarela las que quedaron procesando
CREATE INDEX ix_transaccion_procesando ON transaccion (actualizado_en) WHERE estado = 'PROCESANDO';

CREATE TABLE reembolso (
    id                 uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    intencion_id       uuid          NOT NULL REFERENCES intencion_pago (id),
    transaccion_id     uuid          NOT NULL REFERENCES transaccion (id),
    solicitud_id       uuid,                  -- de order, solo postventa
    monto              numeric(12,2) NOT NULL,
    motivo             varchar(20)   NOT NULL,
    estado             varchar(20)   NOT NULL,
    intentos           integer       NOT NULL DEFAULT 0,
    proximo_intento_en timestamptz,
    id_proveedor       varchar(100),
    ultimo_error       varchar(500),
    resuelto_por       varchar(100),
    nota               varchar(500),
    creado_en          timestamptz   NOT NULL,
    actualizado_en     timestamptz   NOT NULL,
    CONSTRAINT uq_reembolso_solicitud UNIQUE (solicitud_id),
    CONSTRAINT ck_reembolso_motivo CHECK (motivo IN ('CANCELACION', 'PAGO_NO_APLICABLE', 'POSTVENTA')),
    CONSTRAINT ck_reembolso_estado CHECK (estado IN ('PENDIENTE', 'COMPLETADO', 'FALLIDO', 'RESUELTO_MANUAL'))
);
CREATE INDEX ix_reembolso_intencion ON reembolso (intencion_id);
-- Un solo reembolso total por intención (cancelación o pago no aplicable); los postventa pueden ser varios
CREATE UNIQUE INDEX ux_reembolso_total ON reembolso (intencion_id)
    WHERE motivo IN ('CANCELACION', 'PAGO_NO_APLICABLE');

CREATE TABLE boleta (
    id           uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    intencion_id uuid         NOT NULL REFERENCES intencion_pago (id),
    serie        varchar(10)  NOT NULL,
    correlativo  integer      NOT NULL,
    numero       varchar(20)  NOT NULL,       -- B001-00000001
    clave        varchar(255) NOT NULL,       -- ruta del PDF en Garage (bucket privado)
    emitida_en   timestamptz  NOT NULL,
    CONSTRAINT uq_boleta_intencion UNIQUE (intencion_id),
    CONSTRAINT uq_boleta_serie_correlativo UNIQUE (serie, correlativo)
);

-- Se incrementa con UPDATE ... RETURNING dentro de la transacción que guarda la boleta: sin huecos (invariante 5)
CREATE TABLE correlativo_boleta (
    serie  varchar(10) PRIMARY KEY,
    ultimo integer     NOT NULL
);
INSERT INTO correlativo_boleta (serie, ultimo) VALUES ('B001', 0);

CREATE TABLE webhook_recibido (
    id                  uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    proveedor           varchar(20)  NOT NULL,
    evento_id_proveedor varchar(100) NOT NULL,
    payload             jsonb        NOT NULL,
    recibido_en         timestamptz  NOT NULL,
    procesado_en        timestamptz,
    CONSTRAINT ck_webhook_proveedor CHECK (proveedor IN ('CULQI', 'MERCADO_PAGO', 'SIMULADO')),
    CONSTRAINT uq_webhook_evento UNIQUE (proveedor, evento_id_proveedor)
);

-- Tipos que espera fitstore-common (infraestructura 10.3)
CREATE TABLE outbox_evento (
    id           uuid         PRIMARY KEY,
    tipo         varchar(100) NOT NULL,
    agregado_id  varchar(64)  NOT NULL,
    payload      jsonb        NOT NULL,
    creado_en    timestamptz  NOT NULL,
    publicado_en timestamptz
);
CREATE INDEX ix_outbox_pendientes ON outbox_evento (creado_en) WHERE publicado_en IS NULL;

CREATE TABLE evento_procesado (
    evento_id    uuid        PRIMARY KEY,
    procesado_en timestamptz NOT NULL
);
