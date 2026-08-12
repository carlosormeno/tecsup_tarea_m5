package com.tecsup.app.micro.catalog.infrastructure.web.controller;

import com.tecsup.app.micro.catalog.domain.exception.ProductoNoEncontradoException;
import com.tecsup.app.micro.catalog.domain.exception.RestauranteNoEncontradoException;
import com.tecsup.app.micro.catalog.domain.exception.StockInsuficienteException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de dominio a códigos HTTP.
 *
 * El 404 de producto es especialmente relevante: order-service lo traduce a
 * `ProductoNoDisponibleException`, que es un fallo determinista y por tanto no
 * reintentable. La clasificación de errores empieza aquí.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductoNoEncontradoException.class)
    public ProblemDetail productoNoEncontrado(ProductoNoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Producto no encontrado", e.getMessage());
    }

    @ExceptionHandler(RestauranteNoEncontradoException.class)
    public ProblemDetail restauranteNoEncontrado(RestauranteNoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Restaurante no encontrado", e.getMessage());
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ProblemDetail stockInsuficiente(StockInsuficienteException e) {
        return problema(HttpStatus.CONFLICT, "Stock insuficiente", e.getMessage());
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
