package com.tecsup.app.micro.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Reglas de acceso, comunes a todos los servicios.
 *
 * Ningún endpoint DE NEGOCIO es público: todo lo que no esté en la lista
 * blanca exige un JWT válido.
 *
 * La lista se configura por propiedad porque no es igual en todos: los cuatro
 * servicios de negocio solo abren actuator y Swagger, mientras que user-service
 * tiene que abrir además /auth/**, ya que son los endpoints que ENTREGAN el
 * token y no pueden exigirlo.
 *
 *   seguridad:
 *     rutas-publicas: /actuator/**, /swagger-ui/**, /v3/api-docs/**
 *
 * Mitigación de lo que queda abierto: ninguna de esas rutas sale de la red de
 * Podman. Actuator no puede exigir token porque Prometheus raspa cada 15 s y
 * el JWT caduca en una hora.
 *
 * Sesiones STATELESS: el token ya lleva la identidad, y guardar sesión en
 * servidor rompería el escalado horizontal.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final String[] rutasPublicas;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          @Value("${seguridad.rutas-publicas}") String[] rutasPublicas) {
        this.jwtFilter = jwtFilter;
        this.rutasPublicas = rutasPublicas;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // Sin cookies de sesión no hay CSRF que explotar.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(rutasPublicas).permitAll()
                        .anyRequest().authenticated())
                // Sin esto Spring Security responde 403 a quien no manda token,
                // y en una API con JWT eso es incorrecto: 401 significa "no te
                // has identificado" y 403 "te identificaste pero no te alcanza
                // el rol". Confundirlos deja al cliente sin saber si debe
                // reintentar con credenciales o rendirse.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(this::responderNoAutenticado))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Responde 401 CON CUERPO.
     *
     * La alternativa de una línea, `HttpStatusEntryPoint`, devuelve el 401 con
     * el cuerpo vacío. Funciona, pero convierte cualquier problema de token en
     * un misterio: quien llame con curl recibe una respuesta en blanco y no
     * sabe si el servicio está caído, si la ruta no existe o si le falta el
     * token. Costó un buen rato de diagnóstico, así que aquí va explícito.
     */
    private void responderNoAutenticado(HttpServletRequest peticion,
                                        HttpServletResponse respuesta,
                                        AuthenticationException excepcion) throws IOException {

        respuesta.setStatus(HttpStatus.UNAUTHORIZED.value());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.getWriter().write("""
                {"type":"about:blank",\
                "title":"No autenticado",\
                "status":401,\
                "detail":"Falta la cabecera Authorization o el token JWT no es válido"}""");
    }
}
