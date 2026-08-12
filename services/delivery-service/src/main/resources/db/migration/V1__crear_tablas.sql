-- =============================================================================
-- V1: esquema inicial del servicio de Entregas
-- =============================================================================

CREATE TABLE repartidor (
    id       BIGSERIAL     PRIMARY KEY,
    nombre   VARCHAR(150)  NOT NULL,
    telefono VARCHAR(20),
    activo   BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE entrega (
    id             UUID          PRIMARY KEY,
    -- UNIQUE da idempotencia: si `pedido.confirmado` llega dos veces, la
    -- segunda inserción choca aquí en lugar de crear una entrega duplicada
    -- y ocupar a un segundo repartidor con un pedido que ya tiene el suyo.
    pedido_id      UUID          NOT NULL UNIQUE,
    cliente_id     BIGINT        NOT NULL,
    direccion      VARCHAR(255)  NOT NULL,
    repartidor_id  BIGINT        NOT NULL REFERENCES repartidor (id),
    estado         VARCHAR(20)   NOT NULL,
    detalle        VARCHAR(500),
    creado_en      TIMESTAMPTZ   NOT NULL,
    actualizado_en TIMESTAMPTZ   NOT NULL
);

-- Consulta que se hace en cada asignación: cuántas entregas tiene ya en curso
-- cada repartidor.
CREATE INDEX idx_entrega_repartidor_estado ON entrega (repartidor_id, estado);

-- =============================================================================
-- Eventos que agotaron sus reintentos. Se consultan en GET /api/admin/dlq
-- =============================================================================
CREATE TABLE failed_events (
    id             BIGSERIAL     PRIMARY KEY,
    topic_origen   VARCHAR(255)  NOT NULL,
    offset_origen  BIGINT,
    payload        TEXT,
    mensaje_error  VARCHAR(1000),
    ocurrido_en    TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_failed_events_fecha ON failed_events (ocurrido_en DESC);
