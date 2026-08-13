# ADR-012 · Kubernetes con kind, además de Podman Compose

| | |
|---|---|
| **Estado** | Aceptada |
| **Fecha** | 2026-08-12 |
| **Sustituye parcialmente a** | [ADR-009](ADR-009-podman-compose.md), que descartaba Kubernetes |
| **Secciones del documento** | 4.2 Despliegue · 6.1 Escalabilidad · 6.2 Balanceo · 7.2 Infraestructura como código |
| **Relacionada con** | [ADR-001](ADR-001-microservicios-frente-a-monolito.md) (despliegue independiente) · [ADR-011](ADR-011-jwt-clave-simetrica.md) (CORS deja de hacer falta) |

## Contexto

[ADR-009](ADR-009-podman-compose.md) descartó Kubernetes con un argumento que
sigue siendo válido para lo que evaluaba: **para demostrar la saga, la
compensación, la DLQ y la trazabilidad, Kubernetes no aporta nada** que no
demuestre igual un archivo de compose, y sí añade piezas que pueden fallar
delante del profesor.

Lo que ese ADR no ponderó bien es que **tres puntos del enunciado quedaban sin
cubrir**, y no por decisión de alcance sino por carencia:

| Punto | Con solo compose |
|---|---|
| 4.2 · orquestación | Sin orquestador |
| 6.1 · autoescalado | Escalado manual; el reparto por particiones de Kafka sí, pero sin autoescalado |
| 6.2 · balanceo de carga | **Nada.** El front llamaba a cinco puertos distintos |

El 6.2 era el punto más flojo del trabajo. Y la restricción práctica que había
detrás —5.8 GiB en la máquina de Podman— dejó de existir al subirla a 10 GiB.

## Decisión

**Añadir un despliegue en Kubernetes sobre kind con el proveedor de Podman**,
sin retirar el de compose. Los dos conviven con papeles distintos:

| | Podman Compose | Kubernetes (kind) |
|---|---|---|
| **Para qué** | Desarrollo diario y demostración de la saga | Demostrar orquestación, balanceo y autoescalado |
| **Servicios** | Desde el IDE, contra la infraestructura | Todo dentro del clúster |
| **Acceso** | Cinco puertos + herramientas en sus puertos | **Un solo origen**: `http://localhost:8000` |
| **CORS** | Necesario: es cross-origin | **Innecesario**: el Ingress unifica el origen |
| **Logs** | Los servicios escriben archivos; Promtail monta la carpeta | Promtail lee `/var/log/pods` del nodo |

**No son simultáneos.** Kafka, Postgres y los cinco servicios no caben dos
veces en la misma máquina; levantar uno exige bajar el otro. Está documentado en
el README de `k8s/`.

Contenido de [`k8s/`](../../k8s/): configuración del clúster kind y 14
manifiestos —namespace, ConfigMap y Secret, Postgres como StatefulSet,
Zookeeper, Kafka, observabilidad completa, los cinco servicios, el front,
Ingress y HPA.

## Alternativas consideradas

### A. Quedarse solo con compose — *descartada*

Es lo que decía [ADR-009](ADR-009-podman-compose.md). Se descarta porque deja
tres puntos del enunciado sin cubrir, y uno de ellos —el balanceo— sin nada que
enseñar. El coste de añadir Kubernetes resultó ser una tarde de trabajo, no una
reescritura.

### B. Migrar del todo a Kubernetes y retirar el compose — *descartada*

Tentador por coherencia. Se descarta por dos razones. La primera es de riesgo:
el despliegue de compose está verificado de punta a punta desde hace días, y el
de Kubernetes se acaba de verificar; para una sustentación conviene tener el
camino conocido disponible. La segunda es de ciclo de trabajo: con compose se
arranca un servicio desde el IDE y se depura al instante; en Kubernetes hay que
reconstruir la imagen, cargarla en el nodo y reiniciar el pod.

### C. Kubernetes real (un proveedor de nube o k3s en una máquina) — *descartada*

