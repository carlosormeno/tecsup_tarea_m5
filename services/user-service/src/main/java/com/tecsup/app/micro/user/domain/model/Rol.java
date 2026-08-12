package com.tecsup.app.micro.user.domain.model;

/**
 * Roles del sistema.
 *
 * Viajan dentro del JWT en el claim "roles" y los demás servicios los leen de
 * ahí para construir las autoridades de Spring Security. Por eso este enum es
 * un contrato: cambiar un nombre obliga a reemitir todos los tokens vivos.
 */
public enum Rol {
    CLIENTE,
    REPARTIDOR,
    ADMIN
}
