package com.tecsup.app.micro.delivery.infrastructure.web.controller;

import com.tecsup.app.micro.delivery.application.ActualizarEntregaUseCase;
import com.tecsup.app.micro.delivery.application.ConsultarEntregasUseCase;
import com.tecsup.app.micro.delivery.domain.model.EstadoEntrega;
import com.tecsup.app.micro.delivery.infrastructure.web.dto.EntregaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador de entrada REST.
 *
 * No hay endpoint para CREAR entregas: una entrega solo nace de un
 * `pedido.confirmado`. El PATCH de estado sí es REST porque lo usa la
 * aplicación del repartidor, que es un actor humano y no un evento.
 */
@RestController
@RequestMapping("/api/entregas")
@RequiredArgsConstructor
@Tag(name = "Entregas", description = "Seguimiento y avance de las entregas")
public class EntregaController {

    private final ConsultarEntregasUseCase consultarEntregas;
    private final ActualizarEntregaUseCase actualizarEntrega;

    @GetMapping
    @Operation(summary = "Lista todas las entregas")
    public ResponseEntity<List<EntregaResponse>> listar() {
        return ResponseEntity.ok(
                consultarEntregas.todas().stream().map(EntregaResponse::de).toList());
    }

    @GetMapping("/pedido/{pedidoId}")
    @Operation(summary = "Consulta la entrega de un pedido. Lo usa el seguimiento del front")
    public ResponseEntity<EntregaResponse> porPedido(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(EntregaResponse.de(consultarEntregas.porPedido(pedidoId)));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "El repartidor reporta el avance. Cada cambio publica un evento")
    public ResponseEntity<EntregaResponse> cambiarEstado(
            @PathVariable UUID id,
            @RequestParam EstadoEntrega nuevoEstado,
            @RequestParam(required = false) String detalle) {

        return ResponseEntity.ok(
                EntregaResponse.de(actualizarEntrega.cambiarEstado(id, nuevoEstado, detalle)));
    }
}
