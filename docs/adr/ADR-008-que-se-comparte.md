# ADR-008 · Qué se comparte entre servicios y qué no

| | |
|---|---|
| **Estado** | Aceptada |
| **Fecha** | 2026-08-10 |
| **Secciones del documento** | 2.2 Decisiones clave · 3.1 Módulos · 3.3 Comunicación |
| **Relacionada con** | [ADR-001](ADR-001-microservicios-frente-a-monolito.md) (las fronteras) · [ADR-002](ADR-002-arquitectura-hexagonal.md) (dónde vive cada cosa dentro del servicio) |

## Contexto

Con cinco servicios construidos por la misma persona en el mismo repositorio,
la tentación de compartir es constante y razonable: el filtro de JWT es idéntico
en los cinco, la cola de fallidos también, y el evento `PagoSolicitado` que
publica Pedidos tiene exactamente los mismos cinco campos que el
`PagoSolicitadoDTO` que lee Pagos. Duplicarlo parece un error de principiante.

Pero compartir tiene un coste que no se ve hasta que es tarde: **cada clase
compartida es un punto de acoplamiento en tiempo de compilación**. Si los cinco
servicios dependen de una clase `PedidoConfirmado` común, añadirle un campo
obliga a recompilar y desplegar los cinco a la vez — y entonces ya no son cinco
servicios independientes, sino un monolito repartido en cinco procesos.

La pregunta no es *si* compartir, sino **qué**.

## Decisión

**Se comparte la fontanería técnica; nunca los contratos.**

### Sí se comparte — módulo `shared`, 8 clases

| Paquete | Clases | Por qué se puede compartir |
|---|---|---|
| `dlq` | `FailedEvent`, `FailedEventRepository`, `DeadLetterQueue`, `DLQController` | No es dominio de nadie: es infraestructura de operación. Cambia por motivos técnicos, iguales para los cinco |
| `security` | `JwtTokenProvider`, `JwtAuthenticationFilter`, `SecurityConfig` | La validación de un JWT es la misma operación en todos. Si cambia el algoritmo, **debe** cambiar en los cinco a la vez |
| `messaging` | `TopicsDeReintento` | Declara topics de reintento y DLT con las particiones correctas. Es una regla de configuración de Kafka, no de negocio |

El criterio: **se comparte lo que, si cambia, debe cambiar en todos a la vez.**
Ahí el acoplamiento no es un defecto, es lo que se quiere.

### No se comparte — cada servicio lo define

| Qué | Ejemplo |
|---|---|
| **Eventos** | `PagoSolicitado` en Pedidos ≠ `PagoSolicitadoDTO` en Pagos |
| **DTOs de la API** | `PedidoResponse`, `EntregaResponse`… |
| **Modelos de dominio** | Cada uno tiene su `EstadoPedido` / `EstadoEntrega` |
| **Puertos** | `PedidoRepository`, `EntregaRepository`… |
| **Nombres de topics** | Una clase `Topics` por servicio, con solo los suyos |

Que un evento se defina dos veces no es duplicación por descuido: **son dos
puntos de vista del mismo hecho**. Pedidos publica lo que sabe; Pagos lee lo que
necesita. Jackson ignora los campos desconocidos, así que Pedidos puede añadir
un campo sin romper a nadie.

### El mecanismo que lo hace posible

Si el productor pusiera el nombre de su clase Java en la cabecera de tipo, el
consumidor intentaría instanciar
`com.tecsup.app.micro.order.domain.event.PagoSolicitado`, que en su servicio no
existe. Por eso ambos lados traducen a un **nombre lógico**:

```yaml
# order-service (productor)
spring.json.type.mapping: pagoSolicitado:com.tecsup.app.micro.order.domain.event.PagoSolicitado

# payment-service (consumidor)
spring.json.type.mapping: pagoSolicitado:com.tecsup.app.micro.payment.infrastructure.messaging.dto.PagoSolicitadoDTO
```

