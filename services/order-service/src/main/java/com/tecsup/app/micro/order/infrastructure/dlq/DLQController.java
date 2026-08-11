package com.tecsup.app.micro.order.infrastructure.dlq;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dlq")
@RequiredArgsConstructor
@Tag(name = "DLQ", description = "Eventos que agotaron sus reintentos")
public class DLQController {

    private final DeadLetterQueue dlq;

    @GetMapping
    @Operation(summary = "Lista los eventos que terminaron en la cola de fallidos")
    public ResponseEntity<List<FailedEvent>> listar() {
        return ResponseEntity.ok(dlq.listar());
    }
}
