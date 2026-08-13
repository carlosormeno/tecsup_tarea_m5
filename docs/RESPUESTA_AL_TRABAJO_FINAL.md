# Sistema de Pedidos de Comida · Respuesta al Trabajo Final

**Arquitectura de Software · Módulo 5 · Tecsup**
Autor: Carlos Ormeño · 12 de agosto de 2026
Repositorio: `git@github.com:carlosormeno/tecsup_tarea_m5.git`

---

Este documento recorre el enunciado
[TrabajoFinal_Arquitectura_Microservicios_Pedidos_Comida.md](../TrabajoFinal_Arquitectura_Microservicios_Pedidos_Comida.md)
**punto por punto**. En cada uno:

**Pide:** lo que dice el enunciado, literal.

**Cómo se atendió** · qué se construyó y por qué se decidió así.

**Evidencia** · dónde mirarlo, y cómo comprobar que es cierto.

Donde el enunciado propone una tecnología que **no** se usó (Kubernetes, Nginx,
Stripe, GitHub Actions), se dice claramente, con la justificación y el ADR que
la respalda. Nada se da por hecho sin poder señalarlo.

---

# 1. Introducción General

## 1.1 Propósito del documento

> **Pide:** *Documentar la arquitectura de un sistema de pedidos de comida basado en microservicios.*

**Cómo se atendió.** El sistema no está solo documentado: **está construido y
funcionando**. Este documento describe la arquitectura de un sistema que se
puede levantar, usar desde el navegador y observar mientras trabaja.

La documentación se reparte en cuatro documentos con un propósito cada uno, en
lugar de un archivo único que nadie termina de leer:

| Documento | Responde a |
|---|---|
| Este | ¿Cómo se atendió cada punto del enunciado? |
| [SERVICIOS_Y_FLUJO.md](SERVICIOS_Y_FLUJO.md) | ¿Qué hace cada servicio y cómo encajan? |
| [MAPA_CODIGO.md](MAPA_CODIGO.md) | ¿En qué archivo está cada concepto? |
| [docs/adr/](adr/README.md) | ¿Por qué se decidió así y qué se descartó? |

## 1.2 Alcance del sistema

> **Pide:** *El sistema permite a los usuarios realizar pedidos desde múltiples restaurantes, gestionar pagos y coordinar entregas.*

**Cómo se atendió.** Ese alcance se cumple entero, con un recorrido completo:

| Dentro del alcance | Fuera del alcance, y por qué |
|---|---|
| Registro y acceso de usuarios con JWT | Recuperación de contraseña, verificación por correo |
| Catálogo de restaurantes y productos con stock | Búsqueda, filtros, categorías |
| Creación de pedidos con precio congelado | Programación de pedidos, propinas, cupones |
| **Pago como paso explícito** del cliente | Pasarela real: la aprobación la decide una regla de dominio |
| Asignación de repartidor con reparto de carga | Rutas, mapas, geolocalización |
| Seguimiento del pedido y compensación | Notificaciones push, valoraciones |
| Puntos de fidelidad | Canje de puntos |

**Despliegue:** local, en dos formas verificadas — Podman Compose para el
desarrollo y la demostración de la saga, y Kubernetes sobre kind para la
orquestación, el balanceo y el autoescalado ([ADR-012](adr/ADR-012-kubernetes-con-kind.md)).

## 1.3 Audiencia y nivel técnico esperado

> **Pide:** *Desarrolladores backend, DevOps, QA y arquitectos con conocimientos intermedios en arquitectura distribuida.*

