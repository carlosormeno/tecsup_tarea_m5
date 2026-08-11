package com.tecsup.app.micro.order.application;

import com.tecsup.app.micro.order.domain.client.CatalogoPort;
import com.tecsup.app.micro.order.domain.repository.PedidoRepository;
import com.tecsup.app.micro.order.domain.event.PublicadorEventos;
import com.tecsup.app.micro.order.domain.event.PedidoCreado;
import com.tecsup.app.micro.order.domain.model.LineaPedido;
import com.tecsup.app.micro.order.domain.model.Pedido;
import com.tecsup.app.micro.order.domain.model.ProductoCatalogo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso: crear un pedido y arrancar la saga.
 *
 * NOTA SOBRE CONSISTENCIA (ver ADR-007): guardar en Postgres y publicar en
 * Kafka son dos escrituras sin transacción común. Si la BD confirma y el
 * broker falla, el pedido queda en un estado que nadie más conoce y la saga
 * se detiene. La solución formal es el patrón outbox; aquí se asume el riesgo
 * de forma consciente y se documenta.
 */
@Slf4j
@RequiredArgsConstructor
public class CrearPedidoUseCaseImpl implements CrearPedidoUseCase {

    private final PedidoRepository repositorio;
    private final PublicadorEventos publicador;
    private final CatalogoPort catalogo;

    @Override
    @Transactional
    public Pedido crear(ComandoCrearPedido comando) {
        List<LineaPedido> lineas = comando.items().stream()
                .map(this::resolverLinea)
                .toList();

        Pedido pedido = repositorio.guardar(
                Pedido.crear(comando.clienteId(), comando.direccionEntrega(), lineas));

        log.info("Pedido {} creado por el cliente {} por un total de {}",
                pedido.getId(), pedido.getClienteId(), pedido.total());

        publicador.publicar(PedidoCreado.de(pedido));
        return pedido;
    }

    /**
     * Consulta el catálogo y congela nombre y precio en la línea.
     *
     * Esta es la única llamada síncrona a otro servicio en todo el sistema, y
     * se justifica porque el precio no puede venir del cliente: si viniera,
     * cualquiera podría pedir lo que quisiera al precio que quisiera.
     */
    private LineaPedido resolverLinea(ComandoCrearPedido.ItemSolicitado item) {
        ProductoCatalogo producto = catalogo.obtenerProducto(item.productoId());
        return new LineaPedido(producto.id(), producto.nombre(), producto.precio(), item.cantidad());
    }
}
