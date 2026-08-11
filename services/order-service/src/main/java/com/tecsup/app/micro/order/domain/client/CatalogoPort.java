package com.tecsup.app.micro.order.domain.client;

import com.tecsup.app.micro.order.domain.model.ProductoCatalogo;

/**
 * Puerto de salida hacia el servicio de Catálogo.
 *
 * Es la ÚNICA dependencia síncrona de todo el sistema. Se justifica porque al
 * crear un pedido hay que validar contra la fuente de verdad que el producto
 * existe, está disponible y cuesta lo que el cliente cree que cuesta. Todo lo
 * demás entre servicios va por eventos.
 */
public interface CatalogoPort {

    /**
     * @throws com.tecsup.app.micro.order.domain.exception.ProductoNoDisponibleException
     *         si el producto no existe o está marcado como no disponible
     * @throws com.tecsup.app.micro.order.domain.exception.CatalogoNoDisponibleException
     *         si el servicio de Catálogo no responde
     */
    ProductoCatalogo obtenerProducto(Long productoId);
}
