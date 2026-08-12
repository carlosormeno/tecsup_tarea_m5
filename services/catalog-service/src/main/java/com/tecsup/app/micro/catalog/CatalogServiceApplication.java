package com.tecsup.app.micro.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * scanBasePackages apunta a `com.tecsup.app.micro`, el paquete padre común de
 * este servicio y de `shared`, para que Spring encuentre también la seguridad
 * y la DLQ compartidas.
 *
 * El escaneo de entidades y repositorios JPA va aparte, en
 * {@code infrastructure.config.JpaConfig}.
 */
@SpringBootApplication(scanBasePackages = "com.tecsup.app.micro")
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
