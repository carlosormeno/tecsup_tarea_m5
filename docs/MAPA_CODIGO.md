# Mapa de la arquitectura al código

Dónde está implementado cada concepto del documento de arquitectura. Sirve
para verificar que lo que se describe existe de verdad, y en qué archivo.

Para **qué hace cada servicio y cómo encajan entre sí**, ver
[SERVICIOS_Y_FLUJO.md](SERVICIOS_Y_FLUJO.md). Este documento es el mapa al
código; aquel es la descripción funcional.

**Estado:** los cinco servicios construidos. Falta el front React.

---

## 1. Tipo de saga

**Saga orquestada**, con `order-service` (Pedidos) como orquestador.

La orquestación se reconoce en un detalle concreto: **Entregas no escucha a
Pagos**. Escucha a Pedidos.

```
Pagos ──pago.confirmado──► Pedidos ──pedido.confirmado──► Entregas
                              ▲
                   punto único que decide si el flujo avanza
```

En una saga coreografiada, Entregas reaccionaría directamente a
`pago.confirmado` y no existiría ningún punto central. Aquí sí existe: ningún
participante avanza si Pedidos no publica el evento que le abre la puerta.

**Matiz que conviene declarar:** el orquestador de manual envía *comandos*
(`CobrarPago`) y recibe *respuestas*. Este publica *eventos* en pasado
(`pedido.confirmado`) y cada participante decide qué hacer con ellos. Es
orquestación por su estructura —coordinador único, flujo con compuertas,
estado centralizado— pero con mensajería de estilo evento, que acopla menos:
Pagos no necesita saber que Pedidos existe, solo que existe el hecho
"se ha solicitado el pago de este pedido".

### Dónde arranca y dónde está el orquestador

| Pieza | Archivo |
|---|---|
| **Arranque de la saga** (lo dispara el cliente al pagar) | `application/PagarPedidoUseCaseImpl.java` |
| Puerto de entrada de la saga | `application/AvanzarSagaUseCase.java` |
| **Orquestador** (las 3 reacciones) | `application/AvanzarSagaUseCaseImpl.java` |
| **Estado de la saga** (máquina de estados) | `domain/model/EstadoPedido.java` |
| Agregado que aplica las transiciones | `domain/model/Pedido.java` |

Nótese qué NO está en esa lista: `CrearPedidoUseCaseImpl`. Crear el pedido no
arranca la saga ni publica nada — de hecho esa clase ni siquiera depende de
`PublicadorEventos`. La saga empieza cuando el cliente pulsa «pagar».

`EstadoPedido` es la saga entera en un archivo: define qué transiciones son
legales y cuáles no. Que exista ese estado en un solo sitio es lo que permite
consultar `GET /api/pedidos/{id}` y saber en qué punto va la saga; en una
coreografía habría que reconstruirlo leyendo los topics de los cinco servicios.

### Compensación

| Caso | Archivo | Qué publica |
|---|---|---|
| Pago rechazado | `application/AvanzarSagaUseCaseImpl.java` → `pagoRechazado()` | `PedidoCancelado(huboCobro = false)` |
| Entrega fallida | `application/AvanzarSagaUseCaseImpl.java` → `entregaCambioEstado()` | `PedidoCancelado(huboCobro = true)` |
| Cancela el cliente | `application/CancelarPedidoUseCaseImpl.java` | `PedidoCancelado(huboCobro = según estado)` |
| Definición del evento | `domain/event/PedidoCancelado.java` | — |

El campo `huboCobro` es lo que le dice a Pagos si tiene que reembolsar o no,
sin que tenga que deducirlo por su cuenta.

---

## 2. Clases de Kafka

Todas en `infrastructure/`. **El dominio no tiene una sola referencia a Kafka**
— se puede comprobar con el `grep` de la sección 5.

| Responsabilidad | Archivo |
|---|---|
| Nombres de los topics y sufijo de la DLT | `infrastructure/messaging/Topics.java` |
| **Productor**: publica los eventos | `infrastructure/messaging/KafkaEventPublisher.java` |
| **Consumidor** de eventos de Pagos | `infrastructure/messaging/listener/PagoEventListener.java` |
| **Consumidor** de eventos de Entregas | `infrastructure/messaging/listener/EntregaEventListener.java` |
| DTOs de lectura de eventos ajenos | `infrastructure/messaging/dto/` |
| Creación de topics con sus particiones | `infrastructure/config/KafkaTopicsConfig.java` |
| Configuración (serializadores, trazas, mapeo de tipos) | `src/main/resources/application.yaml` |

