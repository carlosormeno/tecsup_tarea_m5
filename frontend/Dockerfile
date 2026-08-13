# =============================================================================
# ETAPA 1: BUILD
#
# VITE_MISMO_ORIGEN=true hace que el front use rutas relativas: en Kubernetes
# el Ingress sirve el front y las APIs bajo el mismo origen, así que no hace
# falta saber en qué puerto vive cada servicio ni habilitar CORS.
# =============================================================================
FROM node:22-alpine AS build

WORKDIR /build

COPY package*.json ./
RUN npm ci

COPY . .

ENV VITE_MISMO_ORIGEN=true
RUN npm run build

# =============================================================================
# ETAPA 2: RUNTIME
#
# Nginx sirviendo estáticos. El front es una sola página, así que cualquier
# ruta desconocida devuelve index.html.
# =============================================================================
FROM nginx:1.27-alpine

COPY --from=build /build/dist /usr/share/nginx/html

RUN printf 'server {\n\
  listen 8080;\n\
  root /usr/share/nginx/html;\n\
  index index.html;\n\
  location / {\n\
    try_files $uri $uri/ /index.html;\n\
  }\n\
}\n' > /etc/nginx/conf.d/default.conf \
 && sed -i 's/listen\s*80;/listen 8080;/' /etc/nginx/conf.d/default.conf

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
