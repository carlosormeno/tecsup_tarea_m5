package com.tecsup.app.micro.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * scanBasePackages apunta a `com.tecsup.app.micro`, el paquete padre común de
 * este servicio y de `shared`.
 *
 * Sin eso, Spring solo miraría dentro de `...micro.order` y no encontraría el
 * filtro de JWT, la configuración de seguridad ni el DLQController, que viven
 * en `...micro.shared`.
 *
 * El escaneo de entidades y repositorios JPA va aparte, en
 * {@code infrastructure.config.JpaConfig}, por el motivo que se explica allí.
 */
@SpringBootApplication(scanBasePackages = "com.tecsup.app.micro")
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