### Detalles que el documento debería mencionar

**El puerto que aísla el dominio de Kafka** es `domain/event/PublicadorEventos.java`:
una interfaz con un solo método. `KafkaEventPublisher` es su única
implementación. Cambiar de broker es escribir otro adaptador.

**`EventoDominio` no conoce los topics.** Expone `idAgregado()`, un concepto de
dominio; es `KafkaEventPublisher` quien decide que eso sirve como clave de
partición y a qué topic va cada evento, mediante un mapa. Así todos los eventos
de un mismo pedido caen en la misma partición y se consumen en orden.

**Cada servicio define su propio DTO** para leer los eventos ajenos
(`infrastructure/messaging/dto/`) en lugar de compartir las clases del
publicador. Sin eso, los cinco servicios tendrían que compilar juntos.

---

## 3. Manejo de errores y DLQ

Las clases de la DLQ son **compartidas**: viven en `services/shared`,
que los cinco servicios incluyen como dependencia. Lo específico de cada uno
(qué topics escucha, qué excepciones no reintenta) sí está en su propio código.

| Responsabilidad | Archivo |
|---|---|
| Política de reintentos (`@RetryableTopic`) | `<servicio>/infrastructure/messaging/listener/` |
| Último recurso (`@DltHandler`) | los listeners de cada servicio |
| Registro del evento fallido | **compartido**: `shared/dlq/DeadLetterQueue.java` |
| Tabla de eventos fallidos | **compartido**: `shared/dlq/FailedEvent.java` |
| Consulta por REST | **compartido**: `shared/dlq/DLQController.java` → `GET /api/admin/dlq` |
| Protección contra *poison pill* | `application.yaml` → `ErrorHandlingDeserializer` |
| Filtro de idempotencia | `application/AvanzarSagaUseCaseImpl.java` → `yaProcesado()` |

**Clasificación de fallos**, que es lo que decide si un mensaje se reintenta o
va directo a la DLQ:

| Excepción | Tipo | ¿Reintenta? |
|---|---|---|
| `CatalogoNoDisponibleException` | transitorio | sí |
| `ProductoNoDisponibleException` | determinista | no |
| `PedidoNoEncontradoException` | determinista | no |
| `TransicionInvalidaException` | determinista | no |

Están en `domain/exception/`, y la lista de no reintentables se declara en el
`exclude` de `@RetryableTopic` de cada listener.

---

## 4. Hexagonal: puertos y adaptadores

| Puerto | Interfaz | Adaptador |
|---|---|---|
| Entrada REST | `application/CrearPedidoUseCase.java`, `PagarPedidoUseCase.java` y los otros 2 | `infrastructure/web/controller/PedidoController.java` |
| Entrada por eventos | `application/AvanzarSagaUseCase.java` | `infrastructure/messaging/listener/` |
| Salida a BD | `domain/repository/PedidoRepository.java` | `infrastructure/persistence/adapter/PedidoRepositoryAdapter.java` |
| Salida a broker | `domain/event/PublicadorEventos.java` | `infrastructure/messaging/KafkaEventPublisher.java` |
| Salida a Catálogo | `domain/client/CatalogoPort.java` | `infrastructure/client/CatalogoRestAdapter.java` |

El cableado está en `infrastructure/config/BeanConfiguration.java`: registra
cada implementación detrás de su interfaz, de modo que el controlador depende
del puerto y nunca de la clase concreta.

**La prueba de que el hexágono sirve** está en
`src/test/java/.../application/`: la saga completa se prueba con tres dobles
escritos a mano (`Fakes.java`), sin Spring, sin Postgres y sin Kafka, en
milisegundos.

---

## 5. Verificación de las reglas del hexágono

Repetible en cualquier servicio, desde `src/main/java/com/tecsup/app/micro/<servicio>`:

```bash
# El dominio no depende de las otras capas
grep -rn "import com.tecsup.*\(application\|infrastructure\)" domain/

# El dominio no conoce ningún framework
grep -rn "import \(org.springframework\|jakarta.persistence\|org.apache.kafka\)" domain/

# La aplicación no depende de la infraestructura
grep -rn "import com.tecsup.*infrastructure" application/
```

