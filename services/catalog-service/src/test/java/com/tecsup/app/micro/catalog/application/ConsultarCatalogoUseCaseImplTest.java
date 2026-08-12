package com.tecsup.app.micro.catalog.application;

import com.tecsup.app.micro.catalog.domain.exception.ProductoNoEncontradoException;
import com.tecsup.app.micro.catalog.domain.exception.RestauranteNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarCatalogoUseCaseImplTest {

    private ConsultarCatalogoUseCaseImpl consultar;

    @BeforeEach
    void preparar() {
        consultar = new ConsultarCatalogoUseCaseImpl(
                new Fakes.FakeProductos().conProducto(10L, 100).conProducto(20L, 0),
                new Fakes.FakeRestaurantes().conRestaurante(1L));
    }

    @Test
    @DisplayName("consulta un producto por id")
    void productoPorId() {
        assertThat(consultar.productoPorId(10L).getNombre()).isEqualTo("Producto 10");
        assertThat(consultar.productoPorId(10L).estaDisponible()).isTrue();
        // Sin stock deja de estar disponible, aunque el producto exista
        assertThat(consultar.productoPorId(20L).estaDisponible()).isFalse();
    }

    @Test
    @DisplayName("un producto inexistente falla, no devuelve vacío")
    void productoInexistente() {
        assertThatThrownBy(() -> consultar.productoPorId(999L))
                .isInstanceOf(ProductoNoEncontradoException.class);
    }

    @Test
    @DisplayName("el menú de un restaurante inexistente da error, no lista vacía")
    void restauranteInexistente() {
        // Devolver [] haría creer al cliente que el restaurante existe pero no
        // tiene productos, que es una respuesta engañosa.
        assertThatThrownBy(() -> consultar.productosDeRestaurante(999L))
                .isInstanceOf(RestauranteNoEncontradoException.class);
    }

    @Test
    @DisplayName("lista el menú de un restaurante existente")
    void menuDeRestaurante() {
        assertThat(consultar.productosDeRestaurante(1L)).hasSize(2);
        assertThat(consultar.restaurantes()).hasSize(1);
    }
}
