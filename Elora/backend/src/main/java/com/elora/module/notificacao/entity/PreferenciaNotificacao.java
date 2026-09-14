package com.elora.module.notificacao.entity;

import com.elora.module.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** Espelha {@code preferencia_notificacao} do schema v2 (PK compartilhada). */
@Entity
@Table(name = "preferencia_notificacao")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciaNotificacao {

    @Id
    @Column(name = "usuario_id")
    private Integer usuarioId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private Boolean push = true;

    @Column(nullable = false)
    private Boolean email = true;

    @Column(nullable = false)
    private Boolean sms = false;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
