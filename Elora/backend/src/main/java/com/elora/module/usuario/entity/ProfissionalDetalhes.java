package com.elora.module.usuario.entity;

import com.elora.module.usuario.enums.StatusVerificacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Espelha {@code profissional_detalhes} do schema v2 (PK compartilhada com usuario). */
@Entity
@Table(name = "profissional_detalhes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfissionalDetalhes {

    @Id
    @Column(name = "usuario_id")
    private Integer usuarioId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "descricao_perfil", columnDefinition = "TEXT")
    private String descricaoPerfil;

    @Column(name = "preco_hora", precision = 10, scale = 2)
    private BigDecimal precoHora;

    @Column(name = "documento_verificado", nullable = false)
    private Boolean documentoVerificado = false;

    /** v2.2: fluxo de validação do cuidador (fonte oficial da situação). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_verificacao", nullable = false, length = 15)
    private StatusVerificacao statusVerificacao = StatusVerificacao.pendente;

    /** Mantido por trigger no banco (trg_avaliacao_*) — backend só lê. */
    @Column(name = "nota_media", nullable = false, precision = 3, scale = 2)
    private BigDecimal notaMedia = new BigDecimal("0.00");
}
