package com.elora.module.avaliacao.entity;

import com.elora.module.contrato.entity.Contrato;
import com.elora.module.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * MÓDULO AVALIACAO - Entity JPA (tabela {@code avaliacao}, schema v2).
 *
 * <p>Regras vindas do banco (não inventadas):</p>
 * <ul>
 *   <li>{@code contrato_id NOT NULL UNIQUE} — avaliação nasce de um contrato;</li>
 *   <li>trigger {@code trg_avaliacao_valida_papel} — só cliente→profissional
 *       do contrato (o service checa antes para dar erro amigável);</li>
 *   <li>{@code nota BETWEEN 1 AND 5} (CHECK);</li>
 *   <li>trigger {@code trg_avaliacao_media} — mantém {@code nota_media} do
 *       profissional sozinha (backend só lê);</li>
 *   <li>UNIQUE(avaliador_id, avaliado_id) — criada na migration v2.3: um
 *       usuário avalia um cuidador UMA vez (regra do escopo + §4).</li>
 * </ul>
 *
 * <p>Relações LAZY (padrão do projeto): só carregam dentro de @Transactional.</p>
 */
@Entity
@Table(name = "avaliacao", uniqueConstraints = {
        // Par único (migration v2.3 no banco real; aqui gera no create-drop dos testes).
        // O validate do Hibernate confere tabelas/colunas (não uniques), então é seguro.
        @UniqueConstraint(name = "uq_avaliacao_avaliador_avaliado",
                columnNames = {"avaliador_id", "avaliado_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_avaliacao")
    private Integer id;

    // Contrato avaliado (coluna contrato_id, UNIQUE no schema v2).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false, unique = true)
    private Contrato contrato;

    // Quem avaliou = CLIENTE (coluna avaliador_id). Parte da UNIQUE do par.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliador_id", nullable = false)
    private Usuario avaliador;

    // Quem foi avaliado = CUIDADOR (coluna avaliado_id). Parte da UNIQUE do par.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliado_id", nullable = false)
    private Usuario avaliado;

    // Nota 1 a 5. Short porque a coluna é SMALLINT (o validate compara tipos).
    @Column(nullable = false)
    private Short nota;

    // Comentário opcional (TEXT, pode ser null).
    @Column(columnDefinition = "TEXT")
    private String comentario;

    // Data da avaliação (carimbo do Hibernate no INSERT).
    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
