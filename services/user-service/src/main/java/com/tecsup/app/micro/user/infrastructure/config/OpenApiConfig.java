package com.tecsup.app.micro.user.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Servicio de Usuarios")
                .version("1.0")
                .description("""
                        Identidad del sistema. ÚNICO servicio que emite JWT;
                        los otros cuatro solo validan la firma.

                        Publica: nada.
                        Consume: pedido.entregado (suma puntos de fidelidad)

                        POST /auth/registro y POST /auth/login son los dos
                        únicos endpoints de negocio sin token de todo el
                        sistema: pedir un JWT para obtener un JWT es imposible.
                        """));
    }
}
