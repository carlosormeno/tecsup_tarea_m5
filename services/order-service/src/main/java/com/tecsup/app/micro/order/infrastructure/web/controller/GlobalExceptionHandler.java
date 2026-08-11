package com.tecsup.app.micro.order.infrastructure.web.controller;

import com.tecsup.app.micro.order.domain.exception.CatalogoNoDisponibleException;
import com.tecsup.app.micro.order.domain.exception.PedidoNoEncontradoException;
import com.tecsup.app.micro.order.domain.exception.ProductoNoDisponibleException;
import com.tecsup.app.micro.order.domain.exception.TransicionInvalidaException;
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

    @ExceptionHandler(PedidoNoEncontradoException.class)
    public ProblemDetail noEncontrado(PedidoNoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Pedido no encontrado", e.getMessage());
    }

    @ExceptionHandler(ProductoNoDisponibleException.class)
    public ProblemDetail productoNoDisponible(ProductoNoDisponibleException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Producto no disponible", e.getMessage());
    }

    @ExceptionHandler(TransicionInvalidaException.class)
    public ProblemDetail transicionInvalida(TransicionInvalidaException e) {
        return problema(HttpStatus.CONFLICT, "Transición de estado no permitida", e.getMessage());
    }

    @ExceptionHandler(CatalogoNoDisponibleException.class)
    public ProblemDetail catalogoCaido(CatalogoNoDisponibleException e) {
        log.error("Catálogo no disponible", e);
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Catálogo no disponible",
                "No se pudo validar el pedido porque el catálogo no responde. Reintente en unos segundos.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException e) {
        return problema(HttpStatus.BAD_REQUEST, "Petición inválida", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return problema(HttpStatus.BAD_REQUEST, "Petición inválida", detalle);
    }

    private ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(estado, detalle);
        pd.setTitle(titulo);
        return pd;
    }
}
