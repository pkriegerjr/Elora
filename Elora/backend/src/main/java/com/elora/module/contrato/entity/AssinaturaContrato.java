package com.elora.module.contrato.entity;

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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Espelha {@code assinatura_contrato} do schema v2 (REQ-008).
 * Uma linha por (contrato, usuário, papel) — assinatura digital
 * simplificada (hash SHA-256 do termo + provedor interno).
 */
@Entity
@Table(name = "assinatura_contrato")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssinaturaContrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_assinatura")
    private Integer id;

    @Column(name = "contrato_id", nullable = false)
    private Integer contratoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 15)
    private String papel;

    @Column(name = "hash_documento", nullable = false, length = 128)
    private String hashDocumento;

    @Column(nullable = false, length = 50)
    private String provedor = "interno";

    @Column(name = "ip_assinatura", length = 45)
    private String ipAssinatura;

    @CreationTimestamp
    @Column(name = "assinado_em", nullable = false, updatable = false)
    private LocalDateTime assinadoEm;
}
