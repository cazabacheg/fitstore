-- order_db: esquema inicial de order-service (micro order 3.1, ADR-006 y ADR-009).
-- Los pedidos nunca se eliminan (invariante 7). Montos NUMERIC(10,2), PEN con IGV (D8).

-- Número del código legible FS-000123 (D4); el prefijo lo pone la aplicación (fitstore.order.codigo-prefijo)
CREATE SEQUENCE pedido_codigo_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE pedido (
    id                   uuid          PRIMARY KEY,
    codigo               varchar(20)   NOT NULL,
    cliente_id           uuid          NOT NULL,
    estado               varchar(20)   NOT NULL,
    modalidad            varchar(20)   NOT NULL,
    -- Destinatario (copia congelada, D3)
    dest_nombres         varchar(100)  NOT NULL,
    dest_apellidos       varchar(100)  NOT NULL,
    dest_dni             varchar(8)    NOT NULL,
    dest_telefono        varchar(20)   NOT NULL,
    dest_email           varchar(254)  NOT NULL,
    -- Dirección de envío: no existe en recojo
    dir_ubigeo           varchar(6),
    dir_departamento     varchar(60),
    dir_provincia        varchar(60),
    dir_distrito         varchar(60),
    dir_direccion        varchar(255),
    dir_referencia       varchar(255),
    -- Envío
    agencia_id           bigint,
    agencia_nombre       varchar(120),
    plazo_dias_min       integer,
    plazo_dias_max       integer,
    codigo_seguimiento   varchar(60),
    -- Montos
    subtotal             numeric(10,2) NOT NULL,
    descuento_total      numeric(10,2) NOT NULL,
    costo_envio          numeric(10,2) NOT NULL,
    total                numeric(10,2) NOT NULL,
    expira_en            timestamptz   NOT NULL,
    boleta_url           varchar(500),
    -- Pago (desde PagoConfirmado)
    pago_proveedor       varchar(20),
    pago_medio           varchar(20),
    pagado_en            timestamptz,
    monto_reembolsado    numeric(10,2) NOT NULL DEFAULT 0,
    idempotency_key      varchar(100)  NOT NULL,
    notas                varchar(500),
    creado_en            timestamptz   NOT NULL,
    actualizado_en       timestamptz   NOT NULL,
    version              bigint        NOT NULL DEFAULT 0,
    CONSTRAINT uk_pedido_codigo UNIQUE (codigo),
    CONSTRAINT uk_pedido_cliente_idempotencia UNIQUE (cliente_id, idempotency_key),
    CONSTRAINT ck_pedido_montos CHECK (subtotal >= 0 AND descuento_total >= 0 AND costo_envio >= 0
                                       AND total >= 0 AND monto_reembolsado >= 0),
    CONSTRAINT ck_pedido_estado CHECK (estado IN ('PENDIENTE_PAGO', 'PAGADO', 'EN_PREPARACION', 'LISTO_DESPACHO',
                                       'LISTO_RECOJO', 'ENVIADO', 'ENTREGADO', 'CANCELADO', 'EXPIRADO', 'DEVUELTO')),
    CONSTRAINT ck_pedido_modalidad CHECK (modalidad IN ('DELIVERY_LOCAL', 'AGENCIA', 'RECOJO'))
);
CREATE INDEX ix_pedido_cliente ON pedido (cliente_id, creado_en DESC);
CREATE INDEX ix_pedido_estado ON pedido (estado, creado_en);
-- Lo recorre el planificador de expiración cada minuto
CREATE INDEX ix_pedido_pendientes_expiracion ON pedido (expira_en) WHERE estado = 'PENDIENTE_PAGO';

CREATE TABLE pedido_item (
    id                   bigint        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pedido_id            uuid          NOT NULL REFERENCES pedido (id),
    variante_id          bigint        NOT NULL,
    producto_id          bigint        NOT NULL,
    sku                  varchar(60)   NOT NULL,
    producto_nombre      varchar(200)  NOT NULL,
    talla                varchar(20)   NOT NULL,
    color                varchar(40)   NOT NULL,
    imagen_url           varchar(500),
    precio_unitario      numeric(10,2) NOT NULL,
    cantidad             integer       NOT NULL,
    descuento_porcentaje numeric(5,2)  NOT NULL DEFAULT 0,
    descuento_monto      numeric(10,2) NOT NULL,
    total                numeric(10,2) NOT NULL,
    CONSTRAINT ck_pedido_item_cantidad CHECK (cantidad > 0)
);
CREATE INDEX ix_pedido_item_pedido ON pedido_item (pedido_id);
-- Top de productos del dashboard
CREATE INDEX ix_pedido_item_producto ON pedido_item (producto_id);

CREATE TABLE pedido_historial (
    id                   bigint        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pedido_id            uuid          NOT NULL REFERENCES pedido (id),
    estado_anterior      varchar(20),
    estado_nuevo         varchar(20)   NOT NULL,
    actor_tipo           varchar(10)   NOT NULL,
    actor_id             uuid,
    comentario           varchar(500),
    creado_en            timestamptz   NOT NULL,
    CONSTRAINT ck_pedido_historial_actor CHECK (actor_tipo IN ('CLIENTE', 'STAFF', 'SISTEMA'))
);
CREATE INDEX ix_pedido_historial_pedido ON pedido_historial (pedido_id, creado_en);
-- Ventas por fecha de pago (paso a PAGADO)
CREATE INDEX ix_pedido_historial_estado ON pedido_historial (estado_nuevo, creado_en);

CREATE TABLE pedido_reembolso (
    id                   uuid          PRIMARY KEY,
    pedido_id            uuid          NOT NULL REFERENCES pedido (id),
    monto                numeric(10,2) NOT NULL,
    motivo               varchar(30)   NOT NULL,
    nota                 varchar(500),
    items                jsonb         NOT NULL,
    estado               varchar(15)   NOT NULL,
    solicitado_por       uuid          NOT NULL,
    creado_en            timestamptz   NOT NULL,
    completado_en        timestamptz,
    CONSTRAINT ck_pedido_reembolso_monto CHECK (monto > 0),
    CONSTRAINT ck_pedido_reembolso_motivo CHECK (motivo IN ('DEFECTUOSO', 'TALLA_MODELO_EQUIVOCADO', 'NO_CONFORME',
                                                 'NO_ENTREGADO', 'OTRO')),
    CONSTRAINT ck_pedido_reembolso_estado CHECK (estado IN ('SOLICITADO', 'COMPLETADO'))
);
CREATE INDEX ix_pedido_reembolso_pedido ON pedido_reembolso (pedido_id);

-- Comunes: outbox (ADR-009) y consumidor idempotente, con los tipos que espera fitstore-common (infraestructura 10.3)
CREATE TABLE outbox_evento (
    id            uuid         PRIMARY KEY,
    tipo          varchar(100) NOT NULL,
    agregado_id   varchar(64)  NOT NULL,
    payload       jsonb        NOT NULL,
    creado_en     timestamptz  NOT NULL,
    publicado_en  timestamptz
);
CREATE INDEX ix_outbox_pendientes ON outbox_evento (creado_en) WHERE publicado_en IS NULL;

CREATE TABLE evento_procesado (
    evento_id     uuid         PRIMARY KEY,
    procesado_en  timestamptz  NOT NULL
);
