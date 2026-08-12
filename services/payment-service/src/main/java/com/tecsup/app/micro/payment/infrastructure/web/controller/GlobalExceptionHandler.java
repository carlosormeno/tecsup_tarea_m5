package com.tecsup.app.micro.payment.infrastructure.web.controller;

import com.tecsup.app.micro.payment.domain.exception.PagoNoEncontradoException;
import com.tecsup.app.micro.payment.domain.exception.TransicionInvalidaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones de dominio a códigos HTTP. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PagoNoEncontradoException.class)
    public ProblemDetail noEncontrado(PagoNoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Pago no encontrado", e.getMessage());
    }

    @ExceptionHandler(TransicionInvalidaException.class)
    public ProblemDetail transicionInvalida(TransicionInvalidaException e) {
        // 409 y no 400: la petición es correcta, lo que no encaja es el estado
        // actual del recurso. Reintentarla daría lo mismo.
        return problema(HttpStatus.CONFLICT, "Operación no permitida en este estado", e.getMessage());
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
