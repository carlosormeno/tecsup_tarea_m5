package com.tecsup.app.micro.user.application;

import com.tecsup.app.micro.user.domain.model.Usuario;
import com.tecsup.app.micro.user.domain.repository.PedidosPuntuados;
import com.tecsup.app.micro.user.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Caso de uso: sumar puntos de fidelidad al entregarse un pedido.
 *
 * Idempotencia con tabla, como en Catálogo y por el mismo motivo: sumar dos
 * veces deja el saldo mal sin dejar rastro. El registro del pedido y la suma
 * van en la MISMA transacción, y en ese orden importa poco cuál va primero
 * mientras compartan transacción — lo que no puede pasar es que se confirme
 * una sin la otra.
 */
@Slf4j
@RequiredArgsConstructor
public class SumarPuntosUseCaseImpl implements SumarPuntosUseCase {

    private final UsuarioRepository usuarios;
    private final PedidosPuntuados pedidosPuntuados;

    @Override
    @Transactional
    public void porPedidoEntregado(UUID pedidoId, Long clienteId, BigDecimal total) {
        if (pedidosPuntuados.yaPuntuado(pedidoId)) {
            log.info("El pedido {} ya había sumado puntos; se ignora el evento duplicado", pedidoId);
            return;
        }

        Usuario usuario = usuarios.obtener(clienteId);

        int antes = usuario.getPuntosFidelidad();
        usuario.sumarPuntosPor(total);
        int ganados = usuario.getPuntosFidelidad() - antes;

        usuarios.guardar(usuario);
        pedidosPuntuados.registrar(pedidoId, clienteId, ganados);

        log.info("Usuario {} suma {} puntos por el pedido {}; total {}",
                clienteId, ganados, pedidoId, usuario.getPuntosFidelidad());
    }
}
