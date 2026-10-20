-- delivery_db: esquema inicial (micro delivery-service, sección 3.1) más las tablas comunes de eventos (ADR-009,
-- infraestructura 10.3). Los valores de modalidad y estado son los del contrato (AsyncAPI).

CREATE TABLE tarifa_envio (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    modalidad      varchar(20)   NOT NULL,
    ubigeo         varchar(6)    NOT NULL,
    costo          numeric(10,2) NOT NULL,
    plazo_dias_min integer       NOT NULL,
    plazo_dias_max integer       NOT NULL,
    activa         boolean       NOT NULL DEFAULT true,
    creado_en      timestamptz   NOT NULL,
    actualizado_en timestamptz   NOT NULL,
    CONSTRAINT uq_tarifa_modalidad_ubigeo UNIQUE (modalidad, ubigeo),
    CONSTRAINT ck_tarifa_modalidad CHECK (modalidad IN ('DELIVERY_LOCAL', 'AGENCIA')),
    CONSTRAINT ck_tarifa_ubigeo CHECK (ubigeo ~ '^([0-9]{2}|[0-9]{4}|[0-9]{6})$'),
    -- Invariante 2: el delivery local solo cubre Lima Metropolitana (1501...) y Callao (07...)
    CONSTRAINT ck_tarifa_local_lima_callao CHECK (modalidad <> 'DELIVERY_LOCAL' OR ubigeo ~ '^(1501|07)'),
    -- Invariante 3
    CONSTRAINT ck_tarifa_costo CHECK (costo >= 0),
    CONSTRAINT ck_tarifa_plazo_min CHECK (plazo_dias_min >= 0),
    CONSTRAINT ck_tarifa_plazos CHECK (plazo_dias_min <= plazo_dias_max)
);

CREATE TABLE agencia (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          varchar(100) NOT NULL,
    url_seguimiento varchar(300),
    activa          boolean      NOT NULL DEFAULT true,
    orden           integer      NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_agencia_nombre ON agencia (lower(nombre));

CREATE TABLE repartidor (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombres        varchar(100) NOT NULL,
    telefono       varchar(20)  NOT NULL,
    vehiculo       varchar(20)  NOT NULL,
    placa          varchar(10),
    activo         boolean      NOT NULL DEFAULT true,
    creado_en      timestamptz  NOT NULL,
    actualizado_en timestamptz  NOT NULL,
    CONSTRAINT ck_repartidor_vehiculo CHECK (vehiculo IN ('MOTO', 'AUTO', 'BICICLETA'))
);

CREATE TABLE entrega (
    id                 uuid PRIMARY KEY,
    pedido_id          uuid         NOT NULL,
    codigo_pedido      varchar(30)  NOT NULL,
    modalidad          varchar(20)  NOT NULL,
    estado             varchar(30)  NOT NULL,
    -- Copia del destinatario y la dirección que llegan en PedidoListoDespacho (D3)
    dest_nombres       varchar(100) NOT NULL,
    dest_apellidos     varchar(100) NOT NULL,
    dest_dni           varchar(8)   NOT NULL,
    dest_telefono      varchar(20)  NOT NULL,
    dest_email         varchar(254) NOT NULL,
    dir_ubigeo         varchar(6)   NOT NULL,
    dir_departamento   varchar(60)  NOT NULL,
    dir_provincia      varchar(60)  NOT NULL,
    dir_distrito       varchar(60)  NOT NULL,
    dir_direccion      varchar(200) NOT NULL,
    dir_referencia     varchar(200),
    repartidor_id      bigint REFERENCES repartidor (id),
    fecha_programada   date,
    intentos           integer      NOT NULL DEFAULT 0,
    agencia_id         bigint REFERENCES agencia (id),
    codigo_seguimiento varchar(50),
    clave_recojo       varchar(30),
    resumen_items      text         NOT NULL,
    bultos             integer      NOT NULL,
    creado_en          timestamptz  NOT NULL,
    actualizado_en     timestamptz  NOT NULL,
    version            bigint       NOT NULL DEFAULT 0,
    CONSTRAINT uq_entrega_pedido UNIQUE (pedido_id),
    CONSTRAINT ck_entrega_modalidad CHECK (modalidad IN ('DELIVERY_LOCAL', 'AGENCIA')),
    CONSTRAINT ck_entrega_estado CHECK (estado IN ('PENDIENTE_ASIGNACION', 'ASIGNADA', 'EN_RUTA', 'FALLIDA',
        'PENDIENTE_ENVIO', 'ENVIADO_AGENCIA', 'DISPONIBLE_EN_AGENCIA', 'ENTREGADA', 'DEVUELTA')),
    CONSTRAINT ck_entrega_intentos CHECK (intentos >= 0),
    CONSTRAINT ck_entrega_bultos CHECK (bultos >= 1)
);
CREATE INDEX ix_entrega_estado ON entrega (estado);
CREATE INDEX ix_entrega_fecha_repartidor ON entrega (fecha_programada, repartidor_id);

CREATE TABLE entrega_evento (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entrega_id  uuid        NOT NULL REFERENCES entrega (id),
    estado      varchar(30) NOT NULL,
    observacion text,
    actor_id    uuid,
    creado_en   timestamptz NOT NULL,
    CONSTRAINT ck_entrega_evento_estado CHECK (estado IN ('PENDIENTE_ASIGNACION', 'ASIGNADA', 'EN_RUTA', 'FALLIDA',
        'PENDIENTE_ENVIO', 'ENVIADO_AGENCIA', 'DISPONIBLE_EN_AGENCIA', 'ENTREGADA', 'DEVUELTA'))
);
CREATE INDEX ix_entrega_evento_entrega ON entrega_evento (entrega_id, creado_en);

-- Tablas comunes de eventos: los tipos que espera fitstore-common (infraestructura 10.3)
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
