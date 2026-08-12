package com.tecsup.app.micro.user.infrastructure.web.controller;

import com.tecsup.app.micro.user.application.ConsultarUsuariosUseCase;
import com.tecsup.app.micro.user.infrastructure.web.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consultas de usuario. A diferencia de /auth, estas SÍ exigen token. */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Perfil y puntos de fidelidad")
public class UsuarioController {

    private final ConsultarUsuariosUseCase consultarUsuarios;

    @GetMapping("/{id}")
    @Operation(summary = "Consulta un usuario y sus puntos de fidelidad")
    public ResponseEntity<UsuarioResponse> porId(@PathVariable Long id) {
        return ResponseEntity.ok(UsuarioResponse.de(consultarUsuarios.porId(id)));
    }

    @GetMapping
    @Operation(summary = "Lista los usuarios")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(
                consultarUsuarios.todos().stream().map(UsuarioResponse::de).toList());
    }
}
