package com.elora.module.escala.entity;

import com.elora.module.escala.enums.Periodo;
import com.elora.module.escala.enums.StatusEscala;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * MÓDULO ESCALA - Turno da escala de trabalho (tabela
 * {@code escala_trabalho}, REQ-ELO-013).
 *
 * <p>{@code contratoId} é número puro de propósito: o módulo contrato ainda
 * não existe neste backend, então não há entity para relacionar (mesmo
 * padrão do módulo profissional: IDs, sem dependência cruzada). A FK é
 * garantida pelo banco — contrato inexistente vira 422 no service.</p>
 */
@Entity
@Table(name = "escala_trabalho", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"contrato_id", "data", "periodo"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscalaTrabalho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_escala")
    private Integer id;

    @Column(name = "contrato_id", nullable = false)
    private Integer contratoId;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Periodo periodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatusEscala status = StatusEscala.prevista;
}
