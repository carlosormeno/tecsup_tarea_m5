package com.tecsup.app.micro.order.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CrearPedidoRequest(

        @NotNull(message = "clienteId es obligatorio")
        Long clienteId,

        @NotBlank(message = "La dirección de entrega es obligatoria")
        String direccionEntrega,

        @NotEmpty(message = "El pedido debe tener al menos un item")
        @Valid
        List<ItemRequest> items
) {

    public record ItemRequest(

            @NotNull(message = "productoId es obligatorio")
            Long productoId,

            @Positive(message = "La cantidad debe ser mayor que cero")
            int cantidad
    ) {
    }
}
