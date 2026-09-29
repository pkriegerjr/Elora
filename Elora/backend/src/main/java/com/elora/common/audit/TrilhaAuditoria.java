package com.elora.common.audit;

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

import java.time.LocalDateTime;

/**
 * Espelha {@code trilha_auditoria} do schema v2 (RNF-012).
 * Subconjunto sem {@code dados_antes/depois} (JSONB): Hibernate validate
 * rejeitaria mapeamento String sobre coluna JSONB, e o H2 de testes não
 * tem JSONB — ação/entidade/ator já cobrem "quem alterou o quê".
 */
@Entity
@Table(name = "trilha_auditoria")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrilhaAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long id;

    @Column(name = "ator_id")
    private Integer atorId;

    @Column(nullable = false, length = 80)
    private String acao;

    @Column(nullable = false, length = 80)
    private String entidade;

    @Column(name = "entidade_id", length = 60)
    private String entidadeId;

    @Column(length = 45)
    private String ip;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