Sin cambios. El documento asume conocidos REST, Kafka, contenedores y saga, y
explica en el [glosario](#91-glosario) lo que es propio de este sistema.

---

# 2. Visión Arquitectónica General

## 2.1 Estilo arquitectónico utilizado

> **Pide:** *Arquitectura de Microservicios, con servicios independientes desplegados en contenedores Docker.*

**Cómo se atendió.** Microservicios, sí, y con una precisión que el enunciado no
recoge: **el estilo tiene dos niveles**.

**Hacia fuera, microservicios.** Cinco servicios, uno por contexto delimitado,
con proceso, puerto, base de datos y despliegue propios. La frontera del
servicio coincide con la del contexto: ni un servicio que abarque dos, ni dos
que se repartan uno ([ADR-001](adr/ADR-001-microservicios-frente-a-monolito.md)).

**Hacia dentro, hexagonal pura.** Cada servicio es puertos y adaptadores: el
dominio no conoce Spring, ni JPA, ni Kafka
([ADR-002](adr/ADR-002-arquitectura-hexagonal.md)). Eso es lo que permite
probar la saga completa en milisegundos, sin levantar nada.

Sobre *contenedores Docker*: se usa **Podman**, compatible con el mismo formato
de `Dockerfile` y sin demonio con privilegios de root. Los cinco servicios y el
front tienen su `Dockerfile`.

**Evidencia.**

```bash
# El hexágono está cerrado: los tres comandos devuelven vacío en los 5 servicios
cd services/<servicio>/src/main/java/com/tecsup/app/micro/<servicio>
grep -rn "import com.tecsup.*\(application\|infrastructure\)" domain/
grep -rn "import \(org.springframework\|jakarta.persistence\|org.apache.kafka\)" domain/
grep -rn "import com.tecsup.*infrastructure" application/
```

## 2.2 Decisiones arquitectónicas clave

> **Pide:** *Separación de funcionalidades en servicios independientes, comunicación vía REST y eventos Kafka, uso de base de datos por servicio.*

**Cómo se atendió.** Las tres están, y se documentaron **trece decisiones** en
formato ADR, cada una con las alternativas descartadas y cómo verificarla:

| ADR | Decisión | Lo que se descartó |
|---|---|---|
| [001](adr/ADR-001-microservicios-frente-a-monolito.md) | Cinco microservicios, uno por contexto | Monolito modular · servicios más finos |
| [002](adr/ADR-002-arquitectura-hexagonal.md) | Hexagonal dentro de cada servicio | Capas clásicas · puertos en `application/port/out` |
| [003](adr/ADR-003-una-base-por-servicio.md) | Un motor Postgres, una base y un rol por servicio | Base compartida · esquema por servicio |
| [004](adr/ADR-004-saga-orquestada.md) | Saga orquestada, Pedidos como orquestador | Coreografía · 2PC · orquestador dedicado |
| [005](adr/ADR-005-validacion-de-precio-sincrona.md) | El precio se pregunta por REST y se congela | Réplica local por eventos |
| [006](adr/ADR-006-errores-reintentos-dlq.md) | Reintentos, DLQ por servicio, idempotencia focalizada | DLQ única · idempotencia uniforme |
| [007](adr/ADR-007-sin-outbox.md) | Sin outbox: ventana de inconsistencia asumida | Outbox por sondeo · CDC |
| [008](adr/ADR-008-que-se-comparte.md) | Se comparte fontanería, nunca contratos | Módulo `common` · Avro |
| [009](adr/ADR-009-podman-compose.md) | Podman Compose | Kubernetes · Docker |
| [010](adr/ADR-010-pago-explicito.md) | Crear y pagar son dos pasos | Cobro automático al crear |
| [011](adr/ADR-011-jwt-clave-simetrica.md) | JWT HS256 con clave compartida | RS256 con JWKS · Redis · pasarela |
| [012](adr/ADR-012-kubernetes-con-kind.md) | Kubernetes con kind, además de compose | Solo compose · migrar del todo · Helm |
| [013](adr/ADR-013-circuit-breaker.md) | Circuit breaker solo en la llamada síncrona | En los cinco · solo timeouts · caché |

**Un matiz sobre «comunicación vía REST y eventos»:** no están al mismo nivel.
**Hay exactamente una llamada REST entre servicios** en todo el sistema
—Pedidos a Catálogo, para validar precio y disponibilidad— y todo lo demás va
por Kafka. Que sea una y no varias es una decisión, no una casualidad, y está
justificada en [ADR-005](adr/ADR-005-validacion-de-precio-sincrona.md).

**Evidencia:** `grep -rln "RestClient" services/*/src/main/java` → solo
`order-service`.

## 2.3 Diagramas de alto nivel

> **Pide:** *El sistema consta de servicios como: Servicio de Usuarios, Servicio de Pedidos, Servicio de Catálogo, Servicio de Entregas, y Servicio de Pagos.*

**Cómo se atendió.** Los cinco servicios son esos. Hay **tres diagramas** en
[SERVICIOS_Y_FLUJO.md](SERVICIOS_Y_FLUJO.md), en Mermaid, que se renderizan en
GitHub:

| Diagrama | Qué muestra |
|---|---|
| Contenedores | Los 5 servicios, Kafka, las 5 bases y el front |
| Secuencia · camino feliz | De crear el pedido a entregarlo, con las dos llamadas del cliente |
| Secuencia · compensación | Pago rechazado y qué deshace cada servicio |

Dos reglas se leen directamente en el primero: **solo hay una flecha REST entre
servicios**, y **ninguna base tiene dos flechas**.

---

# 3. Componentes del Sistema

## 3.1 Módulos principales y responsabilidades

> **Pide:** *Cada servicio tiene responsabilidades bien definidas. Por ejemplo, el Servicio de Pedidos gestiona la creación y seguimiento de pedidos.*

**Cómo se atendió.** Cada servicio se documentó con cuatro cosas, y la cuarta es
la que evita solapamientos: **qué NO hace**.

| Servicio | Puerto | Base | En una frase | No hace |
|---|---|---|---|---|
| **Usuarios** | 8081 | `userdb` | Quién eres y qué puedes hacer | No guarda historial de pedidos |
| **Catálogo** | 8082 | `catalogdb` | Qué se puede pedir y a qué precio | No conoce pedidos ni totales |
| **Pedidos** | 8083 | `orderdb` | El ciclo de vida del pedido · **orquestador** | No cobra ni asigna repartidores |
| **Pagos** | 8084 | `paymentdb` | Cobrar y devolver dinero | No decide el estado del pedido |
| **Entregas** | 8085 | `deliverydb` | Llevar el pedido físicamente | No cambia el estado del pedido |

El detalle por servicio —qué posee, qué expone, qué publica, qué consume— está
en [SERVICIOS_Y_FLUJO.md §2](SERVICIOS_Y_FLUJO.md).

## 3.2 Interfaces y APIs expuestas

> **Pide:** *APIs REST para clientes web/móviles, y APIs internas entre servicios. Contratos documentados con OpenAPI o similares.*

**Cómo se atendió.** Los cinco servicios exponen Swagger UI en
`http://localhost:808x/swagger-ui.html`, generado con springdoc 2.8.6.

| Servicio | Endpoints principales |
|---|---|
| Usuarios | `POST /auth/registro` · `POST /auth/login` · `GET /api/usuarios/{id}` |
| Catálogo | `GET /api/productos` · `GET /api/productos/{id}` · `GET /api/restaurantes` |
| Pedidos | `POST /api/pedidos` · **`POST /api/pedidos/{id}/pagar`** · `GET /api/pedidos?clienteId=` · `POST /api/pedidos/{id}/cancelar` |
| Pagos | `GET /api/pagos/pedido/{id}` · `POST /api/pagos/pedido/{id}/reembolso` |
| Entregas | `GET /api/entregas` · `GET /api/entregas/pedido/{id}` · `PATCH /api/entregas/{id}/estado` |
| Todos | `GET /api/admin/dlq` · `/actuator/health` · `/actuator/prometheus` |

**Dos ausencias deliberadas, que dicen más que la lista:**

**Pagos no expone ningún endpoint para cobrar.** El cobro solo se dispara por
evento, así que nadie puede saltarse la saga con un `curl`. El botón «pagar»
del front no llama a Pagos: llama a Pedidos, que publica el evento.

**Entregas no expone ningún endpoint para crear entregas.** Una entrega solo
nace de un `pedido.confirmado`.

Sobre *APIs internas entre servicios*: solo existe una, `GET /api/productos/{id}`
de Catálogo, y **no es una API distinta** — es la misma que usa el front, con el
JWT del cliente propagado.

## 3.3 Comunicación entre componentes

> **Pide:** *Principalmente asincrónica vía Kafka, con REST para operaciones sincrónicas necesarias.*

**Cómo se atendió.** Exactamente así, con **siete topics**:

| Topic | Publica | Consume |
|---|---|---|
| `pedido.pago-solicitado` | Pedidos | Pagos |
| `pago.confirmado` | Pagos | Pedidos |
| `pago.rechazado` | Pagos | Pedidos |
| `pedido.confirmado` | Pedidos | Catálogo, Entregas |
| `entrega.estado-cambiado` | Entregas | Pedidos |
| `pedido.entregado` | Pedidos | Usuarios |
| `pedido.cancelado` | Pedidos | Pagos, Catálogo |

Todos usan **el id del pedido como clave de partición**: todo lo que afecta a un
pedido cae en la misma partición y se consume en orden, aunque venga de
servicios distintos.

**La saga es orquestada** ([ADR-004](adr/ADR-004-saga-orquestada.md)), y se
reconoce en un detalle verificable: **Entregas no escucha a Pagos, escucha a
Pedidos.**

## 3.4 Integración con sistemas externos (OPCIONAL)

> **Pide:** *Integración con pasarelas de pago como Stripe y servicios de geolocalización para entregas.*

**No se implementa, y es una decisión, no una omisión.**

El punto es opcional en el enunciado. Se descartó porque una integración real
con Stripe aportaría trabajo de credenciales, webhooks y entorno de pruebas
**sin ejercitar ni un patrón arquitectónico nuevo**: la saga, la compensación y
la idempotencia se demuestran igual con una regla de dominio.

Lo que sí se conserva es la **forma** de la integración: el resultado del cobro
lo decide una regla del dominio de Pagos (importe por encima del límite →
rechazado), y el resto del sistema reacciona igual que reaccionaría ante una
pasarela real. Sustituir la regla por una llamada a Stripe sería cambiar un
adaptador, sin tocar la saga.

---

# 4. Detalle del Estilo Arquitectónico

## 4.1 Justificación del estilo y alternativas descartadas

> *El enunciado salta del título 4 al 4.2. Este apartado se añade porque una decisión de estilo sin alternativas descartadas no es una decisión, es una casualidad.*

**Por qué microservicios.** El dominio se descompone en cinco contextos
delimitados con lenguaje propio, dueño de datos propio y motivos de cambio
distintos. La prueba de que las fronteras son reales está en el propio
vocabulario: **el mismo hecho físico tiene dos nombres**. Cuando el repartidor
deja la comida, Entregas lo llama `COMPLETADA` y Pedidos lo llama `ENTREGADO`.
Un modelo único obligaría a un vocabulario de compromiso que no sería el natural
de ninguno.

**Alternativas descartadas** (desarrollo completo en
[ADR-001](adr/ADR-001-microservicios-frente-a-monolito.md)):

| Alternativa | Por qué se descartó |
|---|---|
| **Monolito modular** | Las fronteras quedarían sostenidas solo por disciplina, y el dominio de fallo seguiría siendo único |
| **Servicios más finos** (Notificaciones, Inventario) | Prematuro: Inventario es la columna `stock` de Catálogo |
| **Monolito con el cobro aparte** | No ilustra ninguno de los problemas del módulo: sin varios servicios no hay saga |

**Lo que costó**, dicho sin adornos: se pierde la transacción, hay que tolerar
duplicados en todas partes, el coste de operación se multiplica por cinco, y
depurar es más difícil. Ocurrió de verdad: la cola de mensajes fallidos estuvo
rota de cuatro maneras encadenadas **con 109 pruebas en verde**, porque ninguna
arranca Kafka.

## 4.2 Arquitectura de Microservicios

> **Pide:** *Cada servicio es autónomo, despliegue independiente, y mantiene su propia base de datos. Uso de Kubernetes para orquestación.*

**Autonomía, despliegue independiente y base propia: sí.** Cada servicio tiene
su módulo Maven, su `Dockerfile`, su base y su rol de Postgres. Verificado en
ejecución: **con Pagos detenido, los pedidos se siguen creando**, y el cobro se
procesa al volver el servicio porque el evento espera en Kafka.

**Kubernetes: sí, y verificado.** El sistema completo corre sobre **kind con el
proveedor de Podman** ([ADR-012](adr/ADR-012-kubernetes-con-kind.md)): 14 pods,
todos `1/1 Running` — Postgres, Zookeeper, Kafka, los cinco servicios, el front
y las cuatro herramientas de observabilidad.

**Los dos despliegues conviven, con papeles distintos:**

| | Podman Compose | Kubernetes (kind) |
|---|---|---|
| Para qué | Desarrollo y demostración de la saga | Orquestación, balanceo, autoescalado |
| Acceso | Cinco puertos y cada herramienta en el suyo | **Un solo origen**: `http://localhost:8000` |
| CORS | Necesario: es cross-origin | **Innecesario**: el Ingress unifica el origen |
| Logs | Los servicios escriben archivos; Promtail monta la carpeta | Promtail lee `/var/log/pods` del nodo |

No son simultáneos: no caben dos veces en la misma máquina.

**Lo que costó, y es la parte que merece contarse:** cinco fallos que **solo
aparecen en este despliegue**, ninguno con un mensaje que apunte a su causa.

| Síntoma | Causa | Corrección |
|---|---|---|
| Kafka muere al configurarse, log de 4 líneas | Kubernetes inyecta `KAFKA_PORT=tcp://…` por el Service homónimo, y la imagen de Confluent convierte toda variable `KAFKA_*` en propiedad del broker | `enableServiceLinks: false` |
| Kafka no arranca nunca | Bloqueo circular: el broker se conecta a sí mismo por `kafka:9092`, el Service solo enruta a pods *Ready*, y no está *Ready* hasta que arranque | `publishNotReadyAddresses: true` |
| `ErrImageNeverPull` con las imágenes cargadas | Podman antepone `localhost/`; containerd leía `pedidos/x:1.0` como `docker.io/pedidos/x:1.0` | `localhost/pedidos/<servicio>:1.0` |
| `kind load` dice `image not present locally` con la imagen presente | El proveedor experimental de Podman normaliza el nombre de otra forma | `podman save` + `kind load image-archive` |
| `403 Invalid CORS request` al entrar desde el propio Ingress | **Un `POST` manda `Origin` aunque sea del mismo origen**, y Spring lo valida igual | `setAllowedOriginPatterns` con `http://localhost:*` |

El de CORS enseña algo que va más allá de Kubernetes: **una API se verifica
desde el cliente que la va a usar**. Todas las comprobaciones del Ingress hechas
con `curl` daban `200` sobre un camino que el navegador rechazaba, porque curl no
envía la cabecera `Origin`.

Y uno que no era de Kubernetes pero salió ahí: **los cinco `Dockerfile` nunca se
habían llegado a construir.** Copiaban solo `pom.xml`, `shared` y su módulo,
mientras el POM agregador declara los seis, así que Maven abortaba con *«Child
module /build/order-service does not exist»*. Corregido con `COPY . .` y un
`.dockerignore` que deja fuera los `target/`.

**Y la confirmación de lo que [ADR-009](adr/ADR-009-podman-compose.md) afirmaba
sin poder probarlo:** el paso a Kubernetes **no exigió tocar una sola línea de
código de negocio**. Todo fueron manifiestos, dos ajustes de `Dockerfile` y una
variable de entorno en el front. Configuración por entorno, sin estado en
memoria y con `liveness` y `readiness` ya expuestas: eso es lo que lo hizo
posible.

---

# 5. Seguridad

## 5.1 Autenticación y autorización

> **Pide:** *Implementación con JWT para sesiones de usuario. Validación de permisos por roles.*

**Cómo se atendió.** JWT HS256 con clave simétrica compartida
([ADR-011](adr/ADR-011-jwt-clave-simetrica.md)):

- **Usuarios es el único que firma.** Los otros cuatro solo verifican, sin
  consultar a nadie: si Usuarios se cae, los demás siguen atendiendo peticiones
  autenticadas.
- **La verificación vive una sola vez**, en el módulo `shared`.
- **Ningún endpoint de negocio es público en ninguno de los cinco servicios.**

Sobre *«ningún endpoint público»*, que el enunciado plantea en absoluto: no se
puede cumplir literalmente. `/auth/login` no puede exigir el token que aún no
existe, Prometheus raspa métricas cada 15 s sin renovar tokens, y un Swagger que
exija pegar un token a mano es inservible en una demostración. La lista blanca
es **mínima y explícita**:

| Servicio | Rutas públicas |
|---|---|
| Usuarios | `/auth/**`, `/actuator/**`, Swagger |
| Los otros cuatro | `/actuator/**`, Swagger |

**El token se propaga entre servicios**: la llamada de Pedidos a Catálogo
reenvía la cabecera del cliente, de modo que no hay un canal interno sin
autenticar.

**Evidencia:** cinco pruebas por servicio — sin token `401`, token válido `200`,
firma ajena `401`, token caducado `401`, cabecera con basura `401` (y no `500`).

## 5.2 Aislamiento de datos

> *Añadido: sin esto, «una base por servicio» es una intención, no un control.*

Cada base tiene **su propio rol de Postgres sin permisos cruzados**, con
`REVOKE ALL ON DATABASE ... FROM PUBLIC` para cerrar el permiso por defecto.

```bash
podman exec postgres psql -U order_svc -d catalogdb -c "select 1"
# FATAL: permission denied for database "catalogdb"
```

Con bases separadas, un `JOIN` entre contextos **no se puede escribir**. Con
esquemas separados sería una línea de SQL.

## 5.3 Gestión de secretos y limitaciones asumidas

> *Añadido: las limitaciones que no se declaran se descubren en la sustentación.*

| Limitación | Riesgo | Cómo se resolvería |
|---|---|---|
| Clave JWT en claro en los `application.yaml` | Cualquiera con acceso al repositorio la ve | Gestor de secretos; ya se lee por variable de entorno |
| **Clave simétrica: los cinco servicios podrían firmar tokens** | Verificar y firmar son la misma capacidad | RS256 con JWKS: solo el emisor firma |
| Sin revocación de tokens | Un token robado vale una hora | Lista negra en Redis, o tokens de vida corta |
| Contraseñas de BD en claro | Igual que la clave JWT | Igual |
| JWT en `localStorage` del front | Vulnerable a XSS | Cookie `HttpOnly` |
| Usuario de ejemplo con contraseña conocida, sembrado por Flyway | Cualquiera que lea el repositorio puede entrar | Es un dato de demostración; en producción no habría migración de usuarios |

La segunda es la más seria y por eso está resaltada: que solo Usuarios emita
tokens es **una convención del código, no una imposibilidad criptográfica**.

---

# 6. Escalabilidad y Rendimiento

## 6.1 Estrategias de escalabilidad

> **Pide:** *Escalado horizontal automático en Kubernetes por uso de CPU y cola de eventos.*

**Cómo se atendió.** El escalado horizontal **se demostró en ejecución**, y sin
Kubernetes: la unidad de paralelismo de un sistema orientado a eventos no es el
pod, es **la partición**.

Los siete topics tienen **3 particiones**. Al levantar una segunda instancia de
Pagos en el mismo grupo de consumidores, Kafka repartió las particiones solo, a
las `20:27:48`:

| Instancia | Particiones de `pedido.pago-solicitado` |
|---|---|
| 1 (puerto 8084) | 0 y 1 |
| 2 (puerto 8184) | 2 |

Al detener la segunda, las tres volvieron a la primera. **Sin tocar una línea de
código y sin balanceador delante.**

De ahí sale el límite que hay que conocer: con 3 particiones caben **hasta 3
instancias útiles** por grupo; la cuarta se queda ociosa. Y subir el número de
particiones **no es transparente**: cambia a qué partición va cada clave, y con
ello se pierde la garantía de orden por pedido que sostiene la saga.

Los servicios son **sin estado**: no guardan sesión en memoria, así que cualquier
instancia atiende cualquier petición.

*Autoescalado por CPU:* hay manifiestos `HorizontalPodAutoscaler` en
[`k8s/manifiestos/40-hpa.yaml`](../k8s/manifiestos/40-hpa.yaml), con
`maxReplicas: 3` en los consumidores —el número de particiones— y 5 en Catálogo,
cuya carga dominante son lecturas REST y no eventos, así que el techo de
particiones no le aplica.

Ese detalle es el que distingue escalar un servicio web de escalar un consumidor
de eventos: en el primero, más réplicas siempre reparten más carga; en el
segundo, el techo lo pone el número de particiones.

**Verificado en el clúster**, con metrics-server desplegado:

```
catalog-service   cpu: 26%/70%   min 1  max 5
order-service     cpu: 30%/70%   min 1  max 3
payment-service   cpu: 29%/70%   min 1  max 3
```

El parche `--kubelet-insecure-tls` de metrics-server es obligatorio en kind: sin
él no confía en el certificado del kubelet y el HPA se queda en `<unknown>/70%`
indefinidamente, sin decir por qué.

**Evidencia:**
```bash
podman exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group pagos-group
```

## 6.2 Balanceo de carga

> **Pide:** *Nginx y Kubernetes Ingress Controller para balanceo de solicitudes REST.*

**Cómo se atendió.** Con **ingress-nginx sobre Kubernetes** — que es literalmente
lo que pide el enunciado: el controlador *es* nginx, gestionado por Kubernetes en
lugar de configurado a mano.

Hay **tres niveles de reparto**, y conviene no confundirlos porque resuelven
cosas distintas:

| Nivel | Quién reparte | Entre qué |
|---|---|---|
| **7 · HTTP** | El `Ingress` | Decide, según la **ruta**, a qué servicio va cada petición |
| **4 · TCP** | Cada `Service` | Reparte entre las réplicas de ese servicio, sin configurar nada |
| **Eventos** | **Kafka** | Reparte las **particiones** entre los consumidores de un grupo |

El tercero es el que de verdad escala este sistema, porque el grueso del trabajo
va por eventos, no por HTTP.

**Verificado en ejecución**, todo bajo `http://localhost:8000`:

```
GET  /                     -> 200   el front
GET  /api/productos        -> 401   sin token: ningún endpoint de negocio es público
POST /auth/login           -> 200   token para carlos@test.com
GET  /grafana /zipkin /prometheus -> 200 200 200
```

**Y un efecto secundario que enseña algo:** al servir el front y las APIs bajo el
mismo origen, **CORS deja de hacer falta**. En el despliegue de compose es
imprescindible, porque el navegador habla con cinco puertos distintos. CORS no
era un requisito del sistema: era una consecuencia de cómo estaba desplegado.

Para que funcione, el front se construye con `VITE_MISMO_ORIGEN=true` y usa rutas
relativas. Que los prefijos no choquen entre servicios —`/auth` y `/api/usuarios`
son de Usuarios, `/api/productos` de Catálogo, `/api/pedidos` de Pedidos— es lo
que permite enrutar por ruta sin inventar un prefijo artificial.

**Lo que sigue sin haber:** balanceo en el despliegue de compose. Ahí el front
llama a los cinco puertos directamente, y poner nginx delante de una instancia
por servicio sería decorado.

## 6.3 Tolerancia a fallos y alta disponibilidad

> **Pide:** *Replicación de servicios críticos, reintento en servicios consumidores de eventos y circuit breakers.*

De las tres, **dos están implementadas y una no**:

### Reintentos en los consumidores · **sí**

Cuatro entregas en total (la original más tres), con retroceso exponencial de 2,
4 y 8 segundos, distinguiendo el tipo de fallo: los deterministas —producto
inexistente, transición imposible— van directos a la cola de fallidos sin gastar
14 segundos. Todo lo que agota reintentos queda en `failed_events` con topic de
origen, offset, intentos, error y payload, consultable en `GET /api/admin/dlq`.

Incluye protección contra el **poison pill**: `ErrorHandlingDeserializer`, sin el
cual un JSON inválido bloquea la partición para siempre.

### Circuit breaker · **sí**, en el único sitio donde tiene sentido

Resilience4j sobre la única llamada síncrona, Pedidos → Catálogo. Sin él, con
Catálogo caído cada petición espera los 3 s del timeout antes de fallar, y bajo
carga esas esperas se apilan hasta agotar el pool de hilos: **un servicio caído
se lleva por delante a otro que está sano.**

El detalle que hace que funcione: **que un producto no exista NO cuenta como
fallo del circuito**. Es una respuesta correcta a una pregunta mal hecha. Sin esa
exclusión, unos cuantos productos inexistentes abrirían el circuito y bloquearían
las compras válidas.

Y el respaldo **no inventa un producto ni un precio por defecto**: un pedido con
un precio falso es peor que un pedido que no se crea.

### Replicación · **no**

Un solo broker Kafka con `replicas = 1`, un solo Postgres, una instancia por
servicio. Perder el broker es perder los eventos no consumidos. En producción
serían 3 brokers y factor de replicación 3, **sin tocar una línea de código**.

### Lo que sí protege el sistema hoy

| Fallo | Qué pasa |
|---|---|
| Pagos caído | Los pedidos se siguen creando; el cobro se procesa al volver |
| Catálogo caído | No se crean pedidos, con `503` explícito; el circuito falla rápido |
| Un evento falla | 4 intentos con retroceso; luego a la DLQ, sin perderse |
| JSON corrupto | La partición no se bloquea |
| Evento duplicado | Cada consumidor tiene su estrategia de idempotencia |
| Pedidos caído | La saga se detiene, no se pierde: los eventos esperan en Kafka |

**La limitación honesta:** existe una ventana en la que un evento puede perderse
—entre el `commit` de Postgres y la publicación en Kafka— porque no se implementó
el patrón outbox. Está declarada, acotada y documentada en
[ADR-007](adr/ADR-007-sin-outbox.md), con los puntos exactos del código marcados.

---

# 7. DevOps y Despliegue (OPCIONAL)

> **Pide:** *Pipeline en GitHub Actions · Helm Charts · ambientes de desarrollo, staging y producción.*

**No se aplica, por decisión de alcance.** El punto es opcional.

| Lo que pide | Estado |
|---|---|
| 7.1 CI/CD con GitHub Actions | No se implementa |
| 7.2 Infraestructura como código con Helm | **Parcialmente cubierto.** Sin Helm, pero con **14 manifiestos de Kubernetes versionados** en [`k8s/`](../k8s/), la configuración del clúster kind y el `docker-compose.yml`. Todo el despliegue se reconstruye desde el repositorio |
| 7.3 Tres ambientes | Uno solo: desarrollo local |

Lo que sí se cumple del espíritu del punto: **la configuración no está incrustada
en las imágenes**. `DB_URL`, `KAFKA_SERVERS`, `JWT_SECRET`, `LOG_LEVEL` y el
resto se leen por variable de entorno con valor por defecto, que es el tercero
de los doce factores y el requisito previo de cualquier pipeline.

---

# 8. Calidad y Mantenibilidad

## 8.1 Estrategias de pruebas

> **Pide:** *Pruebas unitarias y de integración.*

**125 pruebas automáticas, en cuatro niveles**, todas en verde:

| Nivel | Qué prueba | Sin arrancar |
|---|---|---|
| **Dominio puro** | Máquinas de estados, cálculo de totales, invariantes | Spring, BD, Kafka |
| **Casos de uso** | La saga completa, con dobles escritos a mano (`Fakes.java`) | Spring, BD, Kafka |
| **`@WebMvcTest`** | Contrato HTTP: códigos, validación, mapeo de excepciones | BD, Kafka |
| **`@DataJpaTest`** | Mapeo agregado ↔ entidad, restricciones, orden | Spring completo, Kafka |

| Servicio | Pruebas |
|---|---|
| Pedidos | 48 |
| Catálogo | 21 |
| Pagos | 19 |
| Entregas | 19 |
| Usuarios | 18 |
| **Total** | **125** |

**Dos limitaciones declaradas, y la segunda es importante:**

**H2 no es Postgres.** Las pruebas `@DataJpaTest` corren sobre H2 en modo
compatible. Una diferencia de dialecto podría pasar desapercibida; lo cubre que
los servicios arranquen contra Postgres con `ddl-auto: validate`.

**Ninguna prueba arranca Kafka.** Y eso no es teórico: la cola de mensajes
fallidos estuvo rota de cuatro maneras encadenadas **con 109 pruebas en verde**.
Cubrirlo exigiría Testcontainers, que se dejó fuera del alcance. La lección
quedó escrita en [ADR-006](adr/ADR-006-errores-reintentos-dlq.md):

> Cuando el último recurso falla, no hay último recurso: hay un bucle.

**Lo verificado en ejecución**, que es lo que compensa esa limitación:

| Qué | Resultado |
|---|---|
| Camino feliz completo | `CREADO` → `PAGO_EN_PROCESO` → `PAGADO` → `EN_PREPARACION` → `EN_CAMINO` → `ENTREGADO` |
| Latencia de la saga | **323 ms** desde el clic en pagar hasta `EN_PREPARACION`, cruzando Kafka 4 veces |
| Compensación | Importe alto → `RECHAZADO`, sin reembolso ni reposición de stock |
| Idempotencia | Evento repetido ignorado, sin doble cobro |
| Fidelidad | +42 puntos por un pedido de S/ 42.50 |
| DLQ | Stock insuficiente → entrada con topic, offset, intentos y payload |
| Escalado | Rebalanceo de particiones entre dos instancias |

## 8.2 Observabilidad

> **Pide:** *Prometheus para métricas, Grafana para dashboards y ZipKin para seguimiento de transacciones entre microservicios.*

*(El enunciado escribe «ZipKin»; el nombre correcto es Zipkin.)*

**Cómo se atendió.** Las tres, más una cuarta que el enunciado no pide y que
resultó decisiva para depurar:

| Herramienta | Para qué | Puerto |
|---|---|---|
| **Prometheus** | Métricas de los 5 servicios | 9090 |
| **Grafana** | Tableros y consulta unificada | 3000 |
| **Zipkin** | Trazas distribuidas | 9411 |
| **Loki + Promtail** | **Logs centralizados** de los 5 servicios | 3100 |

**Lo que hace que esto sirva de verdad:** la traza **cruza el broker**. Con
`observation-enabled: true` en el template y el listener de Kafka, un pedido
aparece en Zipkin como una sola cascada, no como trazas sueltas. Verificado: el
mismo `traceId` (`6a7bc3ae009d728b54aecff801bb2ef1`) en dos servicios distintos,
a un lado y otro del broker.

Los logs son **JSON en formato ECS** con `traceId` y `spanId` en cada línea, lo
que permite pasar de una traza a sus logs exactos.

**En el despliegue de Kubernetes, la observabilidad mejora**, y no es una
traducción del compose sino un cambio de enfoque:

| | Compose | Kubernetes |
|---|---|---|
| Recogida de logs | Los servicios escriben en `./logs`; Promtail monta la carpeta | **Promtail como DaemonSet** lee `/var/log/pods` del nodo |
| Etiquetado | Por nombre de archivo | Por la API de Kubernetes: `servicio`, `pod`, `nivel` |
| Requisito para los servicios | Escribir archivos y compartir volumen | **Ninguno**: basta con la salida estándar |

**El salto log → traza queda resuelto** con un *campo derivado* en Grafana:
extrae el `traceId` de cada línea de Loki y lo convierte en enlace directo a esa
traza en Zipkin.

Verificado en el clúster: **Prometheus descubre y raspa los cinco servicios**,
los cinco `up`, y las tres interfaces responden bajo subruta del mismo origen
(`/grafana`, `/zipkin`, `/prometheus`).

Cada herramienta necesita saber que vive bajo un prefijo o genera enlaces
absolutos a `/` y su interfaz se rompe al navegar: de ahí
`GF_SERVER_SERVE_FROM_SUB_PATH`, `ZIPKIN_UI_BASEPATH` y `--web.external-url`.

---

# 9. Anexos y Referencias (OPCIONAL)

## 9.1 Glosario

> **Pide:** *Incluye términos como microservicio, broker de eventos, CI/CD, etc.*

Definidos, no solo enumerados. Se marcan **en negrita** los que son propios de
este sistema y no del vocabulario general.

| Término | Definición |
|---|---|
| **Microservicio** | Servicio con proceso, datos y despliegue propios, cuya frontera coincide con un contexto delimitado |
| **Contexto delimitado** | Zona del dominio con lenguaje propio y coherente. Aquí hay cinco |
| **Arquitectura hexagonal** | El dominio en el centro; todo lo externo entra por puertos e implementa adaptadores |
| **Puerto** | Interfaz que declara el dominio. De entrada (`CrearPedidoUseCase`) o de salida (`PedidoRepository`) |
| **Adaptador** | Implementación de un puerto contra una tecnología concreta |
| **Agregado** | Grupo de objetos con una raíz que garantiza sus invariantes. Aquí: `Pedido`, `Pago`, `Entrega` |
| **Broker de eventos** | Intermediario que almacena y reparte eventos. Aquí, Kafka |
| **Topic** | Canal con nombre dentro del broker. Aquí hay siete |
| **Partición** | Subdivisión de un topic. **Es la unidad de paralelismo**: 3 particiones = hasta 3 consumidores útiles |
| **Clave de partición** | Valor que decide a qué partición va un evento. Aquí, el id del pedido, para garantizar el orden |
| **Grupo de consumidores** | Instancias que se reparten las particiones de un topic |
| **Saga** | Secuencia de transacciones locales con compensación, cuando no hay transacción común |
| **Saga orquestada** | Un servicio decide el orden de los pasos. Aquí, Pedidos |
| **Saga coreografiada** | Cada servicio reacciona a los eventos de los demás, sin coordinador |
| **Compensación** | Acción de negocio que deshace un paso anterior. No es un `ROLLBACK` |
| **`huboCobro`** | Campo de `pedido.cancelado` que indica si hay dinero que devolver |
| **Idempotencia** | Que procesar dos veces el mismo evento tenga el efecto de procesarlo una |
| **Entrega al menos una vez** | Garantía de Kafka: un evento puede repetirse. Obliga a la idempotencia |
| **DLQ / DLT** | Cola de mensajes fallidos, tras agotar los reintentos |
| **Poison pill** | Mensaje que falla al deserializarse, antes del listener. Bloquea la partición si no se protege |
| **Circuit breaker** | Corta las llamadas a un servicio que falla, para fallar rápido en vez de esperar el timeout |
| **Patrón outbox** | Escribir el evento en la misma transacción que los datos, y publicarlo después. **No implementado** ([ADR-007](adr/ADR-007-sin-outbox.md)) |
| **Doble escritura** | Escribir en BD y publicar en el broker sin transacción común. El problema que resuelve el outbox |
| **ADR** | Registro de una decisión de arquitectura, con contexto, alternativas y consecuencias |
| **JWT** | Token firmado que transporta la identidad. Aquí HS256 con clave compartida |
| **CI/CD** | Integración y entrega continuas. **No se aplica** en este trabajo |
| **Consistencia eventual** | Los datos convergen, pero no al instante. Es el precio de no tener transacción distribuida |

## 9.2 Referencias y normativas

> **Pide:** *Guía de 12 factores, documentación de OpenAPI, prácticas de DevSecOps.*

| Referencia | Uso en este trabajo |
|---|---|
| [The Twelve-Factor App](https://12factor.net/) | Configuración por entorno (III), servicios de respaldo (IV), procesos sin estado (VI), logs a stdout (XI) |
| [OpenAPI 3.1](https://spec.openapis.org/oas/latest.html) | Contratos de los 5 servicios, generados con springdoc |
| Evans, *Domain-Driven Design* | Contextos delimitados, agregados, repositorios |
| Richardson, *Microservices Patterns* | Saga, base por servicio, outbox, API composition |
| Cockburn, *Hexagonal Architecture* | Puertos y adaptadores |
| Nygard, *Documenting Architecture Decisions* | Formato de los 13 ADR |
| [Documentación de Apache Kafka](https://kafka.apache.org/documentation/) | Particiones, grupos, garantías de entrega |
| [Spring for Apache Kafka](https://docs.spring.io/spring-kafka/reference/) | `@RetryableTopic`, `@DltHandler`, `ErrorHandlingDeserializer` |
| [Resilience4j](https://resilience4j.readme.io/) | Circuit breaker |

**Sobre DevSecOps:** no se aplica como práctica, pero sí tres controles que le
pertenecen: ningún endpoint de negocio público, aislamiento de datos por roles
de base de datos, y **declaración explícita de las limitaciones de seguridad**
en §5.3 en lugar de silenciarlas.

## 9.3 Documentación técnica relacionada

> **Pide:** *Enlaces a repositorios, Swagger UI, y documentación de infraestructura.*

**Repositorio:** `git@github.com:carlosormeno/tecsup_tarea_m5.git`

**Documentos del proyecto:**

| Documento | Contenido |
|---|---|
| [docs/adr/](adr/README.md) | Los 13 ADR, con índice y grafo de relaciones |
| [SERVICIOS_Y_FLUJO.md](SERVICIOS_Y_FLUJO.md) | Responsabilidades y los tres diagramas |
| [MAPA_CODIGO.md](MAPA_CODIGO.md) | De cada concepto al archivo que lo implementa |
| [SEGUIMIENTO.md](SEGUIMIENTO.md) | Decisiones, checklist, evidencias y bitácora |
| [k8s/](../k8s/) | Manifiestos de Kubernetes y configuración de kind |

**Interfaces, según el despliegue.** Los dos están verificados y no se levantan
a la vez: no caben dos veces en la misma máquina. En Kubernetes todo cuelga de un
único origen gracias al Ingress; en compose, cada cosa vive en su puerto.

| Interfaz | Kubernetes (kind) | Podman Compose |
|---|---|---|
| **Front** | http://localhost:8000 | http://localhost:5173 |
| **APIs** | `http://localhost:8000/api/…` | `http://localhost:808x/api/…` |
| Swagger por servicio | `kubectl port-forward -n pedidos svc/order-service 8083:8083` | `http://localhost:808x/swagger-ui.html` |
| **Grafana** | http://localhost:8000/grafana | http://localhost:3000 |
| **Zipkin** | http://localhost:8000/zipkin | http://localhost:9411 |
| **Prometheus** | http://localhost:8000/prometheus | http://localhost:9090 |
| Loki | *sin interfaz propia; se consulta desde Grafana* | *igual* |
| kafka-ui | *no desplegado* | http://localhost:8090 |
| pgAdmin | *no desplegado* | http://localhost:5050 |
| Postgres | `localhost:30432` (NodePort) | `localhost:5432` |
| Kafka | `localhost:30092` (NodePort) | `localhost:9092` |

Los puertos de servicio son 8081 Usuarios, 8082 Catálogo, 8083 Pedidos, 8084
Pagos y 8085 Entregas.

**kafka-ui y pgAdmin no se llevaron a Kubernetes** a propósito: son herramientas
de inspección durante el desarrollo, no parte del sistema. En el clúster se
llega a lo mismo con `kubectl` y los NodePort de arriba.

**Credenciales de demostración:**

| Para | Usuario | Contraseña |
|---|---|---|
| El front | `carlos@test.com` | `password123` |
| Grafana | `admin` | `admin` |
| pgAdmin *(solo compose)* | `admin@admin.com` | `admin` |

El usuario del front lo siembra Flyway (`V2__datos_ejemplo.sql` de
`user-service`), así que sobrevive a un `podman-compose down -v` y no hay que
registrarlo a mano antes de una demostración.

---

## Cierre: lo que este trabajo no hace

Se agrupa aquí lo que ya está declarado en cada punto, para que quede en un solo
sitio y no haya que buscarlo:

| No implementado | Punto | Justificación |
|---|---|---|
| Integración con Stripe y geolocalización | 3.4 | Opcional; no ejercita ningún patrón nuevo |
| Balanceo de carga HTTP **en el despliegue de compose** | 6.2 | En Kubernetes sí lo hay; en compose, con una instancia por servicio, sería decorado |
| Replicación de broker y base de datos | 6.3 | Un nodo; en producción, sin cambiar código |
| Patrón outbox | 6.3 | Ventana acotada, fallo detectable, coste alto |
| CI/CD, Helm, tres ambientes | 7 | Opcional, fuera del alcance |
| Pruebas con Testcontainers | 8.1 | Fuera del alcance; es la limitación más costosa del trabajo |
| Helm | 7.2 | Con un solo entorno, un chart sobrevuela 14 archivos que ya se leen bien |
| Tolerancia a fallos de nodo | 4.2 | Un solo nodo en kind |

Ninguna de estas ausencias impide que el sistema funcione de principio a fin.
Todas están donde deben estar: escritas, con su motivo y con la forma de
resolverlas.
