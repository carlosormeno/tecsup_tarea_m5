package com.tecsup.app.micro.delivery.infrastructure.config;

import com.tecsup.app.micro.delivery.infrastructure.messaging.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Declara el único topic que este servicio publica. */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic entregaEstadoCambiado() {
        return TopicBuilder.name(Topics.ENTREGA_ESTADO_CAMBIADO)
                .partitions(3).replicas(1).build();
    }
}
