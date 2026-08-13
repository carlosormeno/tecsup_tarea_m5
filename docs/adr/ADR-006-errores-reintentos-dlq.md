# ADR-006 · Manejo de errores: reintentos, una DLQ por servicio e idempotencia focalizada

| | |
|---|---|
| **Estado** | Aceptada |
| **Fecha** | 2026-08-09 · revisada el 2026-08-12 tras corregir cuatro fallos encontrados en ejecución |
| **Secciones del documento** | 2.2 Decisiones clave · 6.3 Tolerancia a fallos |
| **Relacionada con** | [ADR-004](ADR-004-saga-orquestada.md) (qué se rompe cuando un paso falla) · [ADR-007](ADR-007-sin-outbox.md) (el fallo que esto **no** cubre) |

## Contexto

Kafka entrega **al menos una vez**. Eso no es un detalle de configuración: es
una propiedad del transporte con dos consecuencias que hay que resolver
explícitamente en cada consumidor.

1. **Un mismo evento puede llegar dos veces.** Si el consumidor descuenta stock,
   descontará el doble.
2. **Un evento puede fallar al procesarse.** Y si el fallo no se gestiona, el
   offset no avanza, el consumidor relee el mismo registro para siempre y **la
   partición queda bloqueada**.

Hay además un tercer caso, más traicionero: un mensaje cuyo JSON no se puede
deserializar falla **antes** de llegar al listener, donde ningún `try/catch` del
código de negocio puede atraparlo. Es el *poison pill*.

Y no todos los fallos son iguales: que Catálogo no responda es transitorio y
reintentar tiene sentido; que un producto no exista es determinista y
reintentarlo cuatro veces solo retrasa lo inevitable.

## Decisión

Cuatro piezas, en los cinco servicios:

### 1. Reintentos con retroceso exponencial, distinguiendo el tipo de fallo

```java
@RetryableTopic(
        attempts = "4",                                  // la original + 3
        backoff = @Backoff(delay = 2000, multiplier = 2.0),  // 2s, 4s, 8s
        autoCreateTopics = "false",
        dltTopicSuffix = Topics.SUFIJO_DLT,
        exclude = { /* los deterministas */ })
```

En `exclude` van los fallos que no ganan nada reintentándose —producto
inexistente, transición de estado imposible, argumento inválido— y que pasan
directos a la cola de fallidos.

### 2. Una cola de fallidos por servicio

`@RetryableTopic` deriva el nombre de la DLT del topic de origen, así que no
existe literalmente una DLT única por servicio en Kafka. Lo que sí consigue un
**sufijo propio por servicio** es que dos servicios que consumen el mismo topic
no mezclen sus fallos: los de Pedidos van a `pago.confirmado-pedidos-dlt` y los
de Catálogo a `pedido.confirmado-catalogo-dlt`.

La vista unificada sí existe, pero del lado de Postgres: **todos los fallos caen
en la tabla `failed_events`** y se consultan juntos en `GET /api/admin/dlq`.

### 3. Protección contra el *poison pill*

```yaml
value-deserializer: ...ErrorHandlingDeserializer
properties:
  spring.deserializer.value.delegate.class: ...JsonDeserializer
```

`ErrorHandlingDeserializer` envuelve al de JSON y convierte el fallo de
deserialización en algo que el listener sí puede gestionar, en lugar de un
bucle infinito.

### 4. Idempotencia solo donde el duplicado hace daño

No se aplica el mismo mecanismo en todas partes, porque no todas las
operaciones se corrompen igual:

| Servicio | Estrategia | Por qué |
|---|---|---|
| Pedidos | **Ninguna**: si ya está en el estado destino, ignora | La transición ya es idempotente por naturaleza |
| Pagos | Consulta previa + `UNIQUE(pedido_id)` | Cobrar dos veces corrompe datos |
| Entregas | Consulta previa + `UNIQUE(pedido_id)` | Dos repartidores para un pedido |
| Catálogo | **Tabla `evento_procesado`** | Descontar stock dos veces corrompe datos y no hay estado que lo delate |
| Usuarios | **Tabla `pedido_puntuado`** | Sumar puntos dos veces corrompe el saldo |

## Alternativas consideradas

### A. Una DLQ única compartida por los cinco servicios — *descartada*

Un solo topic `dead-letter` para todo. Se descarta porque `pedido.cancelado` lo
consumen Pagos y Catálogo: con DLQ común, un fallo de reembolso y uno de
reposición de stock quedarían mezclados, y para reprocesar habría que filtrar
por servicio de origen. El sufijo por servicio lo resuelve de raíz.

