package com.tecsup.app.micro.order.infrastructure.config;

import com.tecsup.app.micro.order.infrastructure.messaging.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    private static final int PARTICIONES = 3;
    private static final short REPLICAS = 1;   // un solo broker en desarrollo

    @Bean
    public NewTopic pedidoCreado() {
        return TopicBuilder.name(Topics.PEDIDO_CREADO)
                .partitions(PARTICIONES).replicas(REPLICAS).build();
    }

    @Bean
    public NewTopic pedidoConfirmado() {
        return TopicBuilder.name(Topics.PEDIDO_CONFIRMADO)
                .partitions(PARTICIONES).replicas(REPLICAS).build();
    }

    @Bean
    public NewTopic pedidoEntregado() {
        return TopicBuilder.name(Topics.PEDIDO_ENTREGADO)
                .partitions(PARTICIONES).replicas(REPLICAS).build();
    }

    @Bean
    public NewTopic pedidoCancelado() {
        return TopicBuilder.name(Topics.PEDIDO_CANCELADO)
                .partitions(PARTICIONES).replicas(REPLICAS).build();
    }
}
