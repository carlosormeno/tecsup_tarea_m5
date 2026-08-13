-- =============================================================================
-- V2: usuario de ejemplo
--
-- Existe por un motivo concreto: `podman-compose down -v` borra los volúmenes,
-- y con ellos las cinco bases. Catálogo repone sus productos porque los siembra
-- Flyway; los usuarios, hasta ahora, había que volver a registrarlos a mano, y
-- eso es justo lo que no se quiere descubrir delante de nadie.
--
-- Son las credenciales que el front trae ya rellenas en el formulario de
-- acceso, así que entrar es pulsar el botón:
--
--   correo:      carlos@test.com
--   contraseña:  password123
--
-- El hash es BCrypt de esa contraseña, generado por el propio servicio al
-- registrar el usuario y copiado de la base. NO se escribió a mano: un hash
-- inventado o con la longitud incorrecta hace fallar la verificación con un
-- error que no dice nada útil.
-- =============================================================================

INSERT INTO usuario (nombre, email, password_hash, direccion, puntos_fidelidad, activo, creado_en)
VALUES (
    'Carlos Ormeño',
    'carlos@test.com',
    '$2a$10$ktT5qNxB/XTt25uljtDNcetzzhF3gnPHSMRQKOHTCa15bHIRosqSi',
    'Av. Arequipa 123',
    0,
    true,
    now()
)
-- Idempotente a propósito: si la base ya tiene ese correo (porque alguien lo
-- registró antes de aplicar esta migración), no falla la migración entera por
-- una restricción de unicidad.
ON CONFLICT (email) DO NOTHING;

-- El rol. La tabla usuario_rol es una colección de elementos del agregado:
-- un usuario puede tener varios roles, y aquí basta con CLIENTE.
INSERT INTO usuario_rol (usuario_id, rol)
SELECT id, 'CLIENTE' FROM usuario WHERE email = 'carlos@test.com'
ON CONFLICT DO NOTHING;

-- =============================================================================
-- Sincroniza la secuencia con el id más alto insertado.
--
-- Sin esto, el siguiente registro desde el front pediría el id 1, que ya está
-- ocupado, y fallaría con violación de clave primaria. Es el efecto secundario
-- clásico de sembrar filas en una tabla con columna autoincremental.
-- =============================================================================
SELECT setval('usuario_id_seq', (SELECT COALESCE(MAX(id), 1) FROM usuario));
