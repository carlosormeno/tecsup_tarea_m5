# ADR-005 · Validación de precio por REST síncrono frente a réplica local por eventos

| | |
|---|---|
| **Estado** | Aceptada |
| **Fecha** | 2026-08-09 |
| **Secciones del documento** | 2.2 Decisiones clave · 3.3 Comunicación · 6.3 Tolerancia a fallos |
| **Relacionada con** | [ADR-003](ADR-003-una-base-por-servicio.md) (por qué el precio hay que copiarlo) · [ADR-004](ADR-004-saga-orquestada.md) (el resto del flujo sí es asíncrono) |

## Contexto

Al crear un pedido hay que resolver dos preguntas sobre cada producto: **cuánto
cuesta** y **si se puede pedir**. Y hay una que no admite discusión: **el precio
no puede venir del cliente**. Si el `POST /api/pedidos` aceptara el precio en el
cuerpo, cualquiera podría pedir una pizza a un sol.

Pero el dueño del precio es Catálogo, en otra base a la que Pedidos no puede
conectarse ([ADR-003](ADR-003-una-base-por-servicio.md)). Hace falta cruzar la
frontera, y hay dos formas de hacerlo: **preguntar en el momento** o **tener una
copia local** que se mantenga al día por eventos.

Es además el único punto de todo el sistema donde esto ocurre: el resto del
flujo es asíncrono por diseño.

## Decisión

**Una llamada REST síncrona de Pedidos a Catálogo** en el momento de crear el
pedido, para leer nombre, precio y disponibilidad, que se **congelan** en la
línea del pedido.

Detrás de un puerto de dominio, `CatalogoPort`, con su adaptador
`CatalogoRestAdapter`:

- **Tiempos de espera cortos y explícitos**: 2 s de conexión, 3 s de lectura.
  Sin ellos, Catálogo colgado dejaría colgado también a Pedidos.
- **El adaptador traduce fallos HTTP a excepciones de dominio**, y esa
  traducción decide después si algo se reintenta:

  | Respuesta | Excepción | Naturaleza |
  |---|---|---|
  | `404` o `disponible = false` | `ProductoNoDisponibleException` → `422` | determinista, no se reintenta |
  | timeout, `5xx`, conexión caída | `CatalogoNoDisponibleException` → `503` | transitoria, reintentar tiene sentido |

- **El JWT del cliente se propaga** a Catálogo con un interceptor, para que la
  llamada entre servicios no sea una puerta trasera sin autenticar.

Se acepta y se declara: **esta es la única llamada síncrona entre servicios del
sistema**. Cualquier otra sería una desviación de la arquitectura.

## Alternativas consideradas

### A. Réplica local del catálogo alimentada por eventos — *descartada*

Catálogo publicaría `producto.actualizado` y Pedidos mantendría su propia tabla
de precios. Al crear un pedido, leería de local, sin salir del proceso.

Es la opción de manual en una arquitectura orientada a eventos, y tiene
ventajas serias: Pedidos podría crear pedidos con Catálogo caído, y sin latencia
de red.

Se descarta porque **cambia la naturaleza del error**. Con réplica local, un
retraso en la propagación no se manifiesta como «no puedo validar» sino como
«validé con un precio viejo»: el pedido se acepta con un dato incorrecto, el
cliente ve un total que no coincide y el fallo se descubre tarde y en otro
sitio. Con la llamada síncrona, si algo va mal, **se sabe en el mismo instante y
lo sabe el cliente**, con un `503` explícito.

El coste añadido tampoco es menor: una tabla replicada, un consumidor más, y
una pregunta nueva —qué hacer si el evento de actualización llega desordenado—
para un sistema con cinco productos de ejemplo.

### B. Confiar en el precio que envía el cliente — *descartada*

Trivial de implementar. Se descarta por seguridad: es un agujero de negocio
directo, no un compromiso.

### C. Crear el pedido y validar después, por evento — *descartada*

`POST /api/pedidos` respondería `202 Accepted` y la validación llegaría luego.
Se descarta porque traslada al cliente un problema que el sistema puede
resolver ya: tendría que descubrir más tarde que su pedido no era válido, y
habría que modelar un estado nuevo de «pedido pendiente de validar» que no
aporta nada al dominio.

## Consecuencias

### Positivas

- **El precio es correcto en el instante decisivo**, sin ventanas de
  propagación.
- **El fallo es honesto y accionable**: `503` con mensaje, y reintentar tiene
  sentido de verdad.
- **El dominio no se entera de que hay HTTP.** `CrearPedidoUseCaseImpl` depende
  de `CatalogoPort`; en las pruebas ese puerto es `FakeCatalogo`, treinta líneas
  en memoria.
- Una vez creado el pedido, la copia es inmutable: si el precio sube mañana,
  este pedido no cambia ([ADR-003](ADR-003-una-base-por-servicio.md)).

### Negativas

- **Acoplamiento en disponibilidad**: con Catálogo caído **no se pueden crear
  pedidos**. Es el precio literal de esta decisión y no se disimula. Los pedidos
  ya creados siguen su curso con normalidad, porque el resto del flujo es
  asíncrono.
- **Latencia añadida** en el `POST`: una ida y vuelta HTTP por línea del pedido.
- **Catálogo tiene que estar arriba antes que Pedidos** para que el sistema
  funcione del todo, lo que introduce un orden de arranque recomendado.

### Riesgos aceptados

| Riesgo | Por qué se acepta |
|---|---|
| Un producto puede agotarse entre la validación y el descuento de stock | El descuento real lo hace Catálogo al consumir `pedido.confirmado`, y ahí vuelve a comprobar el stock; si no alcanza, la saga compensa |
| N líneas de pedido son N llamadas HTTP | Con pedidos de pocas líneas es irrelevante; si creciera, se añadiría un endpoint por lotes en Catálogo |

## Verificación

| Qué demuestra la decisión | Cómo se comprueba |
|---|---|
| Es la única llamada síncrona del sistema | `grep -rln "RestClient" services/*/src/main` → solo `order-service` |
| El precio no viene del cliente | `CrearPedidoRequest` solo acepta `productoId` y `cantidad` |
| El precio se congela | `pedido_linea.precio_unitario` se escribe con lo que devolvió Catálogo |
| Catálogo caído da 503 y no 500 | `PedidoControllerTest.catalogoCaido()` |
| Producto no disponible da 422 | `PedidoControllerTest.productoNoDisponible()` |
| El dominio no conoce HTTP | `CatalogoPort` está en `domain/client/`, sin un solo import de Spring |
| El JWT se propaga | `RestClientConfig.propagarJwt()`; sin él, Catálogo respondería `401` |
