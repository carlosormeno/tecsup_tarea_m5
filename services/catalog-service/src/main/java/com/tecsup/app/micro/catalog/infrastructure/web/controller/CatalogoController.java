package com.tecsup.app.micro.catalog.infrastructure.web.controller;

import com.tecsup.app.micro.catalog.application.ConsultarCatalogoUseCase;
import com.tecsup.app.micro.catalog.infrastructure.web.dto.ProductoResponse;
import com.tecsup.app.micro.catalog.infrastructure.web.dto.RestauranteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada REST.
 *
 * `GET /api/productos/{id}` es el endpoint más importante del sistema aunque
 * no lo parezca: es la única llamada síncrona entre servicios, y order-service
 * la usa para validar precio y disponibilidad antes de crear cada pedido.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Catálogo", description = "Restaurantes y productos disponibles")
public class CatalogoController {

    private final ConsultarCatalogoUseCase consultarCatalogo;

    @GetMapping("/productos/{id}")
    @Operation(summary = "Consulta un producto. Lo llama order-service al crear un pedido")
    public ResponseEntity<ProductoResponse> productoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ProductoResponse.de(consultarCatalogo.productoPorId(id)));
    }

    @GetMapping("/productos")
    @Operation(summary = "Lista todos los productos del catálogo")
    public ResponseEntity<List<ProductoResponse>> productos() {
        return ResponseEntity.ok(
                consultarCatalogo.productos().stream().map(ProductoResponse::de).toList());
    }

    @GetMapping("/restaurantes")
    @Operation(summary = "Lista los restaurantes")
    public ResponseEntity<List<RestauranteResponse>> restaurantes() {
        return ResponseEntity.ok(
                consultarCatalogo.restaurantes().stream().map(RestauranteResponse::de).toList());
    }

    @GetMapping("/restaurantes/{id}/productos")
    @Operation(summary = "Lista el menú de un restaurante")
    public ResponseEntity<List<ProductoResponse>> menu(@PathVariable Long id) {
        return ResponseEntity.ok(
                consultarCatalogo.productosDeRestaurante(id).stream()
                        .map(ProductoResponse::de).toList());
    }
}
