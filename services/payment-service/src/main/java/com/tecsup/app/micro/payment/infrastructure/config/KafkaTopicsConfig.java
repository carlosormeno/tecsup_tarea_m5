package com.tecsup.app.micro.payment.infrastructure.config;

import com.tecsup.app.micro.payment.infrastructure.messaging.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declara los topics que este servicio PUBLICA.
 *
 * Tres particiones permiten hasta tres instancias del mismo grupo consumiendo
 * en paralelo; como la clave es el id del pedido, sus eventos van siempre a la
 * misma partición y se procesan en orden.
 */
@Configuration
public class KafkaTopicsConfig {

    private static final int PARTICIONES = 3;
    private static final short REPLICAS = 1;   // un solo broker en desarrollo

    @Bean
    public NewTopic pagoConfirmado() {
        return TopicBuilder.name(Topics.PAGO_CONFIRMADO)
                .partitions(PARTICIONES).replicas(REPLICAS).build();
    }

    @Bean
    public NewTopic pagoRechazado() {
        return TopicBuilder.name(Topics.PAGO_RECHAZADO)
                .partitions(PARTICIONES).replicas(REPLICAS).build();
    }
}
