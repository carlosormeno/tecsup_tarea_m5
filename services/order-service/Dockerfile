# =============================================================================
# ETAPA 1: BUILD
# Compila dentro de la imagen, así "podman-compose build" funciona sin
# necesidad de correr "mvn package" antes.
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

# Primero solo el pom: si no cambia, Podman reusa la capa de dependencias
# y las siguientes construcciones no vuelven a descargar medio Maven Central.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# =============================================================================
# ETAPA 2: RUNTIME
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /build/target/*.jar /app/order-service.jar

# Carpeta donde el servicio escribe el log que después lee Promtail
RUN mkdir -p /logs && chown -R spring:spring /logs /app
USER spring

EXPOSE 8083

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/order-service.jar"]
