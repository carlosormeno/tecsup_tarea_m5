package com.tecsup.app.micro.user.infrastructure.security;

import com.tecsup.app.micro.user.domain.model.Rol;
import com.tecsup.app.micro.user.domain.model.Usuario;
import com.tecsup.app.micro.user.domain.repository.EmisorDeTokens;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * ÚNICO emisor de tokens de todo el sistema.
 *
 * Los otros cuatro servicios solo tienen el `JwtTokenProvider` del módulo
 * compartido, que valida pero no firma. Esta clase, la que sí firma, existe
 * en un solo sitio a propósito.
 *
 * El token lleva:
 *   sub   → id del usuario, que es lo que los demás servicios usan como
 *           identidad del solicitante
 *   roles → lista de roles, que se convierten en autoridades ROLE_*
 *   exp   → caducidad
 *
 * LIMITACIÓN DECLARADA: la clave es simétrica y compartida, así que cualquiera
 * de los cinco servicios podría técnicamente firmar tokens, no solo validarlos.
 * Un par asimétrico (privada aquí, pública en el resto) lo impediría de raíz.
 * Se asume por simplicidad; ver ADR y sección 5 del documento.
 */
@Component
public class JwtEmisor implements EmisorDeTokens {

    private final SecretKey clave;
    private final Duration validez;

    public JwtEmisor(@Value("${jwt.secret}") String secreto,
                     @Value("${jwt.expiracion-segundos:3600}") long segundos) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.validez = Duration.ofSeconds(segundos);
    }

    @Override
    public String emitirPara(Usuario usuario) {
        List<String> roles = usuario.getRoles().stream().map(Rol::name).toList();
        long ahora = System.currentTimeMillis();

        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("roles", roles)
                .claim("email", usuario.getEmail())
                .issuedAt(new Date(ahora))
                .expiration(new Date(ahora + validez.toMillis()))
                .signWith(clave)
                .compact();
    }

    @Override
    public long validezEnSegundos() {
        return validez.toSeconds();
    }
}
