package com.tecsup.app.micro.user.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Raíz del agregado Usuario.
 *
 * Nunca guarda la contraseña en claro: solo su hash. El cifrado en sí es
 * infraestructura y entra por el puerto {@code CifradorDeClaves}, así que esta
 * clase no sabe que existe BCrypt.
 */
public class Usuario {

    private static final int PUNTOS_POR_SOL = 1;

    private final Long id;
    private final String nombre;
    private final String email;
    private final String passwordHash;
    private final Set<Rol> roles;
    private final Instant creadoEn;

    private String direccion;
    private int puntosFidelidad;
    private boolean activo;

    private Usuario(Long id, String nombre, String email, String passwordHash, Set<Rol> roles,
                    String direccion, int puntosFidelidad, boolean activo, Instant creadoEn) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = EnumSet.copyOf(roles);
        this.direccion = direccion;
        this.puntosFidelidad = puntosFidelidad;
        this.activo = activo;
        this.creadoEn = creadoEn;
    }

    /**
     * Registra un usuario nuevo.
     *
     * Recibe el hash ya calculado, no la contraseña: el dominio nunca ve la
     * clave en claro ni conoce el algoritmo de cifrado.
     */
    public static Usuario registrar(String nombre, String email, String passwordHash,
                                    String direccion, Set<Rol> roles) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El usuario necesita un nombre");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("El email no es válido");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("El usuario necesita una contraseña");
        }

        Set<Rol> asignados = (roles == null || roles.isEmpty())
                ? EnumSet.of(Rol.CLIENTE)   // por defecto, cliente
                : EnumSet.copyOf(roles);

        return new Usuario(null, nombre, email.toLowerCase().trim(), passwordHash,
                asignados, direccion, 0, true, Instant.now());
    }

    public static Usuario reconstituir(Long id, String nombre, String email, String passwordHash,
                                       Set<Rol> roles, String direccion, int puntosFidelidad,
                                       boolean activo, Instant creadoEn) {
        return new Usuario(id, nombre, email, passwordHash, roles, direccion,
                puntosFidelidad, activo, creadoEn);
    }

    /**
     * Suma puntos por un pedido entregado: un punto por cada sol gastado,
     * redondeando hacia abajo.
     *
     * Recibe el importe y no los puntos ya calculados porque la regla de
     * conversión es de negocio y debe vivir en el dominio, no en quien
     * consume el evento.
     */
    public void sumarPuntosPor(java.math.BigDecimal totalDelPedido) {
        if (totalDelPedido == null || totalDelPedido.signum() < 0) {
            throw new IllegalArgumentException("El total del pedido no puede ser negativo");
        }
        this.puntosFidelidad += totalDelPedido.intValue() * PUNTOS_POR_SOL;
    }

    public void cambiarDireccion(String nueva) {
        if (nueva == null || nueva.isBlank()) {
            throw new IllegalArgumentException("La dirección no puede estar vacía");
        }
        this.direccion = nueva;
    }

    public void desactivar() {
        this.activo = false;
    }

    public boolean tieneRol(Rol rol) {
        return roles.contains(rol);
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Set<Rol> getRoles() { return Collections.unmodifiableSet(roles); }
    public String getDireccion() { return direccion; }
    public int getPuntosFidelidad() { return puntosFidelidad; }
    public boolean isActivo() { return activo; }
    public Instant getCreadoEn() { return creadoEn; }
}
