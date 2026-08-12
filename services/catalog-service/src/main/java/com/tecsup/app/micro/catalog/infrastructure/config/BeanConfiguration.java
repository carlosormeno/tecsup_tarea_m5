package com.tecsup.app.micro.catalog.infrastructure.config;

import com.tecsup.app.micro.catalog.application.AjustarStockUseCase;
import com.tecsup.app.micro.catalog.application.AjustarStockUseCaseImpl;
import com.tecsup.app.micro.catalog.application.ConsultarCatalogoUseCase;
import com.tecsup.app.micro.catalog.application.ConsultarCatalogoUseCaseImpl;
import com.tecsup.app.micro.catalog.domain.repository.EventosProcesados;
import com.tecsup.app.micro.catalog.domain.repository.ProductoRepository;
import com.tecsup.app.micro.catalog.domain.repository.RestauranteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado del hexágono: cada implementación queda registrada detrás de su
 * interfaz, de modo que el controlador y el listener dependen del puerto.
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public ConsultarCatalogoUseCase consultarCatalogoUseCase(ProductoRepository productos,
                                                             RestauranteRepository restaurantes) {
        return new ConsultarCatalogoUseCaseImpl(productos, restaurantes);
    }

    @Bean
    public AjustarStockUseCase ajustarStockUseCase(ProductoRepository productos,
                                                   EventosProcesados eventosProcesados) {
        return new AjustarStockUseCaseImpl(productos, eventosProcesados);
    }
}
