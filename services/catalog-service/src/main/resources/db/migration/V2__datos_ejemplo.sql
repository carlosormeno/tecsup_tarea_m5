-- =============================================================================
-- V2: datos de ejemplo
--
-- Van por Flyway y no por un script suelto para que `podman-compose up` deje
-- el sistema utilizable sin pasos manuales: sin catálogo no se puede crear
-- ningún pedido, así que sin esto no habría nada que demostrar.
-- =============================================================================

INSERT INTO restaurante (nombre, direccion) VALUES
    ('La Pizzería de Tony', 'Av. Arequipa 1234, Lima'),
    ('Chifa Central',       'Jr. Ucayali 780, Lima'),
    ('Pollería El Brasero', 'Av. Benavides 2050, Miraflores');

-- Producto 1: precio y stock pensados para las demostraciones.
INSERT INTO producto (restaurante_id, nombre, descripcion, precio, stock) VALUES
    (1, 'Pizza margarita',      'Muzzarella, tomate y albahaca',        35.90, 100),
    (1, 'Pizza pepperoni',      'Muzzarella y pepperoni',               42.50, 100),
    (1, 'Gaseosa 500ml',        'Bebida fría',                           5.00, 500),
    (2, 'Arroz chaufa de pollo','Con tortilla y sillao',                28.00, 100),
    (2, 'Wantán frito',         'Ocho unidades con salsa tamarindo',    18.50, 100),
    (2, 'Sopa wantán',          'Caldo de pollo con verduras',          22.00,  80),
    (3, 'Pollo a la brasa 1/4', 'Con papas fritas y ensalada',          24.90, 120),
    (3, 'Pollo a la brasa 1/2', 'Con papas fritas y ensalada',          45.00,  90),
    (3, 'Chicha morada 1L',     'Jarra familiar',                       12.00, 200);

-- Producto agotado a propósito: sirve para demostrar en vivo que un pedido
-- con algo no disponible se rechaza con 422 antes siquiera de crearse.
INSERT INTO producto (restaurante_id, nombre, descripcion, precio, stock, activo) VALUES
    (1, 'Tiramisú de la casa', 'Postre artesanal', 15.00, 0, TRUE);
