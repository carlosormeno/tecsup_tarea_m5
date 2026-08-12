package com.tecsup.app.micro.catalog.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Servicio de Catálogo")
                .version("1.0")
                .description("""
                        Fuente de verdad de precios y disponibilidad.

                        Publica: nada. Es el único servicio que solo consume.
                        Consume: pedido.confirmado (descuenta stock),
                                 pedido.cancelado (repone stock)

                        GET /api/productos/{id} es la única llamada síncrona
                        entre servicios de todo el sistema: la hace
                        order-service antes de crear cada pedido.
                        """));
    }
}
