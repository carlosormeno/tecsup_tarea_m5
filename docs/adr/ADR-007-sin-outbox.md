# ADR-007 · No implementar el patrón outbox: riesgo asumido y documentado

| | |
|---|---|
| **Estado** | Aceptada |
| **Fecha** | 2026-08-09 |
| **Secciones del documento** | 2.2 Decisiones clave · 6.3 Tolerancia a fallos · Limitaciones conocidas |
| **Relacionada con** | [ADR-004](ADR-004-saga-orquestada.md) (la saga que se detiene si esto ocurre) · [ADR-006](ADR-006-errores-reintentos-dlq.md) (el fallo que **sí** está cubierto) |

## Contexto

Cada paso de la saga hace dos escrituras que **no comparten transacción**:

```java
repositorio.guardar(pedido);              // 1. Postgres
publicador.publicar(PagoSolicitado.de(pedido));  // 2. Kafka
```

`@Transactional` cubre la primera, no la segunda. Si la base confirma y el
broker falla, queda un pedido en `PAGO_EN_PROCESO` **que nadie más conoce**: el
evento no existe, Pagos nunca cobrará y la saga se detiene sin que ningún
mecanismo lo detecte. Al revés —publicar y que falle la base— produce un evento
que habla de un cambio que no ocurrió.

Es el problema de la **doble escritura**, y conviene separarlo de lo que resuelve
[ADR-006](ADR-006-errores-reintentos-dlq.md): allí se gestiona lo que falla
**al consumir** un evento que existe. Aquí el evento **nunca llegó a existir**, y
por eso no hay reintento ni DLQ que valga: no hay nada que reintentar.

La solución estándar es el patrón **outbox**: escribir el evento en una tabla de
la misma base y en la misma transacción, y publicarlo después desde ahí. Así la
atomicidad la garantiza Postgres.

## Decisión

**No se implementa el outbox.** Se asume la ventana de inconsistencia, se
documenta dónde está y se declara cómo se resolvería.

La decisión se toma sobre tres datos concretos:

1. **La ventana es de milisegundos.** `kafkaTemplate.send()` va justo detrás del
   `guardar()`, con `acks: all`. Para caer dentro hace falta que el proceso muera
   o el broker se caiga exactamente entre las dos líneas.
2. **El fallo es visible.** Un pedido atascado en `PAGO_EN_PROCESO` es detectable
   con una consulta trivial (`estado = 'PAGO_EN_PROCESO' AND actualizado_en <
   now() - interval '5 minutes'`), y `PAGO_EN_PROCESO` existe precisamente
   porque el pago pasó a ser un paso explícito
   ([ADR-010](ADR-010-pago-explicito.md)). Antes de ese cambio, un pedido
   atascado se confundía con uno recién creado.
3. **El coste no es la tabla.** Es el publicador que la vacía: un proceso
   programado o un conector de CDC, con su propio ciclo de vida, su propia
   monitorización y sus propios modos de fallo. Multiplicado por cinco
   servicios, es más infraestructura que la que este trabajo puede sostener.

Los puntos exactos donde vive el riesgo están marcados en el código con un
comentario que cita este ADR:

- `PagarPedidoUseCaseImpl.pagar()`
- `AvanzarSagaUseCaseImpl` (las tres reacciones)
- `CancelarPedidoUseCaseImpl.cancelar()`
- Los equivalentes en Pagos, Catálogo y Entregas

## Alternativas consideradas

### A. Outbox con publicador por sondeo — *descartada para este alcance*

Tabla `outbox` en cada base, escrita en la misma transacción, y un
`@Scheduled` que lee las filas pendientes y las publica.

Es la solución correcta y la que se implementaría en producción. Se descarta
aquí por coste: cinco tablas, cinco procesos programados, y un problema nuevo
que resolver —el sondeo publica **al menos una vez**, así que hay que garantizar
que el consumidor tolere duplicados. Eso ya está resuelto
([ADR-006](ADR-006-errores-reintentos-dlq.md)), lo que significa que el camino
está preparado si se quisiera dar el paso.

### B. Outbox con CDC (Debezium) — *descartada*

Mismo patrón, pero leyendo el WAL de Postgres en lugar de sondear. Es la versión
industrial: sin latencia de sondeo y sin carga extra sobre la base. Se descarta
porque añade Kafka Connect y Debezium al despliegue, que ya tiene diez
contenedores.

### C. Transacciones de Kafka (`exactly-once`) — *descartada*

Kafka las soporta, pero solo entre topics de Kafka. **No hacen atómica una
escritura en Postgres con una publicación en Kafka**, que es exactamente el
problema aquí. No resuelve el caso.

### D. Publicar primero y guardar después — *descartada*

Invertir el orden. Se descarta porque empeora el fallo: en lugar de un pedido
que nadie conoce, quedaría un evento que anuncia algo que no ocurrió, y los
demás servicios actuarían sobre una mentira. Entre las dos, es preferible perder
un evento que emitir uno falso.

## Consecuencias

### Positivas

- El código de los casos de uso se lee de corrido: guardar y publicar, sin una
  capa intermedia.
- Sin tablas de infraestructura mezcladas con las de negocio.
- Sin procesos adicionales que operar ni vigilar.

### Negativas

- **Existe una ventana real en la que un evento puede perderse.** No es
  teórica: es un fallo posible, poco probable y sin recuperación automática.
- **La saga se detendría en silencio.** No hay alerta: haría falta la consulta
  de detección del punto 2, que no está automatizada.
- **La recuperación es manual**: reponer el evento o revertir el pedido a mano.

### Riesgos aceptados

| Riesgo | Probabilidad | Impacto | Mitigación actual |
|---|---|---|---|
| Postgres confirma y Kafka falla | Muy baja | Saga detenida, un pedido | El estado atascado es detectable por consulta |
| El proceso muere entre las dos escrituras | Muy baja | Igual | Igual |
| Kafka rechaza por `acks: all` sin réplicas | Baja en desarrollo (un broker) | Igual | La excepción sube y el `POST` devuelve error, así que el cliente **sí** se entera |

Este último matiz importa y conviene no pasarlo por alto: cuando el disparo lo
provoca una petición HTTP —crear o pagar un pedido—, un fallo al publicar sube
como excepción y el cliente recibe un `5xx`. El caso verdaderamente silencioso
es el de un paso disparado por otro evento, donde no hay nadie esperando
respuesta.

## Verificación

| Qué demuestra la decisión | Cómo se comprueba |
|---|---|
| El riesgo está reconocido en el código, no escondido | `grep -rn "ADR-007" services/*/src/main` → los casos de uso que hacen doble escritura |
| No hay tabla outbox | `\dt` en las cinco bases |
| El estado atascado se detecta | `SELECT * FROM pedido WHERE estado = 'PAGO_EN_PROCESO' AND actualizado_en < now() - interval '5 minutes'` |
| El camino de vuelta está preparado | Los consumidores ya son idempotentes ([ADR-006](ADR-006-errores-reintentos-dlq.md)), que es el requisito previo del outbox |
