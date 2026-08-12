package com.tecsup.app.micro.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Registro de eventos ya aplicados.
 *
 * La clave primaria es el propio `eventoId`: si un evento repetido llegara a
 * insertarse, la base lo rechazaría. Es la red de seguridad por debajo de la
 * comprobación explícita del caso de uso.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "evento_procesado")
public class EventoProcesadoJpaEntity {

    @Id
    @Column(name = "evento_id", length = 60)
    private String eventoId;

    @Column(nullable = false)
    private String topic;

    @Column(name = "procesado_en", nullable = false)
    private Instant procesadoEn;
}