### B. Sin reintentos: todo fallo va directo a la DLQ — *descartada*

Más simple y más predecible. Se descarta porque la mayoría de los fallos reales
en este sistema son transitorios —el otro servicio arrancando, la base
saturada— y se resuelven solos en segundos. Sin reintentos, cada arranque
desordenado llenaría la DLQ de eventos perfectamente válidos.

### C. Idempotencia uniforme en los cinco servicios — *descartada*

Una tabla `evento_procesado` en todos, por consistencia. Se descarta porque en
Pedidos sería ceremonia pura: la máquina de estados ya rechaza el duplicado, y
añadir una tabla sería escribir código para resolver un problema que no existe.
La consistencia que importa es la del **criterio** —proteger donde el duplicado
corrompe— y no la de la implementación.

## Consecuencias

### Positivas

- **Ningún fallo se pierde en silencio.** Todo lo que agota reintentos queda en
  `failed_events` con topic de origen, offset, número de intentos, mensaje de
  error y el payload completo.
- **La partición nunca se bloquea**, ni siquiera con un mensaje corrupto.
- **Los fallos deterministas no gastan 14 segundos** de reintentos inútiles.
- La DLQ es consultable por REST, sin abrir la base ni kafka-ui.

### Negativas

- **Muchas piezas que deben encajar a la vez.** La lección la dio la práctica: la
  DLQ estuvo rota de **cuatro maneras encadenadas** con 109 pruebas en verde
  (relato completo en [SEGUIMIENTO.md](../SEGUIMIENTO.md)). La causa raíz de tres
  de ellas fue la misma:

  > **cuando el último recurso falla, no hay último recurso: hay un bucle.**

  Si publicar en la DLT falla, el offset no se confirma y el mismo mensaje se
  relee para siempre. Un fallo de la red de seguridad no degrada el sistema:
  lo tumba.

- **Ninguna prueba automática lo cubre.** Las 123 pruebas pasan sin arrancar
  Kafka, así que **nada de esto está verificado por el build**. Es una limitación
  declarada, no un descuido: cubrirlo exigiría Testcontainers, que se dejó fuera
  del alcance.
- **Un evento reintentado llega desordenado** respecto a los que vienen detrás.
  Se tolera porque la máquina de estados rechaza las transiciones imposibles.

### Riesgos aceptados

| Riesgo | Por qué se acepta |
|---|---|
| No hay reproceso automático desde la DLQ | Reinyectar un evento mal procesado sin revisarlo puede repetir el daño; se deja como intervención manual |
| El payload en `failed_events` puede contener datos personales | Entorno de desarrollo; en producción habría que enmascararlo |

## Verificación

| Qué demuestra la decisión | Cómo se comprueba |
|---|---|
| Los topics de reintento y DLT existen con 3 particiones | kafka-ui: `*-retry-2000`, `*-retry-4000`, `*-retry-8000`, `*-dlt` |
| La DLQ atrapa un fallo real | Verificado en ejecución: stock insuficiente → entrada en `failed_events` con topic `pedido.confirmado`, offset 5, 2 intentos y el payload completo |
| Los deterministas no se reintentan | `exclude` en cada `@RetryableTopic`; en el log aparecen una sola vez |
| La DLQ se consulta por REST | `GET /api/admin/dlq` en los cinco servicios |
| La idempotencia funciona | Una prueba por servicio: `idempotencia()` en Pedidos, Pagos y Entregas |

### Los cuatro fallos que hubo que corregir

Se dejan registrados porque son la parte útil de este ADR: explican por qué la
configuración es exactamente la que es.

| # | Síntoma | Causa | Corrección |
|---|---|---|---|
| 1 | Bucle infinito, 7 MB de log en 3 min | Topics de negocio con 3 particiones, DLT con 1: publicar en la DLT fallaba desde las particiones 1 y 2 | `TopicsDeReintento` declara retry y DLT con las mismas particiones; `autoCreateTopics = "false"` |
| 2 | Bucle infinito en Catálogo y Usuarios | Sin bloque `producer:` en su `application.yaml` («no publican eventos»), pero la DLT **sí** publica | Bloque `producer:` en los cinco |
| 3 | Entradas guardadas como `(desconocido)` | `@DltHandler` con parámetro `Object`: Spring pasaba el `ConsumerRecord` | Firma `ConsumerRecord<?, ?>` explícita |
| 4 | `invalid byte sequence for encoding "UTF8": 0x00` | `retry_topic-attempts` es un entero binario de 4 bytes, leído como UTF-8 | Lectura como entero + limpieza de caracteres de control |
