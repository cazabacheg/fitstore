-- notification_db: esquema inicial (micro notification 3.1, SPEC-0a sección 6).
-- Los tamaños de columna que el micro no define son una propuesta del esqueleto 0a.2.

-- Textos editables de cada correo; el diseño vive en el código (micro 2.1 D2).
CREATE TABLE plantilla (
    codigo         varchar(40)  PRIMARY KEY,
    asunto         varchar(200) NOT NULL,
    titulo         varchar(200) NOT NULL,
    mensaje        text         NOT NULL,
    texto_boton    varchar(60),
    activa         boolean      NOT NULL,
    obligatoria    boolean      NOT NULL,
    actualizada_en timestamptz  NOT NULL,
    actualizada_por varchar(100)
);

-- Registro de envíos: una fila por correo, con estado y reintentos (D6).
CREATE TABLE envio_correo (
    id                 uuid         PRIMARY KEY,
    evento_id          uuid,
    plantilla          varchar(40)  NOT NULL,
    destinatario       varchar(254) NOT NULL,
    asunto             varchar(255) NOT NULL,
    variables          jsonb        NOT NULL,
    adjunto            varchar(20),
    estado             varchar(20)  NOT NULL,
    intentos           integer      NOT NULL DEFAULT 0,
    historial_intentos jsonb        NOT NULL DEFAULT '[]'::jsonb,
    proximo_intento_en timestamptz,
    ultimo_error       text,
    referencia         varchar(30),
    creado_en          timestamptz  NOT NULL,
    enviado_en         timestamptz,
    CONSTRAINT fk_envio_correo_plantilla FOREIGN KEY (plantilla) REFERENCES plantilla (codigo),
    CONSTRAINT ck_envio_correo_estado CHECK (estado IN ('PENDIENTE', 'ENVIANDO', 'ENVIADO', 'FALLIDO'))
);
CREATE INDEX ix_envio_correo_pendientes ON envio_correo (proximo_intento_en) WHERE estado = 'PENDIENTE';
CREATE INDEX ix_envio_correo_referencia ON envio_correo (referencia);
CREATE INDEX ix_envio_correo_creado_en ON envio_correo (creado_en DESC);

-- Copia local de la configuración pública de la tienda: una sola fila (D3).
CREATE TABLE marca_tienda (
    id              smallint    PRIMARY KEY,
    configuracion   jsonb       NOT NULL,
    correos_alertas text[]      NOT NULL,
    version         integer     NOT NULL,
    actualizado_en  timestamptz NOT NULL,
    CONSTRAINT ck_marca_tienda_unica CHECK (id = 1)
);

-- Copia local del resumen de cada pedido, tomada de PedidoCreado (D4).
CREATE TABLE pedido_resumen (
    pedido_id uuid         PRIMARY KEY,
    codigo    varchar(20)  NOT NULL,
    email     varchar(254) NOT NULL,
    nombre    varchar(200) NOT NULL,
    items     jsonb        NOT NULL,
    totales   jsonb        NOT NULL,
    modalidad varchar(20)  NOT NULL,
    creado_en timestamptz  NOT NULL,
    CONSTRAINT ck_pedido_resumen_modalidad CHECK (modalidad IN ('DELIVERY_LOCAL', 'AGENCIA', 'RECOJO'))
);

-- Tipos de infraestructura 10.3 (fitstore-common no crea tablas).
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
