# =============================================================================
# ETAPA 1: BUILD
#
# OJO: el contexto de build es ./services, NO ./services/order-service, porque
# este servicio depende del módulo shared. En el compose se declara así:
#
#   build:
#     context: ./services
#     dockerfile: order-service/Dockerfile
#
# Se copia todo y se construye de una vez con -am (also make dependencies).
# Se pierde algo de caché de capas respecto a copiar solo los pom primero, a
# cambio de un build que no falla al resolver el módulo hermano.
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

# Se copia TODO el contexto (services/), no solo este módulo y shared.
#
# El POM agregador declara los seis módulos, y Maven aborta si alguno
# falta aunque se construya uno solo con -pl: "Child module ... does not
# exist". Lo que evita que el contexto pese cientos de megas es el
# .dockerignore, que deja fuera los target/.
COPY . .

RUN mvn -B -pl order-service -am clean package -DskipTests

# =============================================================================
# ETAPA 2: RUNTIME
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /build/order-service/target/*.jar /app/order-service.jar

# Carpeta donde el servicio escribe el log que después lee Promtail
RUN mkdir -p /logs && chown -R spring:spring /logs /app
USER spring

EXPOSE 8083

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/order-service.jar"]
