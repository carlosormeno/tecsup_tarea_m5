package com.tecsup.app.micro.user.domain.repository;

/**
 * Puerto de salida para el cifrado de contraseñas.
 *
 * Existe para que el dominio no sepa que detrás hay BCrypt. Si mañana hubiera
 * que migrar a Argon2, se escribe otro adaptador y ni el agregado Usuario ni
 * los casos de uso cambian.
 */
public interface CifradorDeClaves {

    String cifrar(String claveEnClaro);

    boolean coincide(String claveEnClaro, String hashGuardado);
}
