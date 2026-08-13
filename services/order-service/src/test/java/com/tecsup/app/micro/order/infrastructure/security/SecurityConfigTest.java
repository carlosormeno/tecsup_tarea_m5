package com.tecsup.app.micro.order.infrastructure.security;

import com.tecsup.app.micro.shared.security.JwtAuthenticationFilter;
import com.tecsup.app.micro.shared.security.JwtTokenProvider;
import com.tecsup.app.micro.shared.security.SecurityConfig;

import com.tecsup.app.micro.order.application.CancelarPedidoUseCase;
import com.tecsup.app.micro.order.application.ConsultarPedidosUseCase;
import com.tecsup.app.micro.order.application.CrearPedidoUseCase;
import com.tecsup.app.micro.order.application.PagarPedidoUseCase;
import com.tecsup.app.micro.order.infrastructure.web.controller.PedidoController;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reglas de acceso.
 *
 * A diferencia de PedidoControllerTest, aquí la cadena de filtros SÍ está
 * activa: lo que se prueba es exactamente quién puede entrar y quién no.
 */
@WebMvcTest(controllers = PedidoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class SecurityConfigTest {

    /** La misma clave que src/test/resources/application.yaml. */
    private static final String SECRETO = "clave-de-pruebas-con-al-menos-32-caracteres-para-HS256";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrearPedidoUseCase crearPedido;

    @MockitoBean
    private PagarPedidoUseCase pagarPedido;

    @MockitoBean
    private ConsultarPedidosUseCase consultarPedidos;

    @MockitoBean
    private CancelarPedidoUseCase cancelarPedido;

    private String token(String secreto, long validoPorMs) {
        SecretKey clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        long ahora = System.currentTimeMillis();

        return Jwts.builder()
                .subject("1")
                .claim("roles", List.of("CLIENTE"))
                .issuedAt(new Date(ahora))
                .expiration(new Date(ahora + validoPorMs))
                .signWith(clave)
                .compact();
    }

    @Test
    @DisplayName("sin token, un endpoint de negocio devuelve 401")
    void sinToken() throws Exception {
        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("con un token válido se permite el acceso")
    void tokenValido() throws Exception {
        given(consultarPedidos.todos()).willReturn(List.of());

        mockMvc.perform(get("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(SECRETO, 3600_000)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("un token firmado con otra clave se rechaza")
    void firmaInvalida() throws Exception {
        String otraClave = "una-clave-distinta-igual-de-larga-para-HS256-xx";

        mockMvc.perform(get("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(otraClave, 3600_000)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un token caducado se rechaza")
    void tokenCaducado() throws Exception {
        mockMvc.perform(get("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(SECRETO, -1000)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("una cabecera con basura no revienta el filtro, solo deniega")
    void cabeceraBasura() throws Exception {
        mockMvc.perform(get("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer esto-no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }
}
