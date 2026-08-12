-- =============================================================================
-- V1: esquema inicial del servicio de Usuarios
-- =============================================================================

CREATE TABLE usuario (
    id                BIGSERIAL     PRIMARY KEY,
    nombre            VARCHAR(150)  NOT NULL,
    email             VARCHAR(150)  NOT NULL UNIQUE,
    -- Nunca la clave en claro: solo su hash BCrypt, que ocupa 60 caracteres.
    password_hash     VARCHAR(100)  NOT NULL,
    direccion         VARCHAR(255),
    puntos_fidelidad  INTEGER       NOT NULL DEFAULT 0 CHECK (puntos_fidelidad >= 0),
    activo            BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en         TIMESTAMPTZ   NOT NULL
);

CREATE TABLE usuario_rol (
    usuario_id BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    rol        VARCHAR(20) NOT NULL,
    PRIMARY KEY (usuario_id, rol)
);

-- =============================================================================
-- Pedidos que ya sumaron puntos.
--
-- Cumple dos funciones a la vez:
--   1. IDEMPOTENCIA: si `pedido.entregado` llega dos veces, la clave primaria
--      impide sumar los puntos por duplicado. Igual que en Catálogo, sumar dos
--      veces corrompe datos y no hay forma de detectarlo mirando el total.
--   2. AUDITORÍA: deja el historial de cuántos puntos dio cada pedido, que un
--      simple contador en `usuario` no permitiría reconstruir.
-- =============================================================================
CREATE TABLE pedido_puntuado (
    pedido_id   UUID        PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL REFERENCES usuario (id),
    puntos      INTEGER     NOT NULL,
    otorgado_en TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_puntuado_usuario ON pedido_puntuado (usuario_id, otorgado_en DESC);

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
