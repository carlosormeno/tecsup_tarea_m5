package com.tecsup.app.micro.delivery.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Servicio de Entregas")
                .version("1.0")
                .description("""
                        Asigna repartidores y reporta el avance de la entrega.

                        Publica: entrega.estado-cambiado
                        Consume: pedido.confirmado

                        No hay endpoint para crear entregas: solo nacen de un
                        pedido confirmado. El PATCH de estado lo usa la
                        aplicación del repartidor, y cada cambio publica un
                        evento que hace avanzar el pedido en order-service.
                        """));
    }
}
