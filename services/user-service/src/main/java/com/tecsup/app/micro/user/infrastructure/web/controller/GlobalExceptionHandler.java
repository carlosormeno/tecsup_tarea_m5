package com.tecsup.app.micro.user.infrastructure.web.controller;

import com.tecsup.app.micro.user.domain.exception.CredencialesInvalidasException;
import com.tecsup.app.micro.user.domain.exception.EmailYaRegistradoException;
import com.tecsup.app.micro.user.domain.exception.UsuarioNoEncontradoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ProblemDetail noEncontrado(UsuarioNoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Usuario no encontrado", e.getMessage());
    }

    @ExceptionHandler(EmailYaRegistradoException.class)
    public ProblemDetail emailDuplicado(EmailYaRegistradoException e) {
        return problema(HttpStatus.CONFLICT, "Email ya registrado", e.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ProblemDetail credencialesInvalidas(CredencialesInvalidasException e) {
        // 401 con un mensaje genérico: no se revela si falló el email o la
        // contraseña, para no confirmar qué correos están registrados.
        return problema(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return problema(HttpStatus.BAD_REQUEST, "Petición inválida", detalle);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException e) {
        return problema(HttpStatus.BAD_REQUEST, "Petición inválida", e.getMessage());
    }

    private ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(estado, detalle);
        pd.setTitle(titulo);
        return pd;
    }
}
