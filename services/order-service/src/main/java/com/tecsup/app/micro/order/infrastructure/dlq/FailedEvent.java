package com.tecsup.app.micro.order.infrastructure.dlq;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "failed_events")
public class FailedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_origen", nullable = false)
    private String topicOrigen;

    @Column(name = "offset_origen")
    private Long offsetOrigen;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "mensaje_error", length = 1000)
    private String mensajeError;

    @Column(name = "ocurrido_en", nullable = false)
    private Instant ocurridoEn;
}
