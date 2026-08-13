# ADR-010 · El pago como paso explícito del cliente

| | |
|---|---|
| **Estado** | Aceptada |
| **Fecha** | 2026-08-12 |
| **Sustituye a** | La conducta original, en la que crear un pedido disparaba el cobro |
| **Secciones del documento** | 2.3 Diagramas · 3.2 Interfaces · 3.3 Comunicación |
| **Relacionada con** | [ADR-004](ADR-004-saga-orquestada.md) (la saga que esto arranca) · [ADR-007](ADR-007-sin-outbox.md) (el fallo que este cambio hace detectable) |

## Contexto

En la primera versión, `POST /api/pedidos` creaba el pedido **y**, en la misma
transacción, publicaba el evento que hacía cobrar a Pagos. Crear un pedido lo
pagaba. El front lo reflejaba con un único botón, *Confirmar pedido*, que en
realidad confirmaba y cobraba.

Eso dejaba dos problemas, uno de negocio y otro de modelo:

**No se corresponde con el dominio.** En cualquier tienda, confirmar el pedido y
pagarlo son dos decisiones distintas del cliente, con un intervalo entre medias
en el que puede cambiar de idea, corregir el carrito o simplemente no pagar
nunca.

**El estado `CREADO` significaba dos cosas a la vez**: «recién hecho» y
«esperando la respuesta del cobro». La consecuencia se veía en la máquina de
estados: un pedido rechazado pasaba de `CREADO` a `RECHAZADO` sin que quedara
ningún rastro de que hubo un intento de pago. Y un pedido atascado por un evento
perdido era indistinguible de uno recién creado.

## Decisión

**Separar crear de pagar**, con un estado intermedio nuevo.

| | Antes | Ahora |
|---|---|---|
| `POST /api/pedidos` | crea **y** publica `pedido.creado` | solo crea → `CREADO`, **no publica nada** |
| Pagar | automático | `POST /api/pedidos/{id}/pagar` → `PAGO_EN_PROCESO` |
| Evento que dispara el cobro | `pedido.creado` | `pedido.pago-solicitado` |
| Estados | `CREADO → PAGADO → …` | `CREADO → PAGO_EN_PROCESO → PAGADO → …` |

Dos consecuencias de diseño que se derivan solas:

**`CrearPedidoUseCaseImpl` deja de depender de `PublicadorEventos`.** Crear un
pedido no le cuenta nada a nadie, y eso se lee directamente en el constructor de
la clase: es la prueba estructural de la separación, no un comentario.

**El evento se renombra en lugar de reutilizarse.** Un evento llamado
`pedido.creado` que se publica al pagar es un nombre que miente sobre cuándo
ocurre el hecho, y eso confunde más que renombrar dos constantes y un DTO.
`pedido.creado` desaparece del sistema; no queda ningún topic sin consumidor.

## Alternativas consideradas

### A. Dejarlo como estaba — *descartada*

Funcionaba y estaba verificado de punta a punta. Se descarta porque el modelo
no representaba el negocio y porque el estado `CREADO` sobrecargado impedía
distinguir situaciones que hay que distinguir.

### B. Separar el paso pero conservar el topic `pedido.creado` — *descartada*

Lo más barato: publicar el mismo evento, solo que más tarde. No habría tocado
Pagos ni el mapeo de tipos. Se descarta por lo dicho arriba: el nombre pasaría a
mentir, y en un trabajo cuyo objeto es la arquitectura, un catálogo de eventos
que no se corresponde con los hechos es un defecto de fondo.

### C. Añadir el paso y publicar además `pedido.creado` como hecho de dominio — *descartada*

Publicar los dos: `pedido.creado` al crear, para quien quiera enterarse, y
`pedido.pago-solicitado` al pagar. Es defendible en arquitectura orientada a
eventos —los hechos se publican aunque hoy nadie los consuma— pero se descarta
por coherencia con el resto del trabajo: dejaría un topic sin ningún consumidor
que habría que explicar en la sustentación sin ganar nada a cambio.

### D. Un estado `PENDIENTE_PAGO` en lugar de `PAGO_EN_PROCESO` — *descartada*

Nombrar el estado por lo que falta y no por lo que está ocurriendo. Se descarta
porque el estado nuevo no es «pendiente de que el cliente pague» —eso es
`CREADO`—, sino «el cobro está en marcha y se espera la respuesta de Pagos».

## Consecuencias

### Positivas

- **El doble clic en «pagar» deja de ser un problema.** El segundo intento choca
  contra una transición que ya no existe y devuelve `409` sin publicar un
  segundo evento. La protección vive en el dominio, no en el navegador ni en un
  `if` del controlador.
- **Nadie puede dar un pedido por pagado saltándose a Pagos**: el único camino a
  `PAGADO` pasa por `PAGO_EN_PROCESO`, y a ese estado solo se llega solicitando
  el cobro.
- **`huboCobro` deja de ser una deducción frágil.** Era `!estaEn(CREADO)`; ahora
  es `EstadoPedido.implicaCobro()`, que dice explícitamente a partir de qué
  estado hay dinero que devolver, y añade un caso que antes no se contemplaba:
  cancelar con el cobro en vuelo.
- **Un evento perdido se vuelve detectable.** Un pedido parado en
  `PAGO_EN_PROCESO` es un síntoma inequívoco; en `CREADO` no lo era
  ([ADR-007](ADR-007-sin-outbox.md)).
- El diagrama de secuencia refleja ahora **dos llamadas del cliente**, que es lo
  que de verdad ocurre.

### Negativas

- **Un paso más para el usuario** y una llamada más al servidor.
- **Un estado más que mantener** en la máquina de estados, en el front y en la
  documentación.
- **Cambio incompatible en el catálogo de eventos**: Pedidos y Pagos hay que
  desplegarlos a la vez. En un sistema con consumidores ajenos habría hecho
  falta una transición con los dos topics conviviendo.
- Queda un rastro: el comentario de `V1__crear_tablas.sql` de Pagos sigue
  citando `pedido.creado`. Se dejó a propósito, porque **una migración ya
  aplicada no se edita** —ni siquiera un comentario—: Flyway valida el checksum
  del archivo completo y el arranque fallaría contra cualquier base existente.

## Verificación

| Qué demuestra la decisión | Cómo se comprueba |
|---|---|
| Crear no arranca la saga | `PagarPedidoUseCaseImplTest.crearNoArrancaLaSaga()`: tras crear, `publicador.publicados` está vacío |
| Crear no puede publicar ni queriendo | `CrearPedidoUseCaseImpl` no recibe `PublicadorEventos` en el constructor |
| No se puede saltar el cobro | `PedidoTest.exigePasarPorElCobro()`: `CREADO → PAGADO` lanza `TransicionInvalidaException` |
| El doble clic no cobra dos veces | `PagarPedidoUseCaseImplTest.noSePagaDosVeces()` y `PedidoControllerTest.pagarDosVeces()` → `409` |
| Funciona contra Kafka | Verificado en ejecución (2026-08-12), pedido `776e57a8`: `CREADO` `19:33:58` → clic en pagar `19:34:03` → `PAGO_EN_PROCESO` → cobro aprobado `19:34:04.156` → `EN_PREPARACION` `19:34:04.242`. **323 ms** desde el clic, cruzando el broker cuatro veces |
| No queda ningún topic huérfano | `grep -rn "pedido.creado" services/*/src/main --include="*.java"` → vacío |
