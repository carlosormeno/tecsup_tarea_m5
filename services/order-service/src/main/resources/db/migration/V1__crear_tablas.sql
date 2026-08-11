-- =============================================================================
-- V1: esquema inicial del servicio de Pedidos
-- =============================================================================

CREATE TABLE pedido (
    id                 UUID          PRIMARY KEY,
    cliente_id         BIGINT        NOT NULL,
    direccion_entrega  VARCHAR(255)  NOT NULL,
    estado             VARCHAR(20)   NOT NULL,
    motivo             VARCHAR(500),
    creado_en          TIMESTAMPTZ   NOT NULL,
    actualizado_en     TIMESTAMPTZ   NOT NULL
);

-- Listar los pedidos de un cliente es la consulta más frecuente del front.
CREATE INDEX idx_pedido_cliente ON pedido (cliente_id, creado_en DESC);

-- Útil para tableros del tipo "cuántos pedidos hay en cada estado".
CREATE INDEX idx_pedido_estado ON pedido (estado);

CREATE TABLE pedido_linea (
    pedido_id        UUID           NOT NULL REFERENCES pedido (id) ON DELETE CASCADE,
    producto_id      BIGINT         NOT NULL,
    nombre_producto  VARCHAR(255)   NOT NULL,
    precio_unitario  NUMERIC(10, 2) NOT NULL,
    cantidad         INTEGER        NOT NULL CHECK (cantidad > 0)
);

CREATE INDEX idx_linea_pedido ON pedido_linea (pedido_id);

-- =============================================================================
-- Eventos que agotaron sus reintentos.
--
-- Se consultan por REST en GET /api/admin/dlq. El payload es TEXT y no
-- VARCHAR: un pedido con varias líneas supera de sobra cualquier límite corto,
-- y perder el registro justo aquí sería perder la información del fallo.
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
