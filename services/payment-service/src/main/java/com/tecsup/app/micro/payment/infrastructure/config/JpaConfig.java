package com.tecsup.app.micro.payment.infrastructure.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Extiende el escaneo de JPA al paquete compartido (entidad `FailedEvent` y su
 * repositorio).
 *
 * Aparte de la clase principal porque @EnableJpaRepositories no es condicional
 * y rompería las pruebas de rodaja que no levantan JPA.
 */
@Configuration
@EntityScan("com.tecsup.app.micro")
@EnableJpaRepositories("com.tecsup.app.micro")
public class JpaConfig {
}
