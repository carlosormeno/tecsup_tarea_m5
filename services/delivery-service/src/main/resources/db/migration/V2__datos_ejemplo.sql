-- =============================================================================
-- V2: repartidores de ejemplo
--
-- Sin al menos un repartidor activo no se puede asignar ninguna entrega y la
-- saga se detendría en PAGADO. La semilla va por Flyway para que
-- `podman-compose up` deje el sistema utilizable sin pasos manuales.
-- =============================================================================

INSERT INTO repartidor (nombre, telefono) VALUES
    ('Luis Quispe',    '987654321'),
    ('María Huamán',   '987654322'),
    ('Jorge Ccahuana', '987654323'),
    ('Ana Mamani',     '987654324');

-- Inactivo a propósito: sirve para demostrar que la asignación solo considera
-- repartidores activos.
INSERT INTO repartidor (nombre, telefono, activo) VALUES
    ('Pedro Vargas', '987654325', FALSE);
