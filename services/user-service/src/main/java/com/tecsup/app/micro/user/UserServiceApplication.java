package com.tecsup.app.micro.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * scanBasePackages apunta a `com.tecsup.app.micro`, el paquete padre
 * común de este servicio y de `shared`, para que Spring encuentre
 * también la seguridad y la DLQ compartidas.
 */
@SpringBootApplication(scanBasePackages = "com.tecsup.app.micro")
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
