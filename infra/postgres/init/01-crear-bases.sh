#!/bin/bash
# ============================================================================
# Crea una base de datos y un rol propio por microservicio.
#
# Se ejecuta UNA SOLA VEZ, cuando el volumen de Postgres está vacío. Si ya
# levantaste el contenedor antes y quieres re-ejecutarlo:
#     podman volume rm tarea_final_postgres-data
#
# El REVOKE ... FROM PUBLIC es lo que hace cumplir "database per service":
# sin él, cualquier rol podría conectarse a cualquier base y leer tablas
# ajenas, que es justo lo que la arquitectura prohíbe.
# ============================================================================
set -e

crear_base() {
    local base=$1
    local usuario=$2
    local clave=$3

    echo "  -> creando base '$base' con rol '$usuario'"

    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        CREATE USER $usuario WITH PASSWORD '$clave';
        CREATE DATABASE $base OWNER $usuario;
        REVOKE ALL ON DATABASE $base FROM PUBLIC;
        GRANT ALL PRIVILEGES ON DATABASE $base TO $usuario;
EOSQL
}

echo "== Creando las bases de los microservicios =="

crear_base userdb      user_svc      user_pass
crear_base catalogdb   catalog_svc   catalog_pass
crear_base orderdb     order_svc     order_pass
crear_base paymentdb   payment_svc   payment_pass
crear_base deliverydb  delivery_svc  delivery_pass

echo "== Listo: 5 bases, 5 roles, sin permisos cruzados =="
