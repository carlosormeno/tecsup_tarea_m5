package com.tecsup.app.micro.order.domain.exception;

/**
 * El catálogo respondió, pero el producto no existe o no está disponible.
 *
 * Determinista: no reintentable. Se distingue a propósito de
 * {@link CatalogoNoDisponibleException}, que sí es transitoria.
 */
public class ProductoNoDisponibleException extends RuntimeException {

    public ProductoNoDisponibleException(Long productoId) {
        super("El producto %d no existe o no está disponible".formatted(productoId));
    }
}
