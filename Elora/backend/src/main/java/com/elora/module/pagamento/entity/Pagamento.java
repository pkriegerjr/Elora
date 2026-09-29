package com.elora.module.pagamento.entity;

import com.elora.module.pagamento.enums.MetodoPagamento;
import com.elora.module.pagamento.enums.StatusPagamento;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Espelha {@code pagamento} do schema v2 (REQ-007, RNF-011).
 * FKs como IDs simples (sem associação) para evitar surprises de lazy loading;
 * a existência do contrato é validada no service (404 em vez de 500 do banco).
 */
@Entity
@Table(name = "pagamento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pagamento")
    private Integer id;

    @Column(name = "contrato_id", nullable = false)
    private Integer contratoId;

    @Column(name = "pagador_id", nullable = false)
    private Integer pagadorId;

    @Column(name = "taxa_id")
    private Integer taxaId;

    @Column(name = "valor_bruto", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorBruto;

    @Column(name = "valor_taxa", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTaxa = BigDecimal.ZERO;

    @Column(name = "valor_liquido", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorLiquido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MetodoPagamento metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusPagamento status = StatusPagamento.pendente;

    @Column(name = "gateway_id", length = 120)
    private String gatewayId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 80)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
