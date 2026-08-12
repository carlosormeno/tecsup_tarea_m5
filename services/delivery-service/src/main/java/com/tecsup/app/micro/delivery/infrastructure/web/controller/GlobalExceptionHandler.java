package com.tecsup.app.micro.delivery.infrastructure.web.controller;

import com.tecsup.app.micro.delivery.domain.exception.EntregaNoEncontradaException;
import com.tecsup.app.micro.delivery.domain.exception.SinRepartidoresException;
import com.tecsup.app.micro.delivery.domain.exception.TransicionInvalidaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntregaNoEncontradaException.class)
    public ProblemDetail noEncontrada(EntregaNoEncontradaException e) {
        return problema(HttpStatus.NOT_FOUND, "Entrega no encontrada", e.getMessage());
    }

    @ExceptionHandler(TransicionInvalidaException.class)
    public ProblemDetail transicionInvalida(TransicionInvalidaException e) {
        return problema(HttpStatus.CONFLICT, "Transición de estado no permitida", e.getMessage());
    }

    @ExceptionHandler(SinRepartidoresException.class)
    public ProblemDetail sinRepartidores(SinRepartidoresException e) {
        // 503 y no 409: la situación es temporal y reintentar tiene sentido.
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Sin repartidores disponibles",
                e.getMessage());
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
