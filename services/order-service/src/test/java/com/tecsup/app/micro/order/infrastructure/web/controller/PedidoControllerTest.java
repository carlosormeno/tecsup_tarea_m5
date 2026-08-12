package com.tecsup.app.micro.order.infrastructure.web.controller;

import com.tecsup.app.micro.order.domain.exception.CatalogoNoDisponibleException;
import com.tecsup.app.micro.order.domain.exception.PedidoNoEncontradoException;
import com.tecsup.app.micro.order.domain.exception.ProductoNoDisponibleException;
import com.tecsup.app.micro.order.domain.model.EstadoPedido;
import com.tecsup.app.micro.order.domain.model.LineaPedido;
import com.tecsup.app.micro.order.domain.model.Pedido;
import com.tecsup.app.micro.order.application.CancelarPedidoUseCase;
import com.tecsup.app.micro.order.application.ConsultarPedidosUseCase;
import com.tecsup.app.micro.order.application.CrearPedidoUseCase;
import com.tecsup.app.micro.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba del adaptador REST.
 *
 * Verifica la traducción HTTP <-> puertos de entrada: serialización del JSON,
 * validación de la petición y, sobre todo, que cada excepción de dominio
 * termine en el código de estado correcto. Los casos de uso van simulados
 * porque aquí no se prueba la lógica, solo la capa de entrada.
 *
 * addFilters = false desactiva la cadena de seguridad: estas pruebas son sobre
 * el contrato HTTP, no sobre quién puede llamarlo. Las reglas de acceso se
 * prueban aparte, en SecurityConfigTest.
 */
@WebMvcTest(controllers = PedidoController.class)
@AutoConfigureMockMvc(addFilters = false)
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrearPedidoUseCase crearPedido;

    @MockitoBean
    private ConsultarPedidosUseCase consultarPedidos;

    @MockitoBean
    private CancelarPedidoUseCase cancelarPedido;

    /**
     * JwtAuthenticationFilter sí entra en el slice de @WebMvcTest (los Filter
     * están en su lista de inclusión) y necesita este bean para construirse,
     * aunque con addFilters = false el filtro nunca llegue a ejecutarse.
     */
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private static final String PETICION_VALIDA = """
            {
              "clienteId": 1,
              "direccionEntrega": "Av. Arequipa 123",
              "items": [
                { "productoId": 10, "cantidad": 2 }
              ]
            }
            """;

    private Pedido unPedido() {
        return Pedido.crear(1L, "Av. Arequipa 123", List.of(
                new LineaPedido(10L, "Pizza margarita", new BigDecimal("35.90"), 2)));
    }

    @Test
    @DisplayName("POST /api/pedidos devuelve 201 con el pedido creado")
    void crear() throws Exception {
        given(crearPedido.crear(any())).willReturn(unPedido());

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PETICION_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CREADO"))
                .andExpect(jsonPath("$.clienteId").value(1))
                .andExpect(jsonPath("$.total").value(71.80))
                .andExpect(jsonPath("$.lineas[0].nombreProducto").value("Pizza margarita"))
                .andExpect(jsonPath("$.lineas[0].subtotal").value(71.80));
    }

    @Test
    @DisplayName("un pedido sin items devuelve 400")
    void validaItems() throws Exception {
        String sinItems = """
                {
                  "clienteId": 1,
                  "direccionEntrega": "Av. Arequipa 123",
                  "items": []
                }
                """;

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sinItems))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Petición inválida"));
    }

    @Test
    @DisplayName("una cantidad de cero devuelve 400")
    void validaCantidad() throws Exception {
        String cantidadCero = """
                {
                  "clienteId": 1,
                  "direccionEntrega": "Av. Arequipa 123",
                  "items": [ { "productoId": 10, "cantidad": 0 } ]
                }
                """;

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cantidadCero))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("un producto no disponible devuelve 422")
    void productoNoDisponible() throws Exception {
        willThrow(new ProductoNoDisponibleException(10L))
                .given(crearPedido).crear(any());

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PETICION_VALIDA))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Producto no disponible"));
    }

    @Test
    @DisplayName("si el catálogo está caído devuelve 503, porque reintentar sí tiene sentido")
    void catalogoCaido() throws Exception {
        willThrow(new CatalogoNoDisponibleException("timeout", new RuntimeException()))
                .given(crearPedido).crear(any());

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PETICION_VALIDA))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("GET /api/pedidos/{id} devuelve 200")
    void porId() throws Exception {
        Pedido pedido = unPedido();
        given(consultarPedidos.porId(pedido.getId())).willReturn(pedido);

        mockMvc.perform(get("/api/pedidos/{id}", pedido.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pedido.getId().toString()));
    }

    @Test
    @DisplayName("un pedido inexistente devuelve 404")
    void noEncontrado() throws Exception {
        UUID id = UUID.randomUUID();
        given(consultarPedidos.porId(id)).willThrow(new PedidoNoEncontradoException(id));

        mockMvc.perform(get("/api/pedidos/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Pedido no encontrado"));
    }

    @Test
    @DisplayName("GET /api/pedidos?clienteId= filtra por cliente")
    void listarPorCliente() throws Exception {
        given(consultarPedidos.porCliente(1L)).willReturn(List.of(unPedido()));

        mockMvc.perform(get("/api/pedidos").param("clienteId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clienteId").value(1));
    }

    @Test
    @DisplayName("POST /api/pedidos/{id}/cancelar devuelve el pedido cancelado")
    void cancelar() throws Exception {
        Pedido pedido = unPedido();
        pedido.transicionarA(EstadoPedido.CANCELADO, "me arrepentí");
        given(cancelarPedido.cancelar(any(UUID.class), anyString())).willReturn(pedido);

        mockMvc.perform(post("/api/pedidos/{id}/cancelar", pedido.getId())
                        .param("motivo", "me arrepentí"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.motivo").value("me arrepentí"));
    }
}