El contrato es **el nombre lógico y la forma del JSON**, no la clase.

## Alternativas consideradas

### A. Un módulo `common` con los eventos y modelos — *descartada*

Lo más cómodo de escribir. Se descarta porque convierte el despliegue en
conjunto: cualquier cambio en un evento obliga a recompilar los cinco. Es
exactamente el acoplamiento que [ADR-001](ADR-001-microservicios-frente-a-monolito.md)
quería eliminar, reintroducido por la puerta de atrás del `pom.xml`.

### B. No compartir absolutamente nada — *descartada*

Duplicar también el filtro JWT y la DLQ, para que la independencia sea total.
Se descarta por un motivo concreto: el filtro de JWT duplicado cinco veces
significa que un fallo de seguridad hay que arreglarlo en cinco sitios, con la
certeza de olvidar uno. Aquí la duplicación no compra independencia, compra
riesgo.

### C. Contratos formales con Avro y Schema Registry — *descartada*

Definir los eventos en `.avsc` y generar las clases. Es la solución seria para
compatibilidad de esquemas. Se descarta por alcance: añade un servicio más
(Schema Registry) y un paso de generación de código al build, para cinco eventos
que caben en una pantalla. Se menciona en el documento como evolución natural.

## Consecuencias

### Positivas

- **Ningún servicio importa clases de otro.** Es verificable con un `grep`.
- La corrección de la DLQ del 2026-08-12 se hizo **una vez** y benefició a los
  cinco.
- Cada servicio puede cambiar su representación interna de un evento sin
  avisar a nadie, mientras respete la forma del JSON.
- El módulo compartido es pequeño y fácil de auditar: 8 clases, ninguna con
  regla de negocio.

### Negativas

- **Hay duplicación real y visible**, y hay que explicarla cada vez que alguien
  la ve por primera vez.
- **El contrato no lo comprueba el compilador.** Si Pedidos renombra un campo
  del JSON, nada falla hasta que un evento llega a Pagos en ejecución. Se
  mitigó revisando los contratos campo a campo entre servicios, pero es una
  revisión manual.
- **`shared` es un punto de acoplamiento**: cambiarlo obliga a recompilar los
  cinco. Es asumido y acotado a fontanería que ya debía cambiar a la vez.
- La configuración del `type.mapping` hay que mantenerla sincronizada en dos
  archivos por evento.

### Riesgos aceptados

| Riesgo | Por qué se acepta |
|---|---|
| `shared` puede crecer hasta convertirse en el `common` que se rechazó | El criterio está escrito arriba y es comprobable: si una clase tiene regla de negocio, no entra |
| Un cambio de esquema incompatible se descubre en ejecución | Sin Schema Registry no hay otra forma; se compensa con la política de solo añadir campos, nunca renombrarlos ni quitarlos |

## Verificación

```bash
# Ningún servicio importa clases de otro servicio.
# Para cada uno, se buscan importaciones de los paquetes de los otros cuatro.
cd services
for s in order catalog payment delivery user; do
  otros=$(echo "order catalog payment delivery user" | tr ' ' '\n' | grep -v "^$s$" | paste -sd'|' -)
  grep -rEn "import com\.tecsup\.app\.micro\.($otros)\." $s-service/src/main/java
done
# -> vacío en los cinco (comprobado el 2026-08-12)

# shared no contiene regla de negocio
find shared/src/main/java -name "*.java" | wc -l    # 8
```

| Qué demuestra la decisión | Cómo se comprueba |
|---|---|
| El mismo evento existe dos veces, a propósito | `PagoSolicitado` (Pedidos) y `PagoSolicitadoDTO` (Pagos), en paquetes distintos |
| El contrato es el nombre lógico | `spring.json.type.mapping` en los `application.yaml` de ambos lados |
| `shared` no tiene dominio | Sus 8 clases son DLQ, seguridad y topics de reintento |
| La independencia es real | Cambiar `PedidoResponse` en Pedidos no obliga a recompilar ningún otro servicio |
