package com.elora.module.pagamento.entity;

import com.elora.module.pagamento.enums.StatusRepasse;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Espelha {@code repasse} do schema v2 (REQ-011).
 * Criado automaticamente quando um pagamento é aprovado.
 */
@Entity
@Table(name = "repasse")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Repasse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_repasse")
    private Integer id;

    @Column(name = "pagamento_id", nullable = false)
    private Integer pagamentoId;

    @Column(name = "profissional_id", nullable = false)
    private Integer profissionalId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatusRepasse status = StatusRepasse.pendente;

    @Column(name = "processado_em")
    private LocalDateTime processadoEm;
}
