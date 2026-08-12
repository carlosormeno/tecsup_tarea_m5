package com.tecsup.app.micro.user.infrastructure.security;

import com.tecsup.app.micro.user.domain.repository.CifradorDeClaves;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adaptador de cifrado con BCrypt.
 *
 * BCrypt incorpora la sal en el propio hash y es deliberadamente lento, que es
 * justo lo que se quiere para contraseñas: encarece los ataques por fuerza
 * bruta. Un SHA-256 sería miles de veces más rápido de romper.
 */
@Component
public class BCryptCifrador implements CifradorDeClaves {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String cifrar(String claveEnClaro) {
        return encoder.encode(claveEnClaro);
    }

    @Override
    public boolean coincide(String claveEnClaro, String hashGuardado) {
        return encoder.matches(claveEnClaro, hashGuardado);
    }
}
