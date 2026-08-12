package com.tecsup.app.micro.payment.infrastructure.config;

import com.tecsup.app.micro.payment.infrastructure.messaging.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import com.tecsup.app.micro.shared.messaging.TopicsDeReintento;
import org.springframework.kafka.core.KafkaAdmin;
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
                Topics.PEDIDO_CREADO,
                Topics.PEDIDO_CANCELADO);
    }
}
