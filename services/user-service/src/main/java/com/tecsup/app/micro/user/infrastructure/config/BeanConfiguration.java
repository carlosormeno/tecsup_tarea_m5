package com.tecsup.app.micro.user.infrastructure.config;

import com.tecsup.app.micro.user.application.AutenticarUsuarioUseCase;
import com.tecsup.app.micro.user.application.AutenticarUsuarioUseCaseImpl;
import com.tecsup.app.micro.user.application.ConsultarUsuariosUseCase;
import com.tecsup.app.micro.user.application.ConsultarUsuariosUseCaseImpl;
import com.tecsup.app.micro.user.application.RegistrarUsuarioUseCase;
import com.tecsup.app.micro.user.application.RegistrarUsuarioUseCaseImpl;
import com.tecsup.app.micro.user.application.SumarPuntosUseCase;
import com.tecsup.app.micro.user.application.SumarPuntosUseCaseImpl;
import com.tecsup.app.micro.user.domain.repository.CifradorDeClaves;
import com.tecsup.app.micro.user.domain.repository.EmisorDeTokens;
import com.tecsup.app.micro.user.domain.repository.PedidosPuntuados;
import com.tecsup.app.micro.user.domain.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cableado del hexágono. */
@Configuration
public class BeanConfiguration {

    @Bean
    public RegistrarUsuarioUseCase registrarUsuarioUseCase(UsuarioRepository usuarios,
                                                           CifradorDeClaves cifrador) {
        return new RegistrarUsuarioUseCaseImpl(usuarios, cifrador);
    }

    @Bean
    public AutenticarUsuarioUseCase autenticarUsuarioUseCase(UsuarioRepository usuarios,
                                                             CifradorDeClaves cifrador,
                                                             EmisorDeTokens emisor) {
        return new AutenticarUsuarioUseCaseImpl(usuarios, cifrador, emisor);
    }

    @Bean
    public ConsultarUsuariosUseCase consultarUsuariosUseCase(UsuarioRepository usuarios) {
        return new ConsultarUsuariosUseCaseImpl(usuarios);
    }

    @Bean
    public SumarPuntosUseCase sumarPuntosUseCase(UsuarioRepository usuarios,
                                                 PedidosPuntuados pedidosPuntuados) {
        return new SumarPuntosUseCaseImpl(usuarios, pedidosPuntuados);
    }
}