Más fiel a producción. Se descarta por coste y porque no añade nada
demostrable: el `Deployment`, el `Service`, el `Ingress` y el `HPA` son los
mismos objetos.

### D. Helm en lugar de manifiestos sueltos — *descartada*

Sería lo correcto para gestionar varios entornos. Se descarta porque con un solo
entorno, un chart introduce una capa de plantillas sobre catorce archivos que ya
se leen bien. Los manifiestos planos, además, se dejan leer por quien no conoce
Helm — que en una corrección importa.

## Consecuencias

### Positivas

- **Cubre 4.2, 6.1 y 6.2**, que eran las carencias reales.
- **Un solo origen**: el front y las cinco APIs bajo `http://localhost:8000`,
  enrutadas por prefijo de ruta. **CORS deja de hacer falta**, lo que enseña
  algo que suele pasar desapercibido: CORS no es un requisito del sistema, es
  una consecuencia de cómo se despliega.
- **Dos niveles de balanceo visibles**: el `Service` repartiendo entre pods
  (nivel 4) y el `Ingress` decidiendo por ruta (nivel 7). Más el tercero, que no
  es HTTP: Kafka repartiendo particiones.
- **La observabilidad mejora respecto a compose.** Promtail va como DaemonSet
  leyendo `/var/log/pods`: los servicios ya no escriben archivos ni comparten
  volumen con nadie, y cada línea llega etiquetada con `servicio`, `pod` y
  `nivel` desde la propia API de Kubernetes.
- Confirma lo que [ADR-009](ADR-009-podman-compose.md) afirmaba sin poder
  probarlo: **la aplicación no impedía el paso a Kubernetes.** No se cambió una
  línea de código de negocio. Todo fueron manifiestos, dos ajustes de
  `Dockerfile` y una variable de entorno en el front.

### Negativas

- **Dos despliegues que mantener.** Un cambio de configuración hay que llevarlo
  al compose y a los manifiestos.
- **El ciclo de cambio es mucho más lento**: reconstruir imagen, cargarla en el
  nodo, reiniciar el pod. Minutos frente a segundos.
- **Un nodo**: no se demuestra tolerancia a fallos de nodo, ni replicación de
  broker o base de datos.
- **Sin persistencia real**: Zipkin guarda en memoria y el clúster se recrea con
  frecuencia.
- **Cinco fallos que solo aparecen en este despliegue**, ninguno con un mensaje
  de error que apunte a su causa. Se detallan abajo porque son la parte
  transferible de este ADR.

### Riesgos aceptados

| Riesgo | Por qué se acepta |
|---|---|
| Los dos despliegues pueden divergir | Un solo entorno y un solo autor; el `ConfigMap` refleja las mismas variables que el compose |
| El clúster no sobrevive al reinicio de la máquina de Podman | Recrearlo son dos minutos y los manifiestos lo reconstruyen entero |
| `imagePullPolicy: Never` obliga a cargar las imágenes a mano | Es lo correcto sin registro; con uno, sería `Always` y un `docker push` |

## Lo que costó: cinco fallos que no aparecen en compose

Se dejan escritos porque explican por qué los manifiestos son como son, y
porque ninguno da un mensaje que lleve a su causa.

### 1. Kafka muere al configurarse

Kubernetes inyecta en cada pod variables heredadas de los enlaces de Docker por
cada Service del namespace. Como el Service se llama `kafka`, el pod recibía
`KAFKA_PORT=tcp://10.96.x.x:9092`. La imagen de Confluent convierte **toda**
variable `KAFKA_*` en propiedad del broker, así que arrancaba con
`port=tcp://…`, avisaba de que `port` está obsoleto y moría. Log de cuatro
líneas, ninguna mencionando la causa.

**Corrección:** `enableServiceLinks: false`.

### 2. Bloqueo circular de arranque del broker

El broker se anuncia como `kafka:9092` y su propio controlador se conecta ahí.
Pero un `Service` solo enruta a pods **Ready**, el pod no está Ready hasta que
la sonda diga que el broker responde, y el broker no responde porque no se
alcanza a sí mismo.

