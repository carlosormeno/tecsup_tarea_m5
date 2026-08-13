package com.tecsup.app.micro.order.infrastructure.web.controller;

import com.tecsup.app.micro.order.domain.model.Pedido;
import com.tecsup.app.micro.order.application.CancelarPedidoUseCase;
import com.tecsup.app.micro.order.application.ConsultarPedidosUseCase;
import com.tecsup.app.micro.order.application.CrearPedidoUseCase;
import com.tecsup.app.micro.order.application.PagarPedidoUseCase;
import com.tecsup.app.micro.order.infrastructure.web.dto.CrearPedidoRequest;
import com.tecsup.app.micro.order.infrastructure.web.dto.PedidoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Ciclo de vida de los pedidos")
public class PedidoController {

    private final CrearPedidoUseCase crearPedido;
    private final PagarPedidoUseCase pagarPedido;
    private final ConsultarPedidosUseCase consultarPedidos;
    private final CancelarPedidoUseCase cancelarPedido;

    @PostMapping
    @Operation(summary = "Crea un pedido. Queda en CREADO, a la espera del pago")
    public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody CrearPedidoRequest peticion) {
        List<CrearPedidoUseCase.ComandoCrearPedido.ItemSolicitado> items = peticion.items().stream()
                .map(i -> new CrearPedidoUseCase.ComandoCrearPedido.ItemSolicitado(
                        i.productoId(), i.cantidad()))
                .toList();

        Pedido pedido = crearPedido.crear(new CrearPedidoUseCase.ComandoCrearPedido(
                peticion.clienteId(), peticion.direccionEntrega(), items));

        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.de(pedido));
    }

    /**
     * Arranca la saga.
     *
     * Devuelve 200 con el pedido en `PAGO_EN_PROCESO`, no el resultado del
     * cobro: quien decide si el pago sale bien es Pagos, y responde por evento
     * unos milisegundos después. El cliente ve el resultado consultando el
     * pedido.
     *
     * Un segundo intento sobre el mismo pedido devuelve 409: la transición ya
     * no es válida.
     */
    @PostMapping("/{id}/pagar")
    @Operation(summary = "Solicita el cobro del pedido y arranca la saga")
    public ResponseEntity<PedidoResponse> pagar(@PathVariable UUID id) {
        return ResponseEntity.ok(PedidoResponse.de(pagarPedido.pagar(id)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta un pedido por su identificador")
    public ResponseEntity<PedidoResponse> porId(@PathVariable UUID id) {
        return ResponseEntity.ok(PedidoResponse.de(consultarPedidos.porId(id)));
    }

    @GetMapping
    @Operation(summary = "Lista pedidos, opcionalmente filtrados por cliente")
    public ResponseEntity<List<PedidoResponse>> listar(
            @RequestParam(required = false) Long clienteId) {

        List<Pedido> pedidos = (clienteId == null)
                ? consultarPedidos.todos()
                : consultarPedidos.porCliente(clienteId);

        return ResponseEntity.ok(pedidos.stream().map(PedidoResponse::de).toList());
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancela un pedido y dispara la compensación de la saga")
    public ResponseEntity<PedidoResponse> cancelar(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "Cancelado por el cliente") String motivo) {

        return ResponseEntity.ok(PedidoResponse.de(cancelarPedido.cancelar(id, motivo)));
    }
}
