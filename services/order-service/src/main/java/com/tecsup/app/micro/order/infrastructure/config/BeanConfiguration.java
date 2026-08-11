package com.tecsup.app.micro.order.infrastructure.config;

import com.tecsup.app.micro.order.application.AvanzarSagaUseCase;
import com.tecsup.app.micro.order.application.CancelarPedidoUseCase;
import com.tecsup.app.micro.order.application.ConsultarPedidosUseCase;
import com.tecsup.app.micro.order.application.CrearPedidoUseCase;
import com.tecsup.app.micro.order.domain.client.CatalogoPort;
import com.tecsup.app.micro.order.domain.repository.PedidoRepository;
import com.tecsup.app.micro.order.domain.event.PublicadorEventos;
import com.tecsup.app.micro.order.application.AvanzarSagaUseCaseImpl;
import com.tecsup.app.micro.order.application.CancelarPedidoUseCaseImpl;
import com.tecsup.app.micro.order.application.ConsultarPedidosUseCaseImpl;
import com.tecsup.app.micro.order.application.CrearPedidoUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public CrearPedidoUseCase crearPedidoUseCase(PedidoRepository repositorio,
                                                 PublicadorEventos publicador,
                                                 CatalogoPort catalogo) {
        return new CrearPedidoUseCaseImpl(repositorio, publicador, catalogo);
    }

    @Bean
    public ConsultarPedidosUseCase consultarPedidosUseCase(PedidoRepository repositorio) {
        return new ConsultarPedidosUseCaseImpl(repositorio);
    }

    @Bean
    public CancelarPedidoUseCase cancelarPedidoUseCase(PedidoRepository repositorio,
                                                       PublicadorEventos publicador) {
        return new CancelarPedidoUseCaseImpl(repositorio, publicador);
    }

    @Bean
    public AvanzarSagaUseCase avanzarSagaUseCase(PedidoRepository repositorio,
                                                 PublicadorEventos publicador) {
        return new AvanzarSagaUseCaseImpl(repositorio, publicador);
    }
}
