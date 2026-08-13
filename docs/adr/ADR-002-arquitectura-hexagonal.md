# ADR-002 · Arquitectura hexagonal dentro de cada servicio

| | |
|---|---|
| **Estado** | Aceptada |
| **Fecha** | 2026-08-09 |
| **Secciones del documento** | 2.1 Estilo arquitectónico · 3.1 Módulos · 8.1 Pruebas |
| **Relacionada con** | [ADR-001](ADR-001-microservicios-frente-a-monolito.md) (qué separa a los servicios) · [ADR-008](ADR-008-que-se-comparte.md) (qué cruza la frontera) |

## Contexto

[ADR-001](ADR-001-microservicios-frente-a-monolito.md) fija cinco fronteras
*entre* servicios, pero no dice nada de lo que pasa **dentro** de cada uno. Y el
riesgo real está ahí: cinco servicios pequeños, cada uno con su controlador que
llama a un repositorio de Spring Data que devuelve entidades JPA anotadas, que
viajan hasta el JSON de respuesta. Es el resultado por defecto de Spring Boot y
tiene tres consecuencias concretas:

- **La regla de negocio se dispersa** entre el controlador, el servicio y los
  `@Entity`. La máquina de estados de un pedido acabaría repartida en `if` por
  tres capas.
- **Probar exige levantar infraestructura.** Cualquier prueba del cálculo del
  total necesitaría un contexto de Spring y una base de datos.
- **Cambiar de tecnología obliga a tocar el negocio.** Si Kafka se sustituyera
  por otra cosa, habría que editar las clases que deciden si un pedido puede
  pasar a `PAGADO`.

Sobre eso pesa una restricción explícita del encargo: se pide **arquitectura
hexagonal pura**, y la organización de paquetes debe seguir la del proyecto de
Módulo 3 (`arq_m3_s3_eda_intro_lms`).

## Decisión

**Puertos y adaptadores en los cinco servicios**, con esta estructura de
paquetes idéntica en todos:

```
application/      XxxUseCase (interfaz) + XxxUseCaseImpl   ← puertos de entrada
domain/
  model/          agregados y objetos de valor            ← el hexágono
  event/          eventos de dominio + PublicadorEventos  ← puerto de salida
  exception/      excepciones de negocio
  repository/     XxxRepository                           ← puerto de salida
  client/         XxxPort (dependencias externas)         ← puerto de salida
infrastructure/
  persistence/    adapter · entity · mapper · repository  ← adaptadores
  web/            controller · dto
  messaging/      listener · publicador · dto
  config/         cableado de puertos con adaptadores
  security/
```

Tres reglas de dependencia, sin excepciones:

1. `domain` no importa nada de `application` ni de `infrastructure`.
2. `domain` no importa ningún framework: ni Spring, ni JPA, ni Kafka.
3. `application` no importa nada de `infrastructure`.

La única excepción admitida es `@Transactional` de Spring en `application`, por
pragmatismo: gestionar la transacción a mano en cada caso de uso no aporta nada
y sí ruido.

El cableado vive en `infrastructure/config/BeanConfiguration.java`, que registra
cada implementación detrás de su interfaz. Los casos de uso son **objetos Java
normales**, sin anotaciones de Spring.

## Alternativas consideradas

### A. Capas clásicas (controller → service → repository) — *descartada*

Lo que sale por defecto. Se descarta porque incumple el requisito de hexagonal
puro y porque la dependencia apunta hacia la base de datos: el negocio termina
dependiendo de la persistencia, que es exactamente al revés de lo que interesa.

### B. Hexagonal con los puertos de salida en `application/port/out` — *descartada*

Es la disposición más extendida en la literatura de hexagonal, y de hecho fue la
primera que se escribió aquí. Se descarta por la restricción del encargo: la
estructura debe seguir la de Módulo 3, que coloca los puertos de salida en
`domain/repository`, `domain/event` y `domain/client`.

No es una concesión dolorosa: **un repositorio en el dominio es DDD de manual**
—el patrón Repository nació ahí—, y el argumento de fondo se conserva intacto,
porque lo que importa es hacia dónde apunta la dependencia y no en qué carpeta
está el archivo.

### C. Compartir las clases de dominio entre servicios — *descartada*

Un módulo `common` con `Pedido`, `Producto` y los eventos, para no repetirlos.
Se descarta en [ADR-008](ADR-008-que-se-comparte.md): ataría los cinco
servicios a compilar juntos y convertiría cada cambio de un evento en un
despliegue simultáneo de todos.

## Consecuencias

### Positivas

- **Las pruebas de negocio no necesitan infraestructura.** La saga completa se
  prueba con dobles escritos a mano (`Fakes.java`), sin Spring, sin Postgres y
  sin Kafka, en milisegundos. Que eso sea barato es la medida de si el hexágono
  está bien cerrado.
- **La regla de negocio está en un sitio y se puede señalar.** La máquina de
  estados del pedido es un archivo: `domain/model/EstadoPedido.java`.
- **Cambiar de tecnología es cambiar un adaptador.** Sustituir Kafka tocaría
  `infrastructure/messaging/`; `PublicadorEventos` y el dominio no se enterarían.
- El dominio no conoce siquiera el nombre de los topics: son una decisión de
  transporte y viven en `infrastructure/messaging/Topics.java`.

### Negativas

- **Más archivos y más traducción.** El mismo pedido existe como agregado, como
  `PedidoJpaEntity` y como `PedidoResponse`, con dos mapeadores entre medias.
  Para un CRUD sin reglas, esto es puro coste.
- **Cada evento se define dos veces**, una en quien publica y otra en quien
  consume, con nombres distintos (`PagoSolicitado` / `PagoSolicitadoDTO`). Es
  deliberado ([ADR-008](ADR-008-que-se-comparte.md)) pero se paga.
- **Es fácil de violar sin darse cuenta.** Basta un `import` cómodo para que el
  dominio empiece a conocer JPA. Por eso la verificación de abajo es mecánica y
  no una revisión a ojo.

## Verificación

Repetible en cualquier servicio, desde `src/main/java/com/tecsup/app/micro/<servicio>`:

```bash
# 1. El dominio no depende de las otras capas
grep -rn "import com.tecsup.*\(application\|infrastructure\)" domain/

# 2. El dominio no conoce ningún framework
grep -rn "import \(org.springframework\|jakarta.persistence\|org.apache.kafka\)" domain/

# 3. La aplicación no depende de la infraestructura
grep -rn "import com.tecsup.*infrastructure" application/
```

**Los tres deben devolver vacío** en los cinco servicios. Única excepción
admitida: `@Transactional` en `application`.

| Qué demuestra la decisión | Cómo se comprueba |
|---|---|
| El hexágono está cerrado | Los tres `grep` vacíos en los 5 servicios |
| El dominio se prueba sin infraestructura | `PedidoTest`, `PagoTest`, `EntregaTest`: sin `@SpringBootTest`, milisegundos |
| Los puertos se sustituyen de verdad | `Fakes.java`: implementaciones en memoria de los puertos de salida, escritas a mano |
| El cableado está aislado | `BeanConfiguration.java` es el único sitio donde una interfaz se une a su implementación |
