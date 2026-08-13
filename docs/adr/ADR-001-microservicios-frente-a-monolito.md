# ADR-001 · Arquitectura de microservicios frente a monolito modular

| | |
|---|---|
| **Estado** | Aceptada |
| **Fecha** | 2026-08-09 |
| **Secciones del documento** | 2.1 Estilo arquitectónico · 4.1 Justificación |
| **Relacionada con** | [ADR-002](ADR-002-arquitectura-hexagonal.md) (qué hay dentro de cada servicio) · [ADR-003](ADR-003-una-base-por-servicio.md) (cómo se reparten los datos) |

## Contexto

El dominio de pedidos de comida a domicilio se descompone con nitidez en
**cinco contextos delimitados**, cada uno con su propio lenguaje ubicuo, su
propio agregado raíz y su propio dueño de los datos:

| Contexto | Agregado raíz | Lenguaje propio | Motivo de cambio |
|---|---|---|---|
| Usuarios | Usuario | credenciales, roles, fidelidad | política de identidad |
| Catálogo | Producto / Restaurante | precio, stock, disponibilidad | negociación comercial |
| Pedidos | Pedido | estado, líneas, total | reglas del ciclo de vida |
| Pagos | Pago | transacción, reembolso | normativa y medios de pago |
| Entregas | Entrega | repartidor, seguimiento | logística |

Que las fronteras son reales lo demuestra un detalle del propio dominio: **el
mismo hecho físico tiene dos nombres distintos** según quién lo mire. Cuando el
repartidor deja la comida en la puerta, Entregas lo llama `COMPLETADA` y
Pedidos lo llama `ENTREGADO`. No es redundancia: son dos modelos que responden
a preguntas distintas. Un modelo único obligaría a inventar un vocabulario de
compromiso que no sería el natural de ninguno de los dos.

Los cinco contextos tienen además **motivos de cambio y perfiles de carga
distintos**: el catálogo se lee constantemente y se escribe poco; Pagos escribe
poco pero cada escritura es crítica y auditable; Entregas cambia al ritmo de la
operación logística. Acoplarlos en un despliegue único obliga a que el más
crítico se reinicie cada vez que cambia el menos crítico.

A esa realidad del dominio se suman dos restricciones del encargo: es una tarea
del Módulo 5, cuyo tema es microservicios, y el profesor pide ver los servicios
conectados al broker.

## Decisión

**Cinco microservicios independientes, uno por contexto delimitado** —Usuarios,
Catálogo, Pedidos, Pagos, Entregas—, cada uno con su proceso, su puerto, su
base de datos y su ciclo de despliegue. La frontera del servicio se hace
coincidir con la frontera del contexto: ni un servicio que abarque dos
contextos, ni dos servicios que se repartan uno.

Se comunican por eventos de Kafka, con una única excepción síncrona documentada
en [ADR-005](ADR-005-validacion-de-precio-sincrona.md).

## Alternativas consideradas

### A. Monolito modular — *descartada*

Una sola aplicación Spring Boot con cinco módulos Maven, fronteras internas
vigiladas por paquetes y una base de datos con cinco esquemas.

Tiene ventajas nada despreciables que conviene reconocer: transacciones ACID
reales en lugar de una saga, sin latencia de red, sin consistencia eventual y
un solo despliegue que operar.

Se descarta por dos motivos. El primero es que **las fronteras quedarían
sostenidas solo por disciplina**: nada impide en un monolito que Entregas
consulte la tabla de precios «solo esta vez», y esa es exactamente la erosión
que convierte un monolito modular en uno normal. El segundo es que **el dominio
de fallo sigue siendo único**: una fuga de memoria en el cálculo de rutas tumba
también el cobro y el catálogo, aunque los módulos estén perfectamente
separados en el código.

Secundariamente, tampoco permitiría enseñar servicios conectados al broker, que
es un requisito explícito del encargo.

### B. Microservicios más finos — *descartada*

Separar además Notificaciones, Fidelización e Inventario. Se descarta por
prematuro: Fidelización son quince líneas dentro de Usuarios e Inventario es la
columna `stock` de Catálogo. Partirlos añadiría tres despliegues y tres bases
para repartir código que cabe en una pantalla.

### C. Monolito con un módulo separado para el cobro — *descartada*

El punto medio habitual. Se descarta por la misma razón que A, y porque no
ilustra ninguno de los problemas interesantes del módulo: sin varios servicios
no hay saga, ni compensación, ni idempotencia entre procesos.

## Consecuencias

### Positivas

- Cada servicio se despliega, reinicia y falla por separado. Verificado en
  ejecución: con Pagos caído, los pedidos se siguen creando y el cobro se
  procesa al volver el servicio, porque el evento espera en Kafka.
- Las fronteras son físicas, no de disciplina. Ningún servicio puede leer la
  base de otro aunque quiera: cada base tiene su propio rol de Postgres sin
  permisos cruzados ([ADR-003](ADR-003-una-base-por-servicio.md)).
- Cada contexto usa su propio vocabulario sin negociar con los demás. El mismo
  hecho se llama `COMPLETADA` en Entregas y `ENTREGADO` en Pedidos, y está bien
  que así sea.

### Negativas

- **Se pierde la transacción.** Confirmar un pedido toca cinco bases que no
  comparten transacción, y eso obliga a una saga con compensación
  ([ADR-004](ADR-004-saga-orquestada.md)) y a asumir una ventana de
  inconsistencia ([ADR-007](ADR-007-sin-outbox.md)).
- **Hay que tolerar duplicados en todas partes.** Kafka entrega al menos una
  vez, así que cada consumidor necesita su estrategia de idempotencia
  ([ADR-006](ADR-006-errores-reintentos-dlq.md)).
- **El coste de operación se multiplica por cinco**: cinco despliegues, cinco
  configuraciones, cinco juegos de logs. De ahí que la observabilidad centralizada
  deje de ser un lujo y pase a ser obligatoria.
- **Depurar es más difícil.** Un fallo puede vivir en el hueco entre dos
  servicios, donde ninguna prueba unitaria lo ve. Ocurrió: la cola de mensajes
  fallidos estuvo rota de cuatro maneras distintas con 109 pruebas en verde,
  porque ninguna arranca Kafka (registrado en
  [SEGUIMIENTO.md](../SEGUIMIENTO.md)).

### Riesgos aceptados

| Riesgo | Por qué se acepta |
|---|---|
| El coste del estilo supera al que justificaría el volumen actual de este sistema | Se paga por adelantado la separación que el dominio ya tiene; con cinco contextos tan definidos, el reparto no habrá que rehacerlo |
| Sin orquestador ni CI/CD | Fuera del alcance; ver [ADR-009](ADR-009-podman-compose.md) |
| Un solo desarrollador para cinco servicios | Se mitiga con un servicio de referencia (Pedidos) del que los otros cuatro copian estructura |

## Verificación

| Qué demuestra la decisión | Cómo se comprueba |
|---|---|
| Son cinco procesos reales | Cinco puertos: 8081–8085, cada uno con su Swagger |
| Están conectados al broker | kafka-ui muestra los siete topics con sus grupos de consumidores |
| Los datos están separados de verdad | `\du` en Postgres: cinco roles; ninguno puede conectarse a la base de otro |
| Se despliegan por separado | Cada servicio tiene su `Dockerfile` y su módulo Maven |
| La independencia es real, no nominal | Detener Pagos no impide crear pedidos; al volver, la saga continúa |
