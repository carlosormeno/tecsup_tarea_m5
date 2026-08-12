# =============================================================================
# ETAPA 1: BUILD
#
# El contexto de build es ./services, no ./services/catalog-service, porque
# este servicio depende del módulo shared:
#
#   build:
#     context: ./services
#     dockerfile: catalog-service/Dockerfile
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

COPY pom.xml .
COPY shared shared
COPY catalog-service catalog-service

RUN mvn -B -pl catalog-service -am clean package -DskipTests

# =============================================================================
# ETAPA 2: RUNTIME
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /build/catalog-service/target/*.jar /app/catalog-service.jar

RUN mkdir -p /logs && chown -R spring:spring /logs /app
USER spring

EXPOSE 8082

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/catalog-service.jar"]
