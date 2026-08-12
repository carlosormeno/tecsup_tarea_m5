package com.tecsup.app.micro.catalog.domain.model;

import com.tecsup.app.micro.catalog.domain.exception.StockInsuficienteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductoTest {

    private Producto conStock(int stock) {
        return Producto.reconstituir(1L, 1L, "Pizza margarita", "Muzzarella",
                new BigDecimal("35.90"), stock, true);
    }

    @Test
    @DisplayName("disponible exige estar activo y tener existencias")
    void disponibilidad() {
        assertThat(conStock(10).estaDisponible()).isTrue();
        assertThat(conStock(0).estaDisponible()).isFalse();

        Producto inactivo = Producto.reconstituir(1L, 1L, "Pizza", null,
                new BigDecimal("35.90"), 10, false);
        assertThat(inactivo.estaDisponible()).isFalse();
    }

    @Test
    @DisplayName("descontar reduce el stock")
    void descuenta() {
        Producto producto = conStock(10);
        producto.descontarStock(3);
        assertThat(producto.getStock()).isEqualTo(7);
    }

    @Test
    @DisplayName("no se puede descontar más de lo que hay")
    void noDescuentaDeMas() {
        Producto producto = conStock(2);

        assertThatThrownBy(() -> producto.descontarStock(5))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("tiene 2 unidades y se solicitaron 5");

        // El stock no cambió: la excepción se lanza antes de tocar nada
        assertThat(producto.getStock()).isEqualTo(2);
    }

    @Test
    @DisplayName("descontar hasta cero deja el producto no disponible")
    void agotarCambiaDisponibilidad() {
        Producto producto = conStock(3);
        producto.descontarStock(3);

        assertThat(producto.getStock()).isZero();
        assertThat(producto.estaDisponible()).isFalse();
    }

    @Test
    @DisplayName("reponer devuelve las unidades al inventario")
    void repone() {
        Producto producto = conStock(5);
        producto.descontarStock(5);
        producto.reponerStock(5);

        assertThat(producto.getStock()).isEqualTo(5);
        assertThat(producto.estaDisponible()).isTrue();
    }

    @Test
    @DisplayName("las cantidades deben ser positivas")
    void cantidadesPositivas() {
        Producto producto = conStock(10);

        assertThatThrownBy(() -> producto.descontarStock(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> producto.reponerStock(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
