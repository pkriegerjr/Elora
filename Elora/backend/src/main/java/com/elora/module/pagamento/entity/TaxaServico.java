package com.elora.module.pagamento.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Espelha {@code taxa_servico} do schema v2 (REQ-005, REQ-012).
 * Cálculo: taxa = bruto * percentual / 100 + valor_fixo.
 * Sem delete físico: desativar via {@code ativo=false} (+{@code vigente_ate}).
 */
@Entity
@Table(name = "taxa_servico")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxaServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_taxa")
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentual = BigDecimal.ZERO;

    @Column(name = "valor_fixo", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorFixo = BigDecimal.ZERO;

    @Column(name = "vigente_de", nullable = false)
    private LocalDate vigenteDe;

    @Column(name = "vigente_ate")
    private LocalDate vigenteAte;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "criado_por")
    private Integer criadoPor;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
