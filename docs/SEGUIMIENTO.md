# Seguimiento del Trabajo Final

Documento vivo. Relaciona cada sección de
[`TrabajoFinal_Arquitectura_Microservicios_Pedidos_Comida.md`](../TrabajoFinal_Arquitectura_Microservicios_Pedidos_Comida.md)
con las decisiones tomadas, el código que las implementa y lo que falta.

**Documentos complementarios:**
- [SERVICIOS_Y_FLUJO.md](SERVICIOS_Y_FLUJO.md) — qué hace cada servicio y cómo
  se combinan para completar un pedido. Con diagramas. Alimenta las secciones
  2.3, 3.1 y 3.3 del documento de arquitectura.
- [MAPA_CODIGO.md](MAPA_CODIGO.md) — dónde está implementado cada concepto,
  archivo por archivo.

**Última actualización:** 2026-08-09

---

## 1. Decisiones cerradas

| # | Decisión | Detalle | ¿ADR? |
|---|---|---|---|
| 1 | Estilo | Microservicios, con **arquitectura hexagonal pura** dentro de cada servicio | Sí |
| 2 | Servicios | 5: Usuarios, Catálogo, Pedidos, Pagos, Entregas | Sí |
| 3 | Base de datos | **Un solo motor Postgres** con 5 bases y 5 roles sin permisos cruzados | Sí |
| 4 | Broker | Kafka con Zookeeper (`confluentinc/cp-*:7.5.0`) + kafka-ui | — |
| 5 | Eventos | **7 topics**, saga **orquestada** por Pedidos, con mensajería de estilo evento (no comandos). Ver [MAPA_CODIGO.md](MAPA_CODIGO.md#1-tipo-de-saga) | Sí |
| 6 | Acoplamiento síncrono | Una sola llamada REST entre servicios: Pedidos → Catálogo, para validar precio y disponibilidad | Sí |
| 7 | Errores | `@RetryableTopic` + `@DltHandler`, **una DLT por servicio**, `ErrorHandlingDeserializer` contra poison pill | Sí |
| 8 | Idempotencia | Solo donde el duplicado corrompe datos: descuento de stock en Catálogo | Sí |
| 9 | Outbox | **No se implementa.** Se documenta la ventana de inconsistencia y cómo se resolvería | Sí |
| 10 | Librería compartida | **Se comparte la fontanería técnica, nunca los contratos.** Módulo `shared` con DLQ y validación de JWT; eventos, DTOs, modelos y puertos los define cada servicio. Ver detalle abajo | Sí |
| 11 | Integraciones externas | **Ninguna.** Sin Stripe, sin geolocalización. La sección 3.4 se elimina | — |
| 12 | Orquestación | **Podman Compose.** Kubernetes queda como evolución futura, fuera del alcance | Sí |
| 13 | Stack | Java 21, Spring Boot 3.5.6, Maven | — |
| 14 | Front | React básico, consume las APIs REST. No es microservicio | — |
| 15 | Documentación de API | springdoc-openapi 2.8.6 (Swagger UI) | — |
| 16 | Esquema de BD | Flyway con migraciones versionadas por servicio | — |
| 17 | Observabilidad | Prometheus (métricas) + Zipkin (trazas) + Loki/Promtail (logs) + Grafana | — |
| 18 | Trazas sobre Kafka | `observation-enabled: true` en template y listener, para que la traza cruce el broker | — |
| 19 | Estructura de paquetes | **La del proyecto de Módulo 3** (`arq_m3_s3_eda_intro_lms`), ver detalle abajo | — |
| 20 | Disparo de la saga | **El pago es un paso explícito del cliente**, no un efecto de crear el pedido. Ver detalle abajo | — |

### Detalle de 20 — Por qué el pago es un paso aparte

Hasta el 2026-08-12, `POST /api/pedidos` creaba el pedido **y** publicaba el
evento que hacía cobrar a Pagos: crear un pedido lo pagaba. Se cambió porque no
se corresponde con el negocio (en cualquier tienda se confirma el pedido y
luego se paga) y porque dejaba una costura fea en el modelo: el estado `CREADO`
significaba a la vez «recién hecho» y «esperando la respuesta del cobro», de
modo que un pedido rechazado pasaba de `CREADO` a `RECHAZADO` sin que quedara
rastro de que hubo un intento de pago.

| | Antes | Ahora |
|---|---|---|
| `POST /api/pedidos` | crea y publica `pedido.creado` | solo crea; **no publica nada** |
| Pagar | automático | `POST /api/pedidos/{id}/pagar` → `PAGO_EN_PROCESO` + `pedido.pago-solicitado` |
| Topic que dispara el cobro | `pedido.creado` | `pedido.pago-solicitado` |
| Estados | `CREADO → PAGADO → …` | `CREADO → PAGO_EN_PROCESO → PAGADO → …` |

Tres cosas que salieron gratis con el estado nuevo:

1. **El doble clic en «pagar» ya no publica dos eventos.** El segundo intento
   choca contra una transición inexistente y devuelve `409`. La protección
   vive en el dominio, no en el navegador.
2. **Nadie puede dar un pedido por pagado saltándose a Pagos**: el único camino
   a `PAGADO` pasa por `PAGO_EN_PROCESO`.
3. **`huboCobro` dejó de ser una deducción frágil.** Antes era
   `!estaEn(CREADO)`; ahora es `EstadoPedido.implicaCobro()`, que dice
   explícitamente a partir de qué estado hay dinero que devolver.

El evento se renombró en lugar de reutilizar `pedido.creado` porque ya no se
publica al crear: un nombre que miente sobre cuándo ocurre el hecho es peor que
renombrar dos constantes y un DTO. `pedido.creado` desaparece del sistema; no
queda ningún topic sin consumidor.

**Rastro que queda:** el comentario de `V1__crear_tablas.sql` de Pagos sigue
citando `pedido.creado`. Se dejó a propósito: **una migración ya aplicada no se
edita**, ni siquiera en un comentario, porque Flyway valida el checksum del
archivo completo y el arranque fallaría contra cualquier base existente.

### Detalle de 19 — Estructura de paquetes por servicio

```
application/                 XxxUseCase (interfaz) + XxxUseCaseImpl, juntos
domain/
  model/ event/ exception/
  repository/                puerto de salida de persistencia (Repository de DDD)
  client/                    puertos de salida hacia otros servicios
infrastructure/
  persistence/               adapter/ entity/ mapper/ repository/
  web/                       controller/ dto/
  messaging/                 publicador, Topics, listener/, dto/
  client/ config/ security/ dlq/
```

`BeanConfiguration` registra cada implementación detrás de su interfaz: el controlador depende del puerto, nunca de la clase concreta.

**Tres cosas del ejemplo de Módulo 3 que NO se copian**, porque rompen la pureza:
1. `KafkaEventPublisher` y `RabbitMQEventPublisher` viven ahí en `shared/domain/event/` — son infraestructura dentro del dominio.
2. `CreateCourseUseCaseImpl` inyecta esas clases concretas, no solo el puerto `EventPublisher`.
3. Los `@KafkaListener` están en `application/eventhandler/` y `application/saga/` — anotaciones de Kafka en la capa de aplicación.

**Verificación**, repetible en cada servicio:
```
grep -rn "import com.tecsup.*\(application\|infrastructure\)" domain/     # vacío
grep -rn "import \(org.springframework\|jakarta.persistence\|org.apache.kafka\)" domain/   # vacío
grep -rn "import com.tecsup.*infrastructure" application/                 # vacío
```
Única excepción admitida: `application` importa `@Transactional`.

---

## 2. Decisiones pendientes

| # | Tema | Estado |
|---|---|---|
| P1 | Seguridad | **Cerrado.** JWT con clave simétrica compartida, emitido por Usuarios y validado por los demás. Ningún endpoint **de negocio** es público. Validación servicio a servicio propagando el JWT del cliente. Ver detalle abajo |
| P2 | Pruebas (8.1) | **Cerrado.** Tres niveles: dominio puro, `@WebMvcTest` y `@DataJpaTest`. Sin pruebas de contrato y sin Testcontainers |
| P3 | CI/CD | **Cerrado: no se aplica.** La sección 7 completa se elimina del documento |
| P4 | Repositorio git | **Cerrado.** `git@github.com:carlosormeno/tecsup_tarea_m5.git` por SSH (acceso verificado). `.gitignore` escrito. Commit pendiente de revisión |
| P5 | Datos de ejemplo | **Cerrado: sí.** `V2__datos_ejemplo.sql` en catalog-service y user-service (Pedidos no lleva semilla: los pedidos los crea el usuario) |
| P6 | Alcance del front | **Cerrado a grandes rasgos:** carrito de compras con usuario, catálogo, carrito, checkout y seguimiento. El detalle de pantallas se afina sobre la marcha |

### Detalle de 10 — Qué se comparte y qué no

| | Compartir | Motivo |
|---|---|---|
| Eventos, DTOs de contrato, modelos de dominio, puertos | **Nunca** | Son el contrato de cada servicio. Compartirlos crea el monolito distribuido: cambiar un campo obliga a recompilar y redesplegar los cinco |
| Fontanería técnica sin significado de negocio | **Sí** | Un validador de JWT o un escritor de DLQ no dicen nada del dominio. Es lo que hace cualquier *starter* de Spring Boot |

**Contenido de `services/shared`** (7 clases, cero referencias al negocio):

```
dlq/       FailedEvent, FailedEventRepository, DeadLetterQueue, DLQController
security/  JwtTokenProvider, JwtAuthenticationFilter, SecurityConfig
```

**Regla para añadir algo:** si menciona pedidos, pagos, entregas o cualquier
concepto del negocio, no va ahí.

**Consecuencias que hubo que resolver:**

- `SecurityConfig` no era del todo idéntica: user-service necesita abrir
  `/auth/**`. Se resolvió con la lista blanca configurable
  `seguridad.rutas-publicas`.
- `FailedEvent` es una `@Entity` fuera del paquete del servicio: hace falta
  `@EntityScan` y `@EnableJpaRepositories` apuntando a `com.tecsup.app.micro`.
- Esas dos anotaciones **no pueden ir en la clase `Application`**: no son
  condicionales y romperían las pruebas `@WebMvcTest`, que no levantan JPA.
  Van en una `@Configuration` aparte (`infrastructure/config/JpaConfig`), que
  el filtro de tipos de la rodaja sí descarta.
- Los `Dockerfile` cambian de contexto: pasa a ser `./services` y se construye
  con `mvn -pl <servicio> -am`.

### Detalle de P1 — Seguridad

**Lista blanca, lo único sin JWT:**

| Ruta | Por qué no puede exigir token |
|---|---|
| `POST /auth/login`, `POST /auth/registro` | Pedir un JWT para obtener un JWT es imposible |
| `/actuator/**` | Prometheus raspa cada 15 s y el JWT caduca en 1 h |
| `/swagger-ui.html`, `/v3/api-docs/**` | Inutilizable en la sustentación si va autenticado |

Todo `/api/**` exige token válido. Mitigación de lo anterior: actuator y Swagger solo son alcanzables dentro de la red de Podman.

**Límites asumidos, a documentar en la sección 5:**
- Los eventos de Kafka **no llevan JWT**. El broker es infraestructura de confianza dentro de la red; la autenticación vive en el borde REST.
- La clave simétrica es compartida por los 5 servicios: cualquiera de ellos puede *emitir* tokens, no solo validarlos. Un par asimétrico (Usuarios firma con la privada, el resto valida con la pública) lo evitaría. Se asume por simplicidad y se declara.
- Credenciales en claro en el compose y en los `application.yaml`. Aceptable en desarrollo; en producción irían a un gestor de secretos.

### Detalle de P2 — Alcance de las pruebas

| Nivel | Herramienta | Qué cubre |
|---|---|---|
| Dominio | JUnit puro | Máquina de estados, invariantes del agregado, saga con adaptadores falsos |
| Entrada REST | `@WebMvcTest` | Serialización, validación y traducción de excepciones a códigos HTTP |
| Persistencia | `@DataJpaTest` sobre H2 | Mapeo de ida y vuelta entre agregado y entidad JPA |

**Riesgo asumido:** sin pruebas de contrato, un cambio en el nombre de un campo de un evento no se detecta al compilar — se descubre en ejecución, cuando el consumidor no encuentra el dato. La mitigación es el mapeo explícito de tipos en `application.yaml`, que al menos deja el contrato escrito en un solo sitio por servicio.

Tampoco se valida que `V1__crear_tablas.sql` coincida con las entidades: eso solo lo comprueba Hibernate con `ddl-auto: validate` al arrancar contra Postgres de verdad.

---

## 3. Mapa: secciones del documento ↔ cómo se atacan

Leyenda: **OK** listo · **REV** hay que reescribir · **NUEVO** hay que crearlo · **QUITAR** se elimina

| Sección | Estado | Qué se hace |
|---|---|---|
| 1.1 Propósito | REV | Añadir portada con autor, fecha y curso |
| 1.2 Alcance | REV | Precisar: 5 servicios + front, sin integraciones externas, despliegue local con Podman |
| 1.3 Audiencia | OK | Sin cambios |
| 2.1 Estilo | REV | Añadir que cada servicio es hexagonal por dentro |
| 2.2 Decisiones clave | REV | Convertir en resumen con enlaces a los ADR de `docs/adr/` |
| 2.3 Diagramas | NUEVO | **Listos** en [SERVICIOS_Y_FLUJO.md](SERVICIOS_Y_FLUJO.md): contenedores, secuencia del camino feliz y de la compensación |
| 3.1 Módulos | REV | **Listo** en [SERVICIOS_Y_FLUJO.md §2](SERVICIOS_Y_FLUJO.md): por servicio, qué posee, expone, publica, consume y **no hace** |
| 3.2 Interfaces y APIs | REV | Enumerar endpoints por servicio y enlazar Swagger UI |
| 3.3 Comunicación | REV | **Listo** en [SERVICIOS_Y_FLUJO.md §3-5](SERVICIOS_Y_FLUJO.md): flujo completo, compensación y reglas de quién habla con quién |
| 3.4 Integración externa | QUITAR | Sin sistemas externos |
| 4.1 | NUEVO | Falta: justificación del estilo elegido y alternativas descartadas |
| 4.2 Microservicios | REV | Quitar Kubernetes; describir Podman Compose |
| 5.1 Autenticación | REV | JWT simétrico emitido por Usuarios, validado por el resto. Lista blanca mínima (ver detalle de P1) |
| 5.2 | NUEVO | Aislamiento de datos: un rol Postgres por servicio, `REVOKE ... FROM PUBLIC` |
| 5.3 | NUEVO | Gestión de secretos y limitaciones asumidas en desarrollo |
| 6.1 Escalabilidad | REV | Sin K8s: réplicas de consumidores por grupo, particiones de Kafka como unidad de paralelismo |
| 6.2 Balanceo | REV | Quitar Nginx/Ingress; el reparto entre consumidores lo hace Kafka por particiones |
| 6.3 Tolerancia a fallos | REV | Reintentos con backoff, DLQ por servicio, idempotencia focalizada, poison pill |
| 7.1–7.3 DevOps | QUITAR | No se aplica CI/CD (P3) |
| 8.1 Pruebas | REV | Cuatro niveles: dominio puro, `@WebMvcTest`, `@DataJpaTest`, contrato. Declarar la limitación de H2 frente a Postgres |
| 8.2 Observabilidad | REV | Cuatro herramientas y correlación traza↔log por `traceId`. Corregir "ZipKin" → "Zipkin" |
| 9.1 Glosario | REV | Definir los términos, no solo anunciarlos |
| 9.2 Referencias | OK | Ajuste menor |
| 9.3 Doc. relacionada | REV | Enlaces reales: Swagger UI, kafka-ui, Grafana, Zipkin |

---

## 4. Checklist de entregables

### Infraestructura
- [x] `docker-compose.yml` con Postgres, Kafka, Zookeeper, kafka-ui, Prometheus, Grafana, Zipkin, Loki, Promtail
- [x] Script de creación de las 5 bases con rol propio
- [x] Configuración de Prometheus, Grafana (datasources) y Promtail
- [ ] Dashboard de Grafana en `infra/observability/grafana/provisioning/dashboards/json/`
- [ ] Los 5 servicios y el front añadidos al compose
- [x] Verificar que todo levanta con `podman-compose up -d` — **10 contenedores arriba, 5 bases con rol propio creadas** (2026-08-10)

### Microservicios
- [x] **order-service** (Pedidos) — **35 pruebas en verde** y **verificado contra infraestructura real** (2026-08-10). Ver evidencias abajo
- [x] **catalog-service** (Catálogo) — **21 pruebas en verde**, hexágono verificado, contratos REST y de evento alineados con order-service. Incluye datos de ejemplo por Flyway
- [x] **payment-service** (Pagos) — **19 pruebas en verde**, hexágono verificado, contratos de evento alineados con order-service. Falta probarlo en ejecución
- [x] **delivery-service** (Entregas) — **16 pruebas en verde**, hexágono verificado, contrato de evento alineado con order-service. Reparto de carga entre repartidores
- [x] **user-service** (Usuarios) — **18 pruebas en verde**. Único emisor de JWT, BCrypt tras un puerto, idempotencia de puntos con tabla
- [ ] Swagger accesible en los 5
- [ ] Métricas en `/actuator/prometheus` en los 5
- [ ] DLQ consultable por REST en los 5

### Front
- [x] Proyecto React (Vite 8, sin router ni librería de estado)
- [x] Pantallas: acceso, catálogo, carrito con cantidades, mis pedidos con botón de pagar, panel de repartidor

### Documentación
- [x] **Actualizar [MAPA_CODIGO.md](MAPA_CODIGO.md) al terminar cada servicio** (tabla comparativa + los tres `grep` de verificación). Forma parte de dar un servicio por terminado, no es un extra
- [x] **Los 11 ADRs en [docs/adr/](adr/README.md)** (ver sección 5), con índice y grafo de relaciones
- [ ] Documento de arquitectura actualizado según la tabla de arriba
- [ ] `plantilla_requisitos.md` completa
- [ ] README con instrucciones para levantar el sistema

### Evidencias ya obtenidas (2026-08-10)

| Qué se probó | Cómo se comprobó | Resultado |
|---|---|---|
| Las 5 bases con rol propio | `psql -c "\l"` y `"\du"` | Creadas |
| Flyway migró `orderdb` | `select ... from flyway_schema_history` | `version 1, success = t` |
| El esquema de Flyway coincide con las entidades | El servicio arrancó con `ddl-auto: validate` | Sin errores |
| Seguridad: sin token | `GET /api/pedidos` sin cabecera | **401** |
| Seguridad: con token válido | `GET /api/pedidos` con `Bearer` | **200 `[]`** |
| Circuito REST → JPA → Postgres | `GET /api/pedidos` y `GET /api/admin/dlq` | Dos `[]` |
| Topics creados al arrancar | kafka-ui en el 8090 | Los 4 `pedido.*` |

### Saga verificada de extremo a extremo (2026-08-11)

Con `order-service` y `payment-service` corriendo contra Kafka y Postgres reales.

**Camino feliz** — pedido de 71.80, por debajo del límite de 500:

| Momento | Servicio | Qué pasó |
|---|---|---|
| `00:50:44.333` | order | Pedido `CREADO`, publica `pedido.creado` |
| `00:50:44.622` | payment | Pago `APROBADO`, publica `pago.confirmado` |
| `00:50:44.705` | order | Pedido `PAGADO` |

La prueba de que el evento viajó bien: la referencia `tx-adad24c2-...` la generó
Pagos y aparece en el campo `motivo` del pedido en order-service, **sin ninguna
llamada HTTP entre ambos**. Eso valida el mapeo de tipos, los DTOs por servicio
y toda la deserialización.

**Compensación** — pedido de 718.00, por encima del límite:

| Momento | Servicio | Qué pasó |
|---|---|---|
| `00:51:58.060` | order | Pedido `CREADO`, publica `pedido.creado` |
| `00:51:58.079` | payment | Pago `RECHAZADO`, publica `pago.rechazado` |
| `00:51:58.099` | order | Pedido `RECHAZADO`, publica `pedido.cancelado` |
| `00:51:58.119` | payment | Consume la cancelación, ve el pago en `RECHAZADO` y **no reembolsa** |

Ese último paso es la tolerancia de `ReembolsarPagoUseCaseImpl`: no lanza
excepción porque no hay nada que devolver. Si lanzara, cada cancelación de un
pedido impago acabaría en la DLQ sin motivo.

**Latencia de la saga: ~39 ms** en régimen. El primer pedido midió 372 ms
porque incluía el arranque en frío del grupo de consumidores (asignación de
particiones y primer *poll*); el dato representativo es el segundo.

**Trazas a través del broker:** el `traceId` `6a7bc3ae009d728b54aecff801bb2ef1`
nació en el `POST /api/pedidos` de order-service y aparece en los logs de
payment-service tras dos saltos por Kafka. Consultable en Zipkin.

### Camino feliz completo con 4 servicios (2026-08-11)

Un solo `POST /api/pedidos` (producto 1, cantidad 2) desencadenó:

| Servicio | Qué hizo, sin que nadie se lo pidiera por HTTP |
|---|---|
| Pedidos | `CREADO` → `PAGADO` → `EN_PREPARACION` |
| Pagos | Cobró 71.80 y publicó `pago.confirmado` |
| Catálogo | Descontó stock del producto 1: **100 → 98** |
| Entregas | Asignó a **Luis Quispe** y publicó `ASIGNADA` |

Dos `PATCH` simulando al repartidor completaron el flujo hasta `ENTREGADO`.
Las **cuatro colas de fallidos quedaron vacías**.

La mejor evidencia de que los eventos transportan datos reales: el campo
`motivo` del pedido decía **"Asignada a Luis Quispe"**, un texto generado en
el dominio de Entregas que llegó a Pedidos por Kafka.

### La DLQ no funcionaba: cuatro fallos encadenados (2026-08-12)

Al provocar fallos a propósito para ver actuar la cola de mensajes fallidos,
aparecieron **cuatro defectos distintos, todos en el mismo mecanismo**. Ninguno
lo detectaron las 109 pruebas, porque ninguna levanta Kafka.

| # | Causa | Síntoma |
|---|---|---|
| 1 | Los topics de negocio tienen 3 particiones; `@RetryableTopic` crea la DLT con **1**. El recuperador publica en la misma partición de origen | Bucle infinito para todo mensaje de las particiones 1 y 2 |
| 2 | Servicios que "no publican eventos" **sin bloque `producer`**. La DLQ sí publica, y sin serializador configurado Spring usa `StringSerializer` | Bucle infinito, `SerializationException` |
| 3 | Payload del `@DltHandler` declarado como `Object`: Spring entrega el `ConsumerRecord` entero, y las cabeceras `@Header` llegan nulas | Entradas sin diagnóstico: `(desconocido)`, `offset null` |
| 4 | `retry_topic-attempts` es un entero **binario**, leído como texto UTF-8: mete bytes `0x00` y Postgres rechaza el INSERT | Bucle infinito |

**Un quinto detalle:** `@RetryableTopic` escribe las cabeceras
`kafka_original-*` y `kafka_exception-*`, no las `kafka_dlt-original-*` a las
que apuntan las constantes `KafkaHeaders.DLT_*`. Pedir la cabecera equivocada
no da error, devuelve `null`, y la cola queda llena de entradas vacías.

#### La lección, y es la buena

**Tres de los cuatro fallos terminan igual: en un bucle infinito.** Y el motivo
es siempre el mismo: si la publicación a la DLT falla, el consumidor no puede
confirmar el offset, así que relee el mismo registro para siempre. Un caso
generó 7 MB de log en tres minutos.

> Cuando falla el mecanismo de último recurso, no hay último recurso: hay un
> bucle. El código de manejo de errores necesita su propio manejo de errores,
> porque es el único que no tiene una red debajo.

**Y sobre las pruebas:** las 109 pruebas pasaban con la cola de fallidos
completamente rota. Es el argumento más honesto del trabajo sobre los límites
de las pruebas unitarias: verifican la lógica de negocio, no la integración con
la infraestructura. Un mecanismo de resiliencia hay que **verlo actuar**.

#### Correcciones aplicadas

- Clase `TopicsDeReintento` en `shared`: declara los topics de reintento y las
  DLT con las mismas 3 particiones, y también los topics consumidos, para que
  el orden de arranque no importe.
- `autoCreateTopics = "false"` en los seis listeners: una sola fuente crea los
  topics.
- `KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'false'` en el broker: impide que un
  consumidor cree un topic con el número de particiones equivocado.
- Bloque `producer` también en los servicios que solo consumen.
- El `@DltHandler` recibe el `ConsumerRecord` y `DeadLetterQueue` extrae de él
  cabeceras y payload, con respaldo si alguna falta.
- Lectura tipada de cabeceras (texto, entero de 4 bytes, largo de 8) y limpieza
  de caracteres de control antes de guardar.
- Un `WARN` que lista las cabeceras presentes cuando falta alguna esperada: si
  cambia la versión de Spring, el log lo dice en la primera ejecución.

#### Verificado en ejecución

```
topic  : pedido.confirmado          (el de origen, no la DLT)
offset : 5                          (el de origen)
error  : StockInsuficienteException: El producto 3 tiene 1 unidades
         y se solicitaron 5 [intentos: 2]
payload: {"eventoId":"b050493e-bc6f-413c-86c2-40f631546c25",
          "ocurridoEn":"2026-08-12T13:53:20.590285Z",
          "pedidoId":"...","items":[{"productoId":3,"cantidad":5}]}
```

Con esa entrada, quien revise la cola sabe **qué falló** (la excepción de
dominio, no el envoltorio de Spring), **dónde** (topic y offset de origen),
**cuántas veces se intentó**, y tiene **el evento completo** para reprocesarlo
a mano si procede. El consumidor, mientras tanto, siguió adelante.

Y el consumidor sigue adelante sin releer el mensaje.

### Fallo grave encontrado y corregido: la DLQ solo funcionaba en 1 de cada 3 mensajes (2026-08-12)

Al arrancar `user-service`, un evento que debía ir a la cola de fallidos dejó
al consumidor en **bucle infinito**: 7 MB de log en tres minutos releyendo el
mismo registro.

**Causa.** Los topics de negocio se crean con **3 particiones**, pero
`@RetryableTopic` genera sus topics de reintento y su DLT con **una sola**. Y
`DeadLetterPublishingRecoverer` publica en la MISMA partición de la que vino el
mensaje: si venía de la 1 o la 2, esa partición no existe en el destino y la
publicación falla. Al fallar, el offset no se confirma y el consumidor relee el
registro indefinidamente.

**Cómo se manifestó.** El fallo anterior de Catálogo venía de la partición 0 y
sí llegó a su DLT; este venía de la partición 1 y no. La cola de fallidos
funcionaba para uno de cada tres mensajes, justo la pieza sobre la que descansa
todo el manejo de errores.

**Corrección.** Clase `TopicsDeReintento` en el módulo `shared`: declara los
topics de reintento y las DLT con las mismas 3 particiones que los de negocio.
Cada servicio la usa con una línea en su `KafkaTopicsConfig`.

**Para el documento:** es el mejor ejemplo de por qué no basta con configurar
un mecanismo de resiliencia — hay que verlo actuar. Las 109 pruebas pasaban con
la DLQ rota, porque ninguna levanta Kafka.

### Un fallo real que atrapó la DLQ (2026-08-11)

Al arrancar `catalog-service` por primera vez, su grupo de consumidores nuevo
con `auto-offset-reset: earliest` reprocesó los eventos del día anterior. Uno
de ellos pedía descontar 2 unidades de un producto que en el catálogo real
tiene stock 0, así que lanzó `StockInsuficienteException` — no reintentable —
y acabó en `pedido.confirmado-catalogo-dlt`.

**No fue un error de programación sino de datos históricos contra reglas
nuevas**, y es justo lo que una DLQ debe capturar. En producción se evita
posicionando el grupo en `latest` al desplegar un consumidor nuevo.

### Demostraciones para la sustentación
- [x] **Camino feliz COMPLETO** con 4 servicios: `CREADO → PAGADO → EN_PREPARACION → EN_CAMINO → ENTREGADO` (2026-08-11)
- [x] **Compensación: pago rechazado → pedido rechazado → no se reembolsa** (2026-08-11)
- [x] **Mensaje fallido llegando a la DLT**, con topic y offset de origen, excepción de dominio y payload completo (2026-08-12)
- [x] **Traza cruzando Kafka**: mismo `traceId` en dos servicios (2026-08-11)
- [ ] Salto de una traza a sus logs en Grafana por `traceId` — **pendiente de verificar**: hay que confirmar que el formato ECS vuelque el `traceId` al archivo de log (la consola sí lo muestra). Ver nota en `datasource.yml`
- [ ] Rehacer el camino feliz con los 5 servicios y token emitido por user-service

---

## 5. ADRs

**Escritos los trece** (2026-08-12), en [docs/adr/](adr/README.md). Formato:
Nygard (Contexto · Decisión · Consecuencias) más dos apartados propios,
**Alternativas consideradas** —que es lo que pide la sección 4.1 del documento—
y **Verificación**, que ancla cada afirmación a evidencia comprobable en el
repositorio.

| ID | Título | Estado |
|---|---|---|
| [ADR-001](adr/ADR-001-microservicios-frente-a-monolito.md) | Arquitectura de microservicios frente a monolito modular | Aceptada |
| [ADR-002](adr/ADR-002-arquitectura-hexagonal.md) | Arquitectura hexagonal dentro de cada servicio | Aceptada |
| [ADR-003](adr/ADR-003-una-base-por-servicio.md) | Un motor Postgres con una base por servicio | Aceptada |
| [ADR-004](adr/ADR-004-saga-orquestada.md) | Saga orquestada frente a coreografiada | Aceptada |
| [ADR-005](adr/ADR-005-validacion-de-precio-sincrona.md) | Validación de precio por REST síncrono frente a réplica local por eventos | Aceptada |
| [ADR-006](adr/ADR-006-errores-reintentos-dlq.md) | Estrategia de manejo de errores: reintentos, DLQ por servicio e idempotencia | Aceptada |
| [ADR-007](adr/ADR-007-sin-outbox.md) | No implementar el patrón outbox: riesgo asumido | Aceptada |
| [ADR-008](adr/ADR-008-que-se-comparte.md) | Qué se comparte entre servicios y qué no | Aceptada |
| [ADR-009](adr/ADR-009-podman-compose.md) | Podman Compose en lugar de Kubernetes | Aceptada |
| [ADR-010](adr/ADR-010-pago-explicito.md) | El pago como paso explícito del cliente | Aceptada |
| [ADR-011](adr/ADR-011-jwt-clave-simetrica.md) | JWT con clave simétrica compartida | Aceptada |
| [ADR-012](adr/ADR-012-kubernetes-con-kind.md) | Kubernetes con kind, además de Podman Compose | Aceptada · sustituye en parte al 009 |
| [ADR-013](adr/ADR-013-circuit-breaker.md) | Circuit breaker solo en la llamada síncrona | Aceptada |

Los dos últimos no estaban planificados: el 010 recoge la decisión 20, tomada
el 2026-08-12, y el 011 cubre las secciones 5.1 y 5.3, que se habían quedado sin
ADR que las respaldara.

---

## 6. Bitácora

| Fecha | Avance |
|---|---|
| 2026-08-09 | Definidas responsabilidades de los 5 servicios y sus fronteras |
| 2026-08-09 | Definido el catálogo de 7 topics y el flujo de la saga |
| 2026-08-09 | Definida la política de errores a partir del ejemplo de Módulo 3 |
| 2026-08-09 | Escrita la infraestructura: compose con 10 contenedores, init de BD, observabilidad con Loki |
| 2026-08-09 | Iniciado `order-service` como servicio de referencia |
| 2026-08-09 | `order-service` completo: 56 clases, compila, 13 pruebas unitarias sin Spring ni BD |
| 2026-08-09 | Añadidos `@WebMvcTest` (9) y `@DataJpaTest` (5) a order-service: **27 pruebas** en total |
| 2026-08-09 | Seguridad JWT en order-service (validación, filtro, reglas de acceso, propagación a Catálogo): **32 pruebas** |
| 2026-08-10 | Reestructurado order-service a la organización de paquetes de Módulo 3; casos de uso divididos en una clase cada uno; `EventoDominio` ya no conoce los topics de Kafka. **35 pruebas** |
| 2026-08-10 | order-service verificado contra infraestructura real y subido a GitHub (commit inicial) |
| 2026-08-10 | payment-service completo: **19 pruebas**, contratos de evento verificados campo a campo contra order-service |
| 2026-08-10 | Extraído `shared` (DLQ + seguridad JWT): 7 clases compartidas, 0 duplicadas. Build multimódulo con POM agregador. **54 pruebas en total** |
| 2026-08-12 | Front React construido: acceso, catálogo, carrito con cantidades y seguimiento de pedidos |
| 2026-08-12 | **El pago pasa a ser un paso explícito** (decisión 20): nuevo estado `PAGO_EN_PROCESO`, `POST /api/pedidos/{id}/pagar` y topic `pedido.pago-solicitado`. order-service pasa de 35 a **45 pruebas** |
| 2026-08-12 | Añadido el panel de repartidor al front: la entrega no avanza sola más allá de `ASIGNADA`, y sin él la saga nunca llegaba a `ENTREGADO`. Verificado en ejecución: `COMPLETADA` → pedido `ENTREGADO` → 42 puntos de fidelidad |
| 2026-08-12 | Corregido `findAll()` sin `ORDER BY` en los listados de pedidos y entregas: el orden lo decidía el plan de Postgres y cambiaba al actualizar una fila. Nueva prueba `@DataJpaTest` del adaptador de entregas. **123 pruebas** |
| 2026-08-12 | **Sistema completo desplegado en Kubernetes** (kind sobre Podman): 14 pods, Ingress con un solo origen, observabilidad dentro del clúster. Tres fallos que solo existen en K8s: `enableServiceLinks`, `publishNotReadyAddresses` y el prefijo `localhost/` de las imágenes. Y los 5 `Dockerfile`, que nunca se habían llegado a construir |
| 2026-08-12 | **ADR-012 y ADR-013**; el 009 queda parcialmente sustituido. Documento de respuesta actualizado: 4.2, 6.1, 6.2, 7.2, 8.2 y el cierre |
| 2026-08-12 | **Escritos los 11 ADR** con índice y grafo de relaciones. Cada uno con alternativas descartadas y verificación comprobada contra el repositorio |
