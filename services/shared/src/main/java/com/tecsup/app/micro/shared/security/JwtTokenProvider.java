package com.tecsup.app.micro.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey clave;

    public JwtTokenProvider(@Value("${jwt.secret}") String secreto) {
        // HS256 exige al menos 256 bits: la clave debe tener 32 caracteres o más.
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @return los claims si la firma y la vigencia son correctas, vacío si no.
     *         Nunca propaga la excepción: un token inválido es un 401, no un
     *         error del servidor.
     */
    public Optional<Claims> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(claims);

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token rechazado: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public String usuarioId(Claims claims) {
        return claims.getSubject();
    }

    /** Los roles viajan en el claim "roles" como lista de cadenas. */
    @SuppressWarnings("unchecked")
    public List<String> roles(Claims claims) {
        Object roles = claims.get("roles");
        return (roles instanceof List<?> lista)
                ? (List<String>) lista
                : List.of();
    }
}
