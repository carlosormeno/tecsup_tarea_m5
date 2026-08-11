package com.tecsup.app.micro.order.infrastructure.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtTokenProvider proveedor;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest peticion,
                                    @NonNull HttpServletResponse respuesta,
                                    @NonNull FilterChain cadena)
            throws ServletException, IOException {

        extraerToken(peticion)
                .flatMap(proveedor::validar)
                .ifPresent(claims -> autenticar(claims, peticion));

        cadena.doFilter(peticion, respuesta);
    }

    private java.util.Optional<String> extraerToken(HttpServletRequest peticion) {
        String cabecera = peticion.getHeader(HttpHeaders.AUTHORIZATION);

        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            return java.util.Optional.of(cabecera.substring(PREFIJO.length()));
        }
        return java.util.Optional.empty();
    }

    private void autenticar(Claims claims, HttpServletRequest peticion) {
        List<SimpleGrantedAuthority> autoridades = proveedor.roles(claims).stream()
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                .toList();

        var autenticacion = new UsernamePasswordAuthenticationToken(
                proveedor.usuarioId(claims), null, autoridades);

        autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(peticion));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
