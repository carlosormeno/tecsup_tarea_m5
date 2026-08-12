package com.tecsup.app.micro.user.infrastructure.web.controller;

import com.tecsup.app.micro.user.application.AutenticarUsuarioUseCase;
import com.tecsup.app.micro.user.application.RegistrarUsuarioUseCase;
import com.tecsup.app.micro.user.infrastructure.web.dto.LoginRequest;
import com.tecsup.app.micro.user.infrastructure.web.dto.LoginResponse;
import com.tecsup.app.micro.user.infrastructure.web.dto.RegistroRequest;
import com.tecsup.app.micro.user.infrastructure.web.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Los DOS únicos endpoints de negocio sin JWT de todo el sistema.
 *
 * Y no por comodidad: exigir un token para obtenerlo sería imposible. Están
 * declarados en `seguridad.rutas-publicas` de este servicio, que es el único
 * que abre `/auth/**`.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Registro y login. Aquí nace el token")
public class AuthController {

    private final RegistrarUsuarioUseCase registrarUsuario;
    private final AutenticarUsuarioUseCase autenticarUsuario;

    @PostMapping("/registro")
    @Operation(summary = "Registra un usuario nuevo con rol CLIENTE")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest peticion) {
        // El rol no se acepta desde fuera: si viniera en la petición,
        // cualquiera podría darse de alta como ADMIN.
        var comando = new RegistrarUsuarioUseCase.ComandoRegistro(
                peticion.nombre(), peticion.email(), peticion.password(),
                peticion.direccion(), Set.of());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UsuarioResponse.de(registrarUsuario.registrar(comando)));
    }

    @PostMapping("/login")
    @Operation(summary = "Devuelve el JWT que aceptan los cinco servicios")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest peticion) {
        return ResponseEntity.ok(LoginResponse.de(
                autenticarUsuario.autenticar(peticion.email(), peticion.password())));
    }
}
