package com.tecsup.app.micro.order.infrastructure.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Extiende el escaneo de JPA al paquete compartido, donde viven la entidad
 * `FailedEvent` y su repositorio.
 *
 * Va en una clase aparte y NO en OrderServiceApplication a propósito:
 * @EnableJpaRepositories no es condicional, así que puesta en la clase
 * principal se activaría también en las pruebas de rodaja @WebMvcTest, que no
 * levantan JPA, y el contexto fallaría con "No bean named
 * 'entityManagerFactory' available". Como @Configuration suelta, el filtro de
 * tipos de @WebMvcTest la descarta y las pruebas web siguen siendo ligeras.
 */
@Configuration
@EntityScan("com.tecsup.app.micro")
@EnableJpaRepositories("com.tecsup.app.micro")
public class JpaConfig {
}
