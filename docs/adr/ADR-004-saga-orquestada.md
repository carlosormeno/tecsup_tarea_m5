# ADR-004 · Saga orquestada frente a coreografiada

| | |
|---|---|
| **Estado** | Aceptada |
| **Fecha** | 2026-08-09 |
| **Secciones del documento** | 2.2 Decisiones clave · 2.3 Diagramas · 3.3 Comunicación |
| **Relacionada con** | [ADR-003](ADR-003-una-base-por-servicio.md) (por qué hace falta una saga) · [ADR-006](ADR-006-errores-reintentos-dlq.md) (qué pasa cuando un paso falla) · [ADR-010](ADR-010-pago-explicito.md) (quién la arranca) |

## Contexto

Completar un pedido toca cuatro contextos y **cinco bases de datos que no
comparten transacción** ([ADR-003](ADR-003-una-base-por-servicio.md)): hay que
cobrar en `paymentdb`, descontar stock en `catalogdb`, crear la entrega en
`deliverydb` y mover el estado en `orderdb`. Sin transacción distribuida —que
no se quiere, por el acoplamiento y el bloqueo que impone— la única manera de
mantener la consistencia es una **saga**: una secuencia de transacciones
locales, cada una con su compensación por si un paso posterior falla.

La pregunta que resuelve este ADR no es *si* saga, sino **quién decide el orden
de los pasos**.

## Decisión

**Saga orquestada, con `order-service` (Pedidos) como orquestador.**

La orquestación se reconoce en un detalle concreto y verificable: **Entregas no
escucha a Pagos**. Escucha a Pedidos.

```
Pagos ──pago.confirmado──► Pedidos ──pedido.confirmado──► Entregas
                              ▲
                   punto único que decide si el flujo avanza
```

El estado de la saga es el estado del pedido, y vive en un solo archivo:
`domain/model/EstadoPedido.java`.

```
CREADO → PAGO_EN_PROCESO → PAGADO → EN_PREPARACION → EN_CAMINO → ENTREGADO
                    ↓          ↓            ↓             ↓
               RECHAZADO   CANCELADO    CANCELADO     CANCELADO
```

**Matiz que conviene declarar**, porque un lector estricto lo va a buscar: el
orquestador de manual envía *comandos* (`CobrarPago`) y recibe *respuestas*.
Este publica *eventos en pasado* (`pedido.confirmado`) y cada participante
decide qué hacer con ellos. Es orquestación **por su estructura** —coordinador
único, flujo con compuertas, estado centralizado— pero con **mensajería de
estilo evento**, que acopla menos: Pagos no necesita saber que Pedidos existe,
solo que existe el hecho «se ha solicitado el pago de este pedido».

La compensación viaja en el evento `pedido.cancelado` con el campo
**`huboCobro`**, que le dice a cada participante si hay algo que deshacer. Ese
campo no es una bandera aparte que pudiera contradecir al pedido: se deriva del
estado (`EstadoPedido.implicaCobro()`) justo antes de la transición.

## Alternativas consideradas

### A. Saga coreografiada — *descartada*

Cada servicio reacciona directamente a los eventos de los demás: Entregas
escucharía `pago.confirmado` sin pasar por Pedidos.

Ventajas reales: menos saltos, menor latencia y ningún servicio central del que
dependan los demás.

Se descarta por dos motivos concretos:

1. **El estado de la saga dejaría de existir en un sitio.** Hoy,
   `GET /api/pedidos/{id}` responde en qué punto va el flujo. En una coreografía
   habría que reconstruirlo leyendo los topics de los cinco servicios, o
   duplicando estado en cada uno.
2. **Nadie sería dueño del orden.** Que el stock se descuente después del cobro
   y no antes es una regla de negocio; en una coreografía está implícita en
   quién escucha a quién, repartida entre cinco `@KafkaListener` que hay que
   leer todos para entender el flujo.

### B. Transacción distribuida con 2PC — *descartada*

Consistencia fuerte, sin compensaciones que escribir. Se descarta porque
requiere un coordinador transaccional, bloquea recursos en los cinco servicios
mientras dura y no encaja con Kafka. Es exactamente lo que el estilo de
microservicios trata de evitar.

### C. Orquestador dedicado (un sexto servicio) — *descartada*

Un `saga-orchestrator` separado, sin dominio propio. Se descarta porque el
orquestador natural ya existe: **el pedido es el proceso de negocio**. Un
servicio aparte tendría que replicar el estado del pedido para saber qué hacer,
y entonces habría dos dueños de la misma verdad.

## Consecuencias

### Positivas

- **El flujo se lee en un archivo.** `AvanzarSagaUseCaseImpl` tiene las tres
  reacciones —pago confirmado, pago rechazado, entrega cambiada— y ninguna
  regla de orden vive fuera de él.
- **El estado se consulta con un GET.** No hay que reconstruirlo.
- **La compensación es explícita** y tiene un punto de decisión único.
- Cambiar el orden de los pasos es cambiar un archivo, no renegociar cinco
  suscripciones.

### Negativas

- **Pedidos es el cuello de botella conceptual**: todo pasa por él, y cada paso
  cuesta un salto extra por el broker. Medido en ejecución: **323 ms** desde el
  clic en pagar hasta `EN_PREPARACION`, cruzando Kafka cuatro veces.
- **Si Pedidos está caído, la saga se detiene**, aunque Pagos y Entregas estén
  perfectamente. Los eventos esperan en Kafka y el flujo continúa al volver, así
  que es una parada, no una pérdida.
- **Acoplamiento temporal al orden.** Añadir un paso nuevo obliga a tocar la
  máquina de estados, que es donde debe estar, pero significa desplegar Pedidos
  para un cambio que quizá solo afecta a otro servicio.

### Riesgos aceptados

| Riesgo | Por qué se acepta |
|---|---|
| La compensación no es un `ROLLBACK`: es una acción de negocio que también puede fallar | Cada compensación es idempotente y reintentable; si agota reintentos, queda en la DLQ para intervención manual ([ADR-006](ADR-006-errores-reintentos-dlq.md)) |
| Consistencia eventual visible al usuario | El front sondea cada 3 s y muestra el estado real en todo momento, incluido `PAGO_EN_PROCESO` |

## Verificación

| Qué demuestra la decisión | Cómo se comprueba |
|---|---|
| Es orquestada y no coreografiada | `grep -rn "@KafkaListener" delivery-service/` → escucha `pedido.confirmado`, **no** `pago.confirmado` |
| El estado está centralizado | `domain/model/EstadoPedido.java` en un único servicio |
| La saga se prueba sin infraestructura | `AvanzarSagaUseCaseImplTest`: 5 pruebas, sin Spring ni Kafka |
| El camino feliz funciona | Verificado en ejecución (2026-08-12): `CREADO` → `ENTREGADO` con los cinco servicios y +42 puntos de fidelidad |
| La compensación funciona | Pedido por encima del límite: `pago.rechazado` → `RECHAZADO` → `pedido.cancelado(huboCobro=false)` → ni reembolso ni reposición de stock |
| El orden se mantiene por pedido | Todos los eventos usan el id del pedido como clave de partición |
