package com.tecsup.app.micro.order.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Servicio de Pedidos")
                .version("1.0")
                .description("""
                        Orquestador de la saga de pedidos.

                        Crear el pedido no cobra nada: la saga arranca en
                        POST /api/pedidos/{id}/pagar.

                        Publica: pedido.pago-solicitado, pedido.confirmado, pedido.entregado, pedido.cancelado
                        Consume: pago.confirmado, pago.rechazado, entrega.estado-cambiado
                        """));
    }
}
