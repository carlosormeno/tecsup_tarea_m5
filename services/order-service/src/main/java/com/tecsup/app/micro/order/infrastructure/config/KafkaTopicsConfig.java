package com.tecsup.app.micro.order.infrastructure.config;

import com.tecsup.app.micro.order.infrastructure.messaging.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import com.tecsup.app.micro.shared.messaging.TopicsDeReintento;
import org.springframework.kafka.core.KafkaAdmin;
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

    private static final int PARTICIONES_REINTENTO = 3;
    private static final short REPLICAS_REINTENTO = 1;

    /**
     * Topics de reintento y DLT con el MISMO número de particiones que los de
     * negocio.
     *
     * Sin esto, @RetryableTopic los crea con una sola partición y la
     * publicación a la DLT falla para todo mensaje que venga de las
     * particiones 1 o 2. Al fallar, el offset no se confirma y el consumidor
     * relee el mismo registro en bucle. Ver la clase TopicsDeReintento.
     */
    @Bean
    public KafkaAdmin.NewTopics topicsDeReintento() {
        return TopicsDeReintento.para(PARTICIONES_REINTENTO, REPLICAS_REINTENTO,
                Topics.SUFIJO_DLT,
                Topics.PAGO_CONFIRMADO,
                Topics.PAGO_RECHAZADO,
                Topics.ENTREGA_ESTADO_CAMBIADO);
    }
}
