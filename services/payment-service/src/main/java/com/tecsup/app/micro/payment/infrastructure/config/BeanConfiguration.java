package com.tecsup.app.micro.payment.infrastructure.config;

import com.tecsup.app.micro.payment.application.ConsultarPagosUseCase;
import com.tecsup.app.micro.payment.application.ConsultarPagosUseCaseImpl;
import com.tecsup.app.micro.payment.application.ProcesarPagoUseCase;
import com.tecsup.app.micro.payment.application.ProcesarPagoUseCaseImpl;
import com.tecsup.app.micro.payment.application.ReembolsarPagoUseCase;
import com.tecsup.app.micro.payment.application.ReembolsarPagoUseCaseImpl;
import com.tecsup.app.micro.payment.domain.event.PublicadorEventos;
import com.tecsup.app.micro.payment.domain.repository.PagoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Cableado del hexágono.
 *
 * Registra cada implementación detrás de su interfaz: el controlador y los
 * listeners dependen del puerto, nunca de la clase concreta. Los casos de uso
 * no llevan @Component, así que la capa de aplicación no depende del
 * contenedor de Spring salvo por @Transactional.
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public ProcesarPagoUseCase procesarPagoUseCase(
            PagoRepository repositorio,
            PublicadorEventos publicador,
            // El límite es configuración; la regla que lo aplica vive en el dominio
            @Value("${pagos.limite-autorizado:500.00}") BigDecimal limiteAutorizado) {

        return new ProcesarPagoUseCaseImpl(repositorio, publicador, limiteAutorizado);
    }

    @Bean
    public ReembolsarPagoUseCase reembolsarPagoUseCase(PagoRepository repositorio) {
        return new ReembolsarPagoUseCaseImpl(repositorio);
    }

    @Bean
    public ConsultarPagosUseCase consultarPagosUseCase(PagoRepository repositorio) {
        return new ConsultarPagosUseCaseImpl(repositorio);
    }
}
