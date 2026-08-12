package com.tecsup.app.micro.delivery.infrastructure.config;

import com.tecsup.app.micro.delivery.application.ActualizarEntregaUseCase;
import com.tecsup.app.micro.delivery.application.ActualizarEntregaUseCaseImpl;
import com.tecsup.app.micro.delivery.application.AsignarEntregaUseCase;
import com.tecsup.app.micro.delivery.application.AsignarEntregaUseCaseImpl;
import com.tecsup.app.micro.delivery.application.ConsultarEntregasUseCase;
import com.tecsup.app.micro.delivery.application.ConsultarEntregasUseCaseImpl;
import com.tecsup.app.micro.delivery.domain.event.PublicadorEventos;
import com.tecsup.app.micro.delivery.domain.repository.EntregaRepository;
import com.tecsup.app.micro.delivery.domain.repository.RepartidorRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cableado del hexágono. */
@Configuration
public class BeanConfiguration {

    @Bean
    public AsignarEntregaUseCase asignarEntregaUseCase(EntregaRepository entregas,
                                                       RepartidorRepository repartidores,
                                                       PublicadorEventos publicador) {
        return new AsignarEntregaUseCaseImpl(entregas, repartidores, publicador);
    }

    @Bean
    public ActualizarEntregaUseCase actualizarEntregaUseCase(EntregaRepository entregas,
                                                             PublicadorEventos publicador) {
        return new ActualizarEntregaUseCaseImpl(entregas, publicador);
    }

    @Bean
    public ConsultarEntregasUseCase consultarEntregasUseCase(EntregaRepository entregas) {
        return new ConsultarEntregasUseCaseImpl(entregas);
    }
}