Los tres deben devolver vacío. Única excepción admitida: `application` importa
`@Transactional` de Spring.

---

## 6. Observabilidad

| Qué | Dónde |
|---|---|
| Métricas | `application.yaml` → `management.endpoints` · `GET /actuator/prometheus` |
| Trazas | `application.yaml` → `management.tracing` y `management.zipkin` |
| **Trazas a través de Kafka** | `application.yaml` → `spring.kafka.template.observation-enabled` y `spring.kafka.listener.observation-enabled` |
| Logs estructurados | `application.yaml` → `logging.structured.format.file: ecs` |
| Recolección de logs | `infra/observability/promtail/promtail-config.yml` |
| Correlación traza ↔ log | `infra/observability/grafana/provisioning/datasources/datasource.yml` → `derivedFields` |

Sin las dos propiedades `observation-enabled`, la traza se corta en el broker:
Zipkin mostraría el `POST /api/pedidos` por un lado y el consumo del evento por
otro, como si no tuvieran relación. Con ellas, el flujo completo de la saga
aparece como una sola cascada — que es la mejor evidencia visual de que la saga
funciona.

---

## 7. Seguridad

| Qué | Dónde |
|---|---|
| Validación del JWT | `infrastructure/security/JwtTokenProvider.java` |
| Filtro que puebla el contexto | `infrastructure/security/JwtAuthenticationFilter.java` |
| Reglas de acceso y lista blanca | `infrastructure/security/SecurityConfig.java` |
| Propagación del token a Catálogo | `infrastructure/config/RestClientConfig.java` |
| Aislamiento de datos por servicio | `infra/postgres/init/01-crear-bases.sh` |

---

## 8. Estado y cómo crece este documento

| Servicio | Estado |
|---|---|
| order-service | Completo, 35 pruebas, verificado en ejecución |
| payment-service | Completo, 19 pruebas |
| catalog-service | Completo, 21 pruebas |
| delivery-service | Completo, 16 pruebas |
| user-service | Completo, 18 pruebas |
| Front React | Pendiente |

### Cómo se amplía sin duplicar

Los cinco servicios comparten estructura: mismos paquetes, mismo patrón de
puertos y adaptadores, misma DLQ, misma observabilidad. Repetir las secciones
2 a 7 para cada uno sería ruido y se desincronizaría a la primera
refactorización.

**Regla:** las secciones 2 a 7 describen la **estructura común**, válida para
los cinco. Lo que cambia por servicio va en la tabla de abajo, y solo se
detalla aparte lo que se salga del patrón.

| Servicio | Publica | Consume | Sufijo DLT | Base | Puerto de salida a otro servicio | Idempotencia con tabla |
|---|---|---|---|---|---|---|
| order-service | `pedido.pago-solicitado`, `.confirmado`, `.entregado`, `.cancelado` | `pago.confirmado`, `pago.rechazado`, `entrega.estado-cambiado` | `-pedidos-dlt` | `orderdb` | `CatalogoPort` (REST) | No: la transición de estado ya es idempotente |
| catalog-service | **nada** | `pedido.confirmado`, `pedido.cancelado` | `-catalogo-dlt` | `catalogdb` | — | **Sí, con tabla `evento_procesado`**: descontar stock dos veces corrompe datos de forma irreversible |
| payment-service | `pago.confirmado`, `pago.rechazado` | `pedido.pago-solicitado`, `pedido.cancelado` | `-pagos-dlt` | `paymentdb` | — | **Sí**: cobrar dos veces corrompe datos. Guarda en el caso de uso + `UNIQUE(pedido_id)` en la tabla |
| delivery-service | `entrega.estado-cambiado` | `pedido.confirmado` | `-entregas-dlt` | `deliverydb` | — | **Sí**: dos entregas ocuparían dos repartidores. Guarda + `UNIQUE(pedido_id)` |
| user-service | **nada** | `pedido.entregado` | `-usuarios-dlt` | `userdb` | — | **Sí, con tabla `pedido_puntuado`**: sumar puntos dos veces corrompe el saldo |

**Al terminar cada servicio hay que:**
1. Marcarlo como completo en la tabla de estado.
2. Rellenar su fila de arriba si algo cambió respecto a lo previsto.
3. Correr los tres `grep` de la sección 5 y confirmar que dan vacío.
4. Añadir una sección propia **solo si** el servicio se sale del patrón común.
