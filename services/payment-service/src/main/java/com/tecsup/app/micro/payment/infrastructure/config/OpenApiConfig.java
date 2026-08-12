package com.tecsup.app.micro.payment.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Servicio de Pagos")
                .version("1.0")
                .description("""
                        Cobra y reembolsa. El cobro NO se dispara por HTTP:
                        lo desencadena el evento pedido.creado.

                        Publica: pago.confirmado, pago.rechazado
                        Consume: pedido.creado, pedido.cancelado
                        """));
    }
}
