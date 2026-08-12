package com.tecsup.app.micro.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * scanBasePackages apunta a `com.tecsup.app.micro`, el paquete padre
 * común de este servicio y de `shared`, para que Spring encuentre
 * también la seguridad y la DLQ compartidas.
 */
@SpringBootApplication(scanBasePackages = "com.tecsup.app.micro")
public class DeliveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryServiceApplication.class, args);
    }
}
