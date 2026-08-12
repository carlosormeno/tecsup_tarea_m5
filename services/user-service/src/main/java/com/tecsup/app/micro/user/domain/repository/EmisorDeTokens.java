package com.tecsup.app.micro.user.domain.repository;

import com.tecsup.app.micro.user.domain.model.Usuario;

/**
 * Puerto de salida para emitir tokens.
 *
 * Este es el ÚNICO servicio del sistema que tiene una implementación de este
 * puerto. Los otros cuatro solo validan, con el `JwtTokenProvider` del módulo
 * compartido. Que el código de emisión exista físicamente en un solo servicio
 * es lo que respalda la regla "solo Usuarios emite tokens".
 */
public interface EmisorDeTokens {

    /** @return el JWT firmado, con el id del usuario en el subject y sus roles */
    String emitirPara(Usuario usuario);

    /** Segundos de validez del token emitido, para informarlo al cliente. */
    long validezEnSegundos();
}
