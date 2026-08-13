# Sistema de Pedidos de Comida · Microservicios

Trabajo final de **Arquitectura de Software · Módulo 5 · Tecsup**
Carlos Ormeño · agosto de 2026

Sistema de pedidos de comida a domicilio construido como **cinco
microservicios** que se coordinan mediante una **saga orquestada** sobre Kafka,
con **arquitectura hexagonal pura** dentro de cada servicio.

| | |
|---|---|
| Microservicios | 5 · Usuarios, Catálogo, Pedidos, Pagos, Entregas |
| Front | React + Vite, sin router ni librería de estado |
| Comunicación | 7 topics de Kafka · **una sola** llamada REST entre servicios |
| Datos | Un motor Postgres, **una base y un rol por servicio** |
| Pruebas | **125 automáticas**, en cuatro niveles |
| Decisiones | **13 ADR** con alternativas descartadas y verificación |
| Despliegue | Podman Compose **y** Kubernetes sobre kind |

---

## Por dónde empezar a leer

| Si quieres saber… | Lee |
|---|---|
| Cómo se atendió cada punto del enunciado | **[docs/RESPUESTA_AL_TRABAJO_FINAL.md](docs/RESPUESTA_AL_TRABAJO_FINAL.md)** |
| Qué hace cada servicio y cómo encajan | [docs/SERVICIOS_Y_FLUJO.md](docs/SERVICIOS_Y_FLUJO.md) |
| Por qué se decidió así y qué se descartó | [docs/adr/](docs/adr/README.md) |
| En qué archivo está cada concepto | [docs/MAPA_CODIGO.md](docs/MAPA_CODIGO.md) |
| Cómo se llegó hasta aquí | [docs/SEGUIMIENTO.md](docs/SEGUIMIENTO.md) |

---

## El flujo, en una frase

El cliente crea un pedido, **lo paga en un paso aparte**, y a partir de ahí todo
ocurre por eventos: Pagos cobra, Pedidos confirma, Catálogo descuenta stock,
Entregas asigna repartidor, y cuando el reparto se completa Usuarios suma puntos
de fidelidad.

```
CREADO → PAGO_EN_PROCESO → PAGADO → EN_PREPARACION → EN_CAMINO → ENTREGADO
                    ↓          ↓            ↓             ↓
               RECHAZADO   CANCELADO    CANCELADO     CANCELADO
```

Si el pago se rechaza, la saga **compensa**: nadie descuenta stock y no hay nada
que reembolsar. Medido en ejecución: **323 ms** desde el clic en pagar hasta
`EN_PREPARACION`, cruzando el broker cuatro veces.

---

## Levantarlo

Hay **dos despliegues, y no se levantan a la vez** porque no caben dos veces en
la misma máquina.

### Opción A · Podman Compose (recomendada para desarrollar)

Infraestructura en contenedores, servicios desde el IDE.

```bash
podman-compose up -d          # 10 contenedores
cd services && mvn clean install -DskipTests
```

Después, arrancar cada servicio desde el IDE o con
`mvn -pl <servicio> spring-boot:run`, y el front con:

```bash
cd frontend && npm install && npm run dev
```

| Interfaz | URL |
|---|---|
| Front | http://localhost:5173 |
| Swagger | `http://localhost:808x/swagger-ui.html` (8081–8085) |
| Grafana | http://localhost:3000 · `admin`/`admin` |
| Zipkin | http://localhost:9411 |
| Prometheus | http://localhost:9090 |
| kafka-ui | http://localhost:8090 |
| pgAdmin | http://localhost:5050 |

### Opción B · Kubernetes sobre kind

Todo dentro del clúster, bajo un único origen. Instrucciones completas en
**[k8s/README.md](k8s/README.md)**.

```bash
podman-compose down                       # los dos no caben a la vez
export KIND_EXPERIMENTAL_PROVIDER=podman
kind create cluster --config k8s/kind-cluster.yaml
# … ver k8s/README.md
```

| Interfaz | URL |
|---|---|
| Front y APIs | http://localhost:8000 |
| Grafana | http://localhost:8000/grafana |
| Zipkin | http://localhost:8000/zipkin |
| Prometheus | http://localhost:8000/prometheus |

### Acceso de demostración

| Para | Usuario | Contraseña |
|---|---|---|
| El front | `carlos@test.com` | `password123` |
| Grafana | `admin` | `admin` |

El usuario lo siembra Flyway, así que sobrevive a un `podman-compose down -v`.

---

## Probar la saga completa

1. Entra al front y añade productos al carrito.
2. **Confirma el pedido** → queda en `CREADO`, sin publicar nada.
3. En «Mis pedidos», pulsa **Pagar** → arranca la saga.
4. Ve a **Repartidor** y avanza la entrega: *Recoger y salir* → *Marcar
   entregado*.
5. El pedido llega a `ENTREGADO` y el contador de puntos sube.

**Para ver la compensación:** un pedido por encima de **S/ 500** lo rechaza
Pagos, y el pedido termina en `RECHAZADO` sin descontar stock.

**Para ver la cola de mensajes fallidos:** pide más unidades de las que hay en
stock y consulta `GET /api/admin/dlq` en Catálogo.

---

## Estructura del repositorio

```
services/          los 5 microservicios + el módulo shared
  shared/          DLQ, seguridad JWT y topics de reintento. Sin dominio
  <x>-service/     application/ domain/ infrastructure/
frontend/          React + Vite
k8s/               manifiestos de Kubernetes y configuración de kind
infra/             init de Postgres y configuración de observabilidad
docs/              ADR y documentación de arquitectura
docker-compose.yml infraestructura para desarrollo
```

Dentro de cada servicio, la regla que lo ordena todo: **el dominio no conoce
Spring, ni JPA, ni Kafka.** Verificable:

```bash
cd services/<servicio>/src/main/java/com/tecsup/app/micro/<servicio>
grep -rn "import \(org.springframework\|jakarta.persistence\|org.apache.kafka\)" domain/
# debe devolver vacío
```

---

## Pruebas

```bash
cd services && mvn test        # 125 pruebas
```

| Nivel | Qué cubre | Sin arrancar |
|---|---|---|
| Dominio puro | Máquinas de estados, totales, invariantes | Spring, BD, Kafka |
| Casos de uso | La saga completa, con dobles a mano | Spring, BD, Kafka |
| `@WebMvcTest` | Códigos HTTP, validación, errores | BD, Kafka |
| `@DataJpaTest` | Mapeo agregado ↔ entidad, restricciones | Spring completo, Kafka |

**Limitación declarada:** ninguna prueba arranca Kafka. No es un detalle menor —
la cola de mensajes fallidos estuvo rota de cuatro maneras encadenadas con 109
pruebas en verde. Está explicado en
[ADR-006](docs/adr/ADR-006-errores-reintentos-dlq.md).

---

## Lo que este trabajo no hace

Declarado, no escondido. La lista completa con su justificación está al final de
[docs/RESPUESTA_AL_TRABAJO_FINAL.md](docs/RESPUESTA_AL_TRABAJO_FINAL.md):
integración con pasarelas de pago reales, patrón outbox, CI/CD, Helm,
replicación de broker y base de datos, y pruebas con Testcontainers.
