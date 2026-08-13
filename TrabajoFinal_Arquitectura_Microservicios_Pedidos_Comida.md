# Documento de Arquitectura de Software – Sistema de Pedidos de Comida

**Arquitectura de Software** · V1.1

## 1. Introducción General

### 1.1 Propósito del documento

Documentar la arquitectura de un sistema de pedidos de comida basado en microservicios.

### 1.2 Alcance del sistema

El sistema permite a los usuarios realizar pedidos desde múltiples restaurantes, gestionar pagos y coordinar entregas.

### 1.3 Audiencia y nivel técnico esperado

Este documento está dirigido a desarrolladores backend, DevOps, QA y arquitectos de software con conocimientos intermedios en arquitectura distribuida.

## 2. Visión Arquitectónica General

### 2.1 Estilo arquitectónico utilizado

Arquitectura de Microservicios, con servicios independientes desplegados en contenedores Docker.

### 2.2 Decisiones arquitectónicas clave

Separación de funcionalidades en servicios independientes, comunicación vía REST y eventos Kafka, uso de base de datos por servicio.

### 2.3 Diagramas de alto nivel

El sistema consta de servicios como: Servicio de Usuarios, Servicio de Pedidos, Servicio de Catálogo, Servicio de Entregas, y Servicio de Pagos.

## 3. Componentes del Sistema

### 3.1 Módulos principales y responsabilidades

Cada servicio tiene responsabilidades bien definidas. Por ejemplo, el Servicio de Pedidos gestiona la creación y seguimiento de pedidos.

### 3.2 Interfaces y APIs expuestas

APIs REST para clientes web/móviles, y APIs internas entre servicios. Contratos documentados con OpenAPI o similares.

### 3.3 Comunicación entre componentes

Principalmente asincrónica vía Kafka, con REST para operaciones sincrónicas necesarias.

### 3.4 Integración con sistemas externos (OPCIONAL)

Integración con pasarelas de pago como Stripe y servicios de geolocalización para entregas.

## 4. Detalle del Estilo Arquitectónico

### 4.2 Arquitectura de Microservicios

Cada servicio es autónomo, despliegue independiente, y mantiene su propia base de datos. Uso de Kubernetes para orquestación.

## 5. Seguridad

### 5.1 Autenticación y autorización

Implementación con JWT para sesiones de usuario. Validación de permisos por roles.

## 6. Escalabilidad y Rendimiento

### 6.1 Estrategias de escalabilidad

Escalado horizontal automático en Kubernetes por uso de CPU y cola de eventos.

### 6.2 Balanceo de carga

Nginx y Kubernetes Ingress Controller para balanceo de solicitudes REST.

### 6.3 Tolerancia a fallos y alta disponibilidad

Replicación de servicios críticos, reintento en servicios consumidores de eventos y circuit breakers.

## 7. DevOps y Despliegue (OPCIONAL)

### 7.1 Estrategia de CI/CD

Pipeline en GitHub Actions con pruebas automáticas, builds de Docker, y despliegue en clúster Kubernetes.

### 7.2 Infraestructura como código

Configuraciones de servicios en Helm Charts.

### 7.3 Ambientes de despliegue

Ambientes separados para desarrollo, staging y producción, cada uno en un namespace diferente.

## 8. Calidad y Mantenibilidad

### 8.1 Estrategias de pruebas

Pruebas unitarias y de integración.

### 8.2 Observabilidad

Uso de Prometheus para métricas, Grafana para dashboards y ZipKin para seguimiento de transacciones entre microservicios.

## 9. Anexos y Referencias (OPCIONAL)

### 9.1 Glosario

Incluye términos como microservicio, broker de eventos, CI/CD, etc.

### 9.2 Referencias y normativas

Guía de 12 factores para microservicios, documentación de OpenAPI, prácticas de DevSecOps.

### 9.3 Documentación técnica relacionada

Enlaces a repositorios, Swagger UI, y documentación de infraestructura.
