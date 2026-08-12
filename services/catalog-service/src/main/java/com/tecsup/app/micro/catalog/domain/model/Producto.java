package com.tecsup.app.micro.catalog.domain.model;

import com.tecsup.app.micro.catalog.domain.exception.StockInsuficienteException;

import java.math.BigDecimal;

/**
 * Producto del menú de un restaurante.
 *
 * Este servicio es la FUENTE DE VERDAD del precio y de la disponibilidad. Los
 * demás no pueden deducirlos por su cuenta: Pedidos consulta aquí antes de
 * crear un pedido y se queda con una copia de lo que respondamos.
 */
public class Producto {

    private final Long id;
    private final Long restauranteId;
    private final String nombre;
    private final String descripcion;
    private final BigDecimal precio;
    private final boolean activo;

    private int stock;

    private Producto(Long id, Long restauranteId, String nombre, String descripcion,
                     BigDecimal precio, int stock, boolean activo) {
        this.id = id;
        this.restauranteId = restauranteId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stock = stock;
        this.activo = activo;
    }

    public static Producto crear(Long restauranteId, String nombre, String descripcion,
                                 BigDecimal precio, int stock) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El producto necesita un nombre");
        }
        if (precio == null || precio.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        return new Producto(null, restauranteId, nombre, descripcion, precio, stock, true);
    }

    public static Producto reconstituir(Long id, Long restauranteId, String nombre,
                                        String descripcion, BigDecimal precio, int stock,
                                        boolean activo) {
        return new Producto(id, restauranteId, nombre, descripcion, precio, stock, activo);
    }

    /**
     * Disponible no es un campo, es una regla: hay que estar activo Y tener
     * existencias. Guardarlo como columna aparte permitiría que se
     * desincronizara del stock, que es la clase de incoherencia que después
     * nadie sabe explicar.
     */
    public boolean estaDisponible() {
        return activo && stock > 0;
    }

    /**
     * Descuenta stock por un pedido confirmado.
     *
     * @throws StockInsuficienteException si no alcanza. Es un fallo
     *         DETERMINISTA: reintentarlo daría el mismo resultado, así que el
     *         evento se va directo a la DLQ para revisión manual.
     */
    public void descontarStock(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a descontar debe ser mayor que cero");
        }
        if (stock < cantidad) {
            throw new StockInsuficienteException(id, stock, cantidad);
        }
        this.stock -= cantidad;
    }

    /** Repone stock por un pedido cancelado. */
    public void reponerStock(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a reponer debe ser mayor que cero");
        }
        this.stock += cantidad;
    }

    public Long getId() { return id; }
    public Long getRestauranteId() { return restauranteId; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getPrecio() { return precio; }
    public int getStock() { return stock; }
    public boolean isActivo() { return activo; }
}
