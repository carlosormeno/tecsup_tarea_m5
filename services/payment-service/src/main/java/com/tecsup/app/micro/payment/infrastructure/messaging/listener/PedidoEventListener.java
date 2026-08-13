package com.tecsup.app.micro.payment.infrastructure.messaging.listener;

import com.tecsup.app.micro.payment.application.ProcesarPagoUseCase;
import com.tecsup.app.micro.payment.application.ReembolsarPagoUseCase;
import com.tecsup.app.micro.payment.domain.exception.PagoNoEncontradoException;
import com.tecsup.app.micro.payment.domain.exception.TransicionInvalidaException;
import com.tecsup.app.micro.shared.dlq.DeadLetterQueue;
import com.tecsup.app.micro.payment.infrastructure.messaging.Topics;
import com.tecsup.app.micro.payment.infrastructure.messaging.dto.PagoSolicitadoDTO;
import com.tecsup.app.micro.payment.infrastructure.messaging.dto.PedidoCanceladoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada: eventos que publica Pedidos.
 *
 * Solo traduce del mundo de Kafka a los puertos de entrada. Ni una regla de
 * negocio vive aquí.
 *
 * Política de reintentos: 4 entregas en total (la original más 3), con backoff
 * exponencial de 2s. En `exclude` van los fallos deterministas, que no ganan
 * nada reintentándose y pasan directo a la DLT.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PedidoEventListener {

    private final ProcesarPagoUseCase procesarPago;
    private final ReembolsarPagoUseCase reembolsarPago;
    private final DeadLetterQueue dlq;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            // false: los topics de reintento y DLT los declara KafkaTopicsConfig
            // con 3 particiones. Si los creara la anotación, los haría con una
            // sola y la publicación a la DLT fallaría desde las particiones 1 y 2.
            autoCreateTopics = "false",
            dltTopicSuffix = Topics.SUFIJO_DLT,
            exclude = {
                    PagoNoEncontradoException.class,
                    TransicionInvalidaException.class,
                    IllegalArgumentException.class
            })
    @KafkaListener(topics = Topics.PEDIDO_PAGO_SOLICITADO, groupId = "pagos-group")
    public void alSolicitarsePago(PagoSolicitadoDTO evento) {
        log.info("Recibido pedido.pago-solicitado para el pedido {} por {}",
                evento.pedidoId(), evento.total());

        procesarPago.procesar(evento.pedidoId(), evento.clienteId(), evento.total());
    }

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            // false: los topics de reintento y DLT los declara KafkaTopicsConfig
            // con 3 particiones. Si los creara la anotación, los haría con una
            // sola y la publicación a la DLT fallaría desde las particiones 1 y 2.
            autoCreateTopics = "false",
            dltTopicSuffix = Topics.SUFIJO_DLT,
            exclude = {
                    PagoNoEncontradoException.class,
                    TransicionInvalidaException.class,
                    IllegalArgumentException.class
            })
    @KafkaListener(topics = Topics.PEDIDO_CANCELADO, groupId = "pagos-group")
    public void alCancelarsePedido(PedidoCanceladoDTO evento) {
        log.info("Recibido pedido.cancelado para el pedido {} (huboCobro={})",
                evento.pedidoId(), evento.huboCobro());

        // No se filtra por huboCobro: el caso de uso ya tolera que no exista
        // pago o que esté rechazado. Decidirlo aquí sería meter regla de
        // negocio en el adaptador.
        reembolsarPago.reembolsarPorPedido(evento.pedidoId(), evento.motivo());
    }

    /** Último recurso: se agotaron los reintentos. */
    @DltHandler
    public void alAgotarseLosReintentos(ConsumerRecord<?, ?> registro) {
        dlq.registrar(registro);
    }
}
