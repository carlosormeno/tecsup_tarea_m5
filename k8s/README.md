# Despliegue en Kubernetes (kind sobre Podman)

Alternativa al despliegue de Podman Compose, no sustituto: los dos conviven con
papeles distintos ([ADR-012](../docs/adr/ADR-012-kubernetes-con-kind.md)).

**No se levantan a la vez.** Kafka, Postgres y los cinco servicios no caben dos
veces en la misma máquina. Antes de empezar: `podman-compose down` (sin `-v`,
para no borrar las bases del otro despliegue).

## Requisitos

| | |
|---|---|
| Podman | con **10 GB** de memoria y 6 CPUs |
| kind | v0.32 o superior |
| kubectl | v1.30 o superior |

La memoria es un requisito, no una recomendación. Con los 5.8 GB por defecto no
caben los cinco servicios Java y el kernel empieza a matar contenedores en
mitad de una demostración:

```bash
podman machine stop
podman machine set --memory 10240 --cpus 6
podman machine start
```

Todos los comandos de abajo necesitan:

```bash
export KIND_EXPERIMENTAL_PROVIDER=podman
```

## 1. Clúster e infraestructura

```bash
kind create cluster --config k8s/kind-cluster.yaml

kubectl apply -f k8s/manifiestos/00-namespace.yaml \
              -f k8s/manifiestos/01-config.yaml \
              -f k8s/manifiestos/02-postgres.yaml \
              -f k8s/manifiestos/03-zookeeper.yaml \
              -f k8s/manifiestos/04-kafka.yaml

kubectl wait --for=condition=ready pod -l app=kafka -n pedidos --timeout=300s
```

## 2. Construir las seis imágenes

**El contexto es `services/`, no la carpeta de cada servicio.** El POM agregador
declara los seis módulos y Maven aborta si falta alguno, aunque solo se
construya uno con `-pl`.

```bash
cd services
for s in user catalog order payment delivery; do
  podman build -f $s-service/Dockerfile -t pedidos/$s-service:1.0 .
done
cd ../frontend && podman build -t pedidos/frontend:1.0 . && cd ..
```

Tarda: cada imagen compila con Maven dentro del contenedor. La primera vez,
15–20 minutos.

## 3. Cargarlas en el nodo

Los manifiestos llevan `imagePullPolicy: Never`, así que sin este paso los pods
quedan en `ErrImageNeverPull`.

```bash
for i in user-service catalog-service order-service payment-service delivery-service frontend; do
  podman save localhost/pedidos/$i:1.0 -o /tmp/$i.tar
  kind load image-archive /tmp/$i.tar --name pedidos-comida
  rm /tmp/$i.tar
done
```

> **Por qué por archivo y no con `kind load docker-image`.** Esa vía falla de
> forma intermitente con el proveedor de Podman: responde `image not present
> locally` para imágenes que `podman image inspect` sí resuelve. La carga por
> archivo evita la resolución de nombres y funciona siempre.

## 4. Servicios, front y observabilidad

```bash
kubectl apply -f k8s/manifiestos/05-observabilidad.yaml \
              -f k8s/manifiestos/10-user-service.yaml \
              -f k8s/manifiestos/11-catalog-service.yaml \
              -f k8s/manifiestos/12-order-service.yaml \
              -f k8s/manifiestos/13-payment-service.yaml \
              -f k8s/manifiestos/14-delivery-service.yaml \
              -f k8s/manifiestos/20-frontend.yaml

kubectl get pods -n pedidos -w      # esperar a 14 pods 1/1
```

## 5. Ingress

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl wait --namespace ingress-nginx --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller --timeout=300s

kubectl apply -f k8s/manifiestos/30-ingress.yaml
```

## 6. Autoescalado

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl patch -n kube-system deployment metrics-server --type=json \
  -p '[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'

kubectl apply -f k8s/manifiestos/40-hpa.yaml
kubectl get hpa -n pedidos
```

El parche es obligatorio en kind: sin él, metrics-server no confía en el
certificado del kubelet y el HPA se queda en `<unknown>/70%` para siempre.

## Acceso

Todo bajo un único origen, gracias al Ingress:

| | |
|---|---|
| Front | http://localhost:8000 · `carlos@test.com` / `password123` |
| Grafana | http://localhost:8000/grafana · `admin` / `admin` |
| Zipkin | http://localhost:8000/zipkin |
| Prometheus | http://localhost:8000/prometheus |
| Postgres | `localhost:30432` |
| Kafka | `localhost:30092` |

Swagger no está en el Ingress porque las rutas chocarían entre servicios:

```bash
kubectl port-forward -n pedidos svc/order-service 8083:8083
# http://localhost:8083/swagger-ui.html
```

## Comprobar que funciona

```bash
kubectl get pods -n pedidos                          # 14 pods, todos 1/1

curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8000/            # 200
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8000/api/productos  # 401

# Con la cabecera que manda el navegador. SIN ella no se prueba lo mismo:
# curl no envía Origin, y un POST del navegador sí.
curl -s -X POST http://localhost:8000/auth/login \
  -H "Content-Type: application/json" \
  -H "Origin: http://localhost:8000" \
  -d '{"email":"carlos@test.com","password":"password123"}'
```

## Problemas conocidos

| Síntoma | Causa | Solución |
|---|---|---|
| Kafka en `CrashLoopBackOff`, log de 4 líneas | Kubernetes inyecta `KAFKA_PORT=tcp://…` por el Service homónimo | Ya resuelto: `enableServiceLinks: false` |
| Kafka arranca y no queda nunca `Ready` | El broker se conecta a sí mismo; el Service solo enruta a pods `Ready` | Ya resuelto: `publishNotReadyAddresses: true` |
| `ErrImageNeverPull` | Falta el prefijo `localhost/` en el nombre de la imagen | Ya resuelto en los manifiestos |
| `kind load` no encuentra una imagen que sí existe | Proveedor experimental de Podman | Usar `image-archive`, paso 3 |
| `403 Invalid CORS request` al hacer login | Un `POST` manda `Origin` aunque sea del mismo origen | Ya resuelto: `setAllowedOriginPatterns` |
| El clúster no responde tras reiniciar Podman | El nodo se apagó de golpe y el puerto del API cambia | Recrear: `kind delete cluster` y volver al paso 1 |

## Borrar todo

```bash
kind delete cluster --name pedidos-comida
```