**Corrección:** `publishNotReadyAddresses: true` en el Service.

### 3. Las imágenes existen pero el pod no las encuentra

Podman antepone `localhost/` a toda imagen construida en local. Los manifiestos
decían `pedidos/user-service:1.0`, que containerd interpreta como
`docker.io/pedidos/user-service:1.0` — inexistente en el nodo. Con
`imagePullPolicy: Never`, el pod queda en `ErrImageNeverPull` sin explicar que
el problema es el nombre.

**Corrección:** `localhost/pedidos/<servicio>:1.0` en los seis manifiestos.

### 4. `kind load docker-image` deja de encontrar imágenes que sí existen

Tras reconstruir las imágenes, `kind load docker-image
localhost/pedidos/user-service:1.0` responde **`image not present locally`**,
mientras `podman image inspect` la resuelve sin problema —y por los dos nombres,
con y sin el prefijo `localhost/`—.

Es el proveedor experimental de Podman normalizando el nombre de otra forma. No
merece la pena pelearse: kind tiene una segunda vía que evita la resolución de
nombres por completo.

**Corrección:** exportar y cargar el archivo.

```bash
podman save localhost/pedidos/user-service:1.0 -o /tmp/img.tar
kind load image-archive /tmp/img.tar --name pedidos-comida
```

### 5. `403 Invalid CORS request` al entrar desde el propio Ingress

El front servido por el Ingress hacía login contra su mismo origen y recibía un
`403`. La causa no es evidente: **un `POST` manda la cabecera `Origin` aunque la
petición sea del mismo origen**, y el filtro de CORS de Spring la valida
igualmente contra la lista permitida, que solo contenía `http://localhost:5173`.

Lo que lo hizo difícil de ver es que **con `curl` no se reproduce**: curl no
envía `Origin`. Las comprobaciones del Ingress hechas con curl daban `200` sobre
un camino que el navegador rechazaba.

**Corrección:** `setAllowedOriginPatterns` con `http://localhost:*`, que cubre
los dos despliegues —5173 con Vite y 8000 tras el Ingress— en lugar de una lista
de orígenes exactos.

**La lección, que vale más que la corrección:** una API se verifica desde el
cliente que la va a usar. Un `200` en curl no prueba que el navegador pueda.

### Y uno que no era de Kubernetes, pero salió aquí

Los cinco `Dockerfile` copiaban solo `pom.xml`, `shared` y su propio módulo,
mientras el POM agregador declara los seis. Maven abortaba con *«Child module
/build/order-service does not exist»*. **Nunca se habían llegado a construir**:
el error llevaba ahí desde que se escribieron. Corregido con `COPY . .` y un
`.dockerignore` que deja fuera los `target/`.

## Verificación

Todo lo de esta tabla se comprobó en ejecución el 2026-08-12.

| Qué demuestra la decisión | Cómo se comprueba | Resultado |
|---|---|---|
| El sistema entero corre en Kubernetes | `kubectl get pods -n pedidos` | **14 pods**, todos `1/1 Running`, 0 reinicios |
| El front se sirve desde el clúster | `curl http://localhost:8000/` | `200` |
| Ningún endpoint de negocio es público | `curl http://localhost:8000/api/productos` | `401` |
| El Ingress enruta por prefijo al servicio correcto | `POST http://localhost:8000/auth/login` | `200` con token |
| Prometheus descubre los cinco servicios | `/prometheus/api/v1/targets` | los 5, `up` |
| Las tres interfaces responden bajo subruta | `/grafana`, `/zipkin`, `/prometheus` | `200`, `200`, `200` |
| El login funciona **como lo hace el navegador** | `POST /auth/login` con cabecera `Origin` | `200` con token; el preflight `OPTIONS`, `200` |
| No hizo falta tocar código de negocio | `git diff` del despliegue | solo manifiestos, `Dockerfile` y una variable del front |
