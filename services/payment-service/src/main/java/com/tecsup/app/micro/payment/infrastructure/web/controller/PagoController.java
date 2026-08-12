package com.tecsup.app.micro.payment.infrastructure.web.controller;

import com.tecsup.app.micro.payment.application.ConsultarPagosUseCase;
import com.tecsup.app.micro.payment.application.ReembolsarPagoUseCase;
import com.tecsup.app.micro.payment.infrastructure.web.dto.PagoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador de entrada REST.
 *
 * No expone ningún endpoint para cobrar: el cobro lo dispara el evento
 * `pedido.creado`, nunca una llamada HTTP. Aquí solo hay consultas y el
 * reembolso manual, que es una operación administrativa.
 */
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@Tag(name = "Pagos", description = "Consulta de pagos y reembolsos")
public class PagoController {

    private final ConsultarPagosUseCase consultarPagos;
    private final ReembolsarPagoUseCase reembolsarPago;

    @GetMapping
    @Operation(summary = "Lista todos los pagos")
    public ResponseEntity<List<PagoResponse>> listar() {
        return ResponseEntity.ok(consultarPagos.todos().stream().map(PagoResponse::de).toList());
    }

    @GetMapping("/pedido/{pedidoId}")
    @Operation(summary = "Consulta el pago de un pedido")
    public ResponseEntity<PagoResponse> porPedido(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(PagoResponse.de(consultarPagos.porPedido(pedidoId)));
    }

    @PostMapping("/pedido/{pedidoId}/reembolso")
    @Operation(summary = "Reembolsa manualmente el pago de un pedido")
    public ResponseEntity<PagoResponse> reembolsar(
            @PathVariable UUID pedidoId,
            @RequestParam(defaultValue = "Reembolso administrativo") String motivo) {

        reembolsarPago.reembolsarPorPedido(pedidoId, motivo);
        return ResponseEntity.ok(PagoResponse.de(consultarPagos.porPedido(pedidoId)));
    }
}
