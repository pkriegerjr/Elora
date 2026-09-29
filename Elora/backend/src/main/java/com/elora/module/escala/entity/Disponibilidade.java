package com.elora.module.escala.entity;

import com.elora.module.escala.enums.PeriodoTurno;
import com.elora.module.escala.enums.StatusDisponibilidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Espelha {@code disponibilidade} do schema v2 (UC10 + REQ-013).
 * Quadro do profissional: (usuario, data, periodo), único por {@code uq_disp}.
 */
@Entity(name = "EscalaDisponibilidade")
@Table(name = "disponibilidade")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Disponibilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_disponibilidade")
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private PeriodoTurno periodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusDisponibilidade status = StatusDisponibilidade.disponivel;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
