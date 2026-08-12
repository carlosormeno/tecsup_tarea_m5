-- =============================================================================
-- V1: esquema inicial del servicio de Catálogo
-- =============================================================================

CREATE TABLE restaurante (
    id         BIGSERIAL     PRIMARY KEY,
    nombre     VARCHAR(150)  NOT NULL,
    direccion  VARCHAR(255)  NOT NULL,
    activo     BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE producto (
    id              BIGSERIAL      PRIMARY KEY,
    restaurante_id  BIGINT         NOT NULL REFERENCES restaurante (id),
    nombre          VARCHAR(150)   NOT NULL,
    descripcion     VARCHAR(500),
    precio          NUMERIC(10, 2) NOT NULL CHECK (precio >= 0),
    stock           INTEGER        NOT NULL CHECK (stock >= 0),
    activo          BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_producto_restaurante ON producto (restaurante_id);

-- =============================================================================
-- Eventos ya procesados.
--
-- Esta tabla es la razón por la que Catálogo es distinto de los demás
-- servicios. En Pedidos y en Pagos la idempotencia sale gratis: repetir una
-- transición de estado o comprobar si ya existe un pago basta. Aquí no:
-- descontar stock dos veces deja el inventario mal para siempre, y no hay
-- forma de saber, mirando el stock, si el descuento ya se aplicó.
--
-- La inserción va en la MISMA transacción que el ajuste de stock: o se
-- guardan las dos cosas o ninguna.
-- =============================================================================
CREATE TABLE evento_procesado (
    evento_id    VARCHAR(60)  PRIMARY KEY,
    topic        VARCHAR(255) NOT NULL,
    procesado_en TIMESTAMPTZ  NOT NULL
);

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
