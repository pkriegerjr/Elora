package com.elora.module.juridico.entity;

import com.elora.module.contrato.entity.Contrato;
import com.elora.module.juridico.enums.StatusDenuncia;
import com.elora.module.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * MÓDULO JURÍDICO - Entity JPA da tabela {@code denuncia} (schema v2).
 *
 * <p>Substitui o stub em memória: agora a denúncia persiste, aparece no
 * relatório (que já consulta esta tabela) e sobrevive ao reinício da API.
 * O campo {@code parecer} vem da migration v2.5.</p>
 */
@Entity
@Table(name = "denuncia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Denuncia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_denuncia")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "denunciante_id", nullable = false)
    private Usuario denunciante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "denunciado_id", nullable = false)
    private Usuario denunciado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;

    @Column(nullable = false, length = 150)
    private String motivo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatusDenuncia status = StatusDenuncia.aberta;

    /** Último parecer da equipe (coluna adicionada na v2.5). */
    @Column(columnDefinition = "TEXT")
    private String parecer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analisado_por")
    private Usuario analisadoPor;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
