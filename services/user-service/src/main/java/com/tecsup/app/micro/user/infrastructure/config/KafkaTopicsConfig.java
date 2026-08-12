package com.tecsup.app.micro.user.infrastructure.config;

import com.tecsup.app.micro.user.infrastructure.messaging.Topics;
import com.tecsup.app.micro.shared.messaging.TopicsDeReintento;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Este servicio NO publica eventos, pero sí necesita declarar los topics que
 * @RetryableTopic genera para sus consumidores, con el número correcto de
 * particiones.
 */
@Configuration
public class KafkaTopicsConfig {

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
                Topics.PEDIDO_ENTREGADO);
    }
}
