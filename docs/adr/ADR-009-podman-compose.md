# ADR-009 · Podman Compose en lugar de Kubernetes

| | |
|---|---|
| **Estado** | Aceptada, **parcialmente sustituida por [ADR-012](ADR-012-kubernetes-con-kind.md)** (2026-08-12) |
| **Fecha** | 2026-08-09 |
| **Secciones del documento** | 4.2 Despliegue · 6.1 Escalabilidad |
| **Relacionada con** | [ADR-003](ADR-003-una-base-por-servicio.md) (un solo motor Postgres, por la misma razón) |

> **Nota posterior (2026-08-12).** Lo que sigue se mantiene sin editar porque el
> razonamiento fue correcto para lo que evaluaba: compose sigue siendo el
> despliegue de desarrollo y de demostración de la saga. Lo que este ADR no
> ponderó bien es que descartar Kubernetes dejaba sin cubrir tres puntos del
> enunciado (4.2, 6.1 y 6.2). Ver [ADR-012](ADR-012-kubernetes-con-kind.md).

## Contexto

Hay que desplegar cinco servicios, un front y la infraestructura que necesitan:
Postgres, Kafka con Zookeeper, y el conjunto de observabilidad —Prometheus,
Grafana, Zipkin, Loki y Promtail—, además de kafka-ui y pgAdmin para poder
mirar por dentro durante la demostración.

El Módulo 5 incluye una sesión sobre Kubernetes, así que la opción está sobre la
mesa y hay que justificar por qué no se usa. El destino real de este despliegue
es **un portátil, durante una sustentación**.

## Decisión

**Podman Compose**, con un único `docker-compose.yml` en la raíz. Sin
Kubernetes, sin Helm y sin CI/CD.

Cada servicio tiene su `Dockerfile` (los cinco lo tienen) para poder construirse
como imagen; la infraestructura se levanta con `podman-compose up -d`.

Podman y no Docker por una razón práctica y una de fondo: no exige demonio con
privilegios de root, y el formato de los archivos es el mismo, de modo que la
decisión no ata a nada.

**Estado actual, dicho sin adornos:** el compose levanta los **diez
contenedores de infraestructura**. Los cinco servicios y el front se ejecutan
todavía como procesos locales contra esa infraestructura; añadirlos al compose
está pendiente y figura en el checklist de
[SEGUIMIENTO.md](../SEGUIMIENTO.md). La decisión de este ADR es el mecanismo de
despliegue, y ese no cambia por dónde se ejecuten hoy los servicios.

## Alternativas consideradas

### A. Kubernetes local (kind, minikube o Docker Desktop) — *descartada*

Sería lo que se usaría en producción, y el módulo lo trata.

Se descarta por relación entre coste y beneficio: cinco `Deployment`, cinco
`Service`, `ConfigMap`, `Secret`, `StatefulSet` para Postgres y Kafka, y un
`Ingress` — del orden de veinte manifiestos para ejecutar en un portátil lo que
un archivo de compose ya resuelve. Nada de lo que se quiere demostrar en esta
tarea —la saga, la compensación, la DLQ, la trazabilidad— se demuestra mejor
con Kubernetes; se demuestra igual, con más piezas que pueden fallar delante del
profesor.

El punto que sí conviene declarar: **la aplicación no impide el paso a
Kubernetes**. Es configuración por variables de entorno, sin estado en memoria y
con `liveness` y `readiness` ya expuestas por Actuator. Migrar sería escribir
manifiestos, no reescribir servicios.

### B. Docker Compose — *descartada*

Idéntico en la práctica. Se descarta por la restricción del enunciado, que pide
Podman, y porque el demonio con privilegios de Docker no aporta nada aquí. Los
archivos son compatibles: cambiar de uno a otro es cambiar el comando.

### C. Todo como procesos locales, sin contenedores — *descartada*

Lo más rápido para desarrollar. Se descarta porque instalar Kafka, Zookeeper,
Postgres, Loki y Grafana a mano no es reproducible ni entregable: el trabajo
tiene que poder levantarlo otra persona.

## Consecuencias

### Positivas

- **Un comando levanta todo el entorno**, y `down -v` lo deja limpio para
  repetir una demostración desde cero.
- El archivo de compose es, de hecho, **documentación ejecutable** de la
  topología: se lee y se entiende qué habla con qué.
- Sin curva de aprendizaje ni piezas que fallen por su cuenta durante la
  sustentación.

### Negativas

- **No hay escalado automático ni recuperación ante fallos.** Si un contenedor
  muere, se levanta a mano.
- **El escalado horizontal se ejerce a mano**: `--scale` de compose, sin
  balanceador delante. Para los consumidores de Kafka esto importa menos de lo
  que parece, porque el reparto lo hace el broker por particiones (los topics
  tienen 3), no un balanceador HTTP.
- **No se ejercita Kubernetes**, que es contenido del módulo. Se compensa
  dejando declarado en el documento cómo sería la migración.
- **Un solo nodo**: la tolerancia a fallos de infraestructura no se puede
  demostrar de verdad (un solo broker Kafka, `replicas = 1` en todos los topics).

### Riesgos aceptados

| Riesgo | Por qué se acepta |
|---|---|
| `replicas = 1` en Kafka: perder el broker es perder los eventos no consumidos | Entorno local; en producción serían 3 brokers y factor de replicación 3, sin tocar código |
| Sin límites de CPU y memoria por contenedor | Un portátil no necesita cuotas; en Kubernetes serían `resources.limits` |
| El orden de arranque no está garantizado del todo | Mitigado: `healthcheck` en Postgres y Kafka, y los topics consumidos se declaran explícitamente para que un consumidor que arranca antes que su productor no se quede reintentando |

## Verificación

| Qué demuestra la decisión | Cómo se comprueba |
|---|---|
| Todo el entorno levanta con un comando | `podman-compose up -d` → 10 contenedores arriba |
| Es reproducible desde cero | `podman-compose down -v && up -d` recrea las 5 bases con sus roles |
| Los servicios son contenedorizables | Un `Dockerfile` por servicio, los cinco |
| La configuración no está incrustada | `DB_URL`, `KAFKA_SERVERS`, `JWT_SECRET`… son variables de entorno con valor por defecto |
| El camino a Kubernetes está abierto | `/actuator/health/liveness` y `/readiness` ya responden en los cinco |
