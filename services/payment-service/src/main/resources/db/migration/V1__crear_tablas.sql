-- =============================================================================
-- V1: esquema inicial del servicio de Pagos
-- =============================================================================

CREATE TABLE pago (
    id             UUID           PRIMARY KEY,
    -- UNIQUE es lo que da idempotencia: si `pedido.creado` llega dos veces,
    -- la segunda inserción choca contra esta restricción en lugar de cobrar
    -- por duplicado. Sin esto haría falta una tabla de eventos procesados.
    pedido_id      UUID           NOT NULL UNIQUE,
    cliente_id     BIGINT         NOT NULL,
    monto          NUMERIC(10, 2) NOT NULL CHECK (monto >= 0),
    estado         VARCHAR(20)    NOT NULL,
    referencia     VARCHAR(60),
    motivo         VARCHAR(500),
    creado_en      TIMESTAMPTZ    NOT NULL,
    actualizado_en TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_pago_estado ON pago (estado);
CREATE INDEX idx_pago_cliente ON pago (cliente_id, creado_en DESC);

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
