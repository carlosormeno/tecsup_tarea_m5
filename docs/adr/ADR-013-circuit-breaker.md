# ADR-013 · Circuit breaker solo en la llamada síncrona

| | |
|---|---|
| **Estado** | Aceptada |
| **Fecha** | 2026-08-12 |
| **Secciones del documento** | 6.3 Tolerancia a fallos |
| **Relacionada con** | [ADR-005](ADR-005-validacion-de-precio-sincrona.md) (la llamada que protege) · [ADR-006](ADR-006-errores-reintentos-dlq.md) (lo que protege el camino asíncrono) |

## Contexto

El enunciado nombra tres mecanismos en el punto 6.3: replicación, reintentos y
**circuit breakers**. Los reintentos estaban resueltos
([ADR-006](adr/ADR-006-errores-reintentos-dlq.md)); el circuito, no.

El sistema tiene **una sola llamada síncrona entre servicios**: Pedidos a
Catálogo, para validar precio y disponibilidad
([ADR-005](ADR-005-validacion-de-precio-sincrona.md)). Estaba protegida con
tiempos de espera de 2 y 3 segundos, que evitan que una petición se cuelgue para
siempre pero no evitan el problema real:

**Con Catálogo caído, cada petición espera 3 segundos antes de fallar.** Bajo
carga, esas esperas se apilan y agotan el pool de hilos de Pedidos. El resultado
es que **un servicio caído se lleva por delante a otro que está sano** — y
Pedidos deja de responder incluso a las consultas que no necesitan a Catálogo.

Es el fallo en cascada que el estilo de microservicios debe evitar, y el timeout
por sí solo no lo evita: lo retrasa.

## Decisión

**Resilience4j sobre `CatalogoRestAdapter`, y solo ahí.**

```yaml
resilience4j.circuitbreaker.instances.catalogo:
  sliding-window-size: 10
  minimum-number-of-calls: 5          # no abrir por los dos primeros fallos
  failure-rate-threshold: 50          # 50% de fallos -> abrir
  wait-duration-in-open-state: 10s
  permitted-number-of-calls-in-half-open-state: 3
  ignore-exceptions:
    - ...ProductoNoDisponibleException
```

Tres decisiones dentro de la decisión:

**`ignore-exceptions` es la línea más importante.** Que un producto no exista es
una **respuesta correcta** del catálogo a una pregunta mal hecha, no un fallo
suyo. Sin esa exclusión, unos cuantos productos inexistentes abrirían el
circuito y bloquearían las compras válidas: el mecanismo de protección se
convertiría en la causa de la caída.

**El método de respaldo no inventa nada.** No devuelve un producto por defecto
ni un precio de reserva: traduce a `CatalogoNoDisponibleException`, que el
manejador global convierte en `503`. **Un pedido con un precio falso es peor que
un pedido que no se crea.**

**El mensaje distingue los dos casos.** «No respondió» y «ni lo intenté» son
situaciones distintas —tres segundos de espera frente a un fallo instantáneo— y
quien lea el log necesita saber cuál ocurrió.

## Alternativas consideradas

### A. Circuit breaker en los cinco servicios — *descartada*

Por uniformidad. Se descarta porque **los otros cuatro no tienen ninguna llamada
síncrona**: se comunican por eventos, donde el mecanismo de protección es el
reintento con DLQ. Un circuito sobre un consumidor de Kafka no protege de nada;
sería configuración que hay que explicar y mantener sin que haga nada.

### B. Solo tiempos de espera, sin circuito — *descartada*

Es lo que había. Se descarta por lo dicho en el contexto: el timeout evita que
una petición se cuelgue, pero no evita que cien peticiones esperen tres segundos
cada una y agoten el pool.

### C. Circuito con respaldo que devuelve datos en caché — *descartada*

Guardar el último precio conocido y servirlo si Catálogo no responde. Es
atractivo —el sistema seguiría aceptando pedidos— y se descarta a propósito:
reintroduce por la puerta de atrás el problema que
[ADR-005](ADR-005-validacion-de-precio-sincrona.md) rechazó, que es aceptar un
pedido con un precio que puede haber cambiado. Fallar es la respuesta correcta.

### D. Mamparo (bulkhead) en lugar de circuito — *descartada*

Aislar las llamadas a Catálogo en su propio pool de hilos, para que agotarlo no
afecte al resto. Resuelve el mismo problema y es complementario, no alternativo.
Se descarta por alcance: con un solo destino síncrono, el circuito cubre el caso
con menos piezas.

## Consecuencias

### Positivas

- **Un Catálogo caído deja de propagarse.** Pedidos sigue respondiendo a
  consultas y sigue avanzando la saga por eventos.
- **El fallo es inmediato en vez de lento**, que para el usuario es mejor: un
  error claro en el acto en lugar de tres segundos de espera.
- **El circuito se recupera solo**: pasados 10 s deja pasar tres llamadas de
  prueba y se cierra si funcionan.
- **Visible en `/actuator/health`**, con `register-health-indicator: true`.

### Negativas

- **Una dependencia más** en `order-service`, con su AOP por debajo. La
  anotación funciona por proxy: una llamada interna dentro de la misma clase
  no pasaría por el circuito.
- **Parámetros que no se han ajustado con carga real.** El 50% sobre ventana de
  10 es un punto de partida razonable, no un valor medido.
- **Sin prueba automática del circuito abriéndose de verdad.** Se prueba la
  traducción del método de respaldo, que es lo propio; el comportamiento del
  circuito es de la librería.

## Verificación

| Qué demuestra la decisión | Cómo se comprueba |
|---|---|
| Un fallo de transporte se traduce a excepción de dominio | `CatalogoRestAdapterTest.falloDeTransporte()` |
| Con el circuito abierto falla rápido y lo dice | `CatalogoRestAdapterTest.circuitoAbierto()` |
| Está solo donde hace falta | `grep -rn "CircuitBreaker" services/*/src/main` → solo `order-service` |
| El estado del circuito es observable | `GET /actuator/health` → `circuitBreakers.catalogo` |
| Un producto inexistente no abre el circuito | `ignore-exceptions` en `application.yaml` |
