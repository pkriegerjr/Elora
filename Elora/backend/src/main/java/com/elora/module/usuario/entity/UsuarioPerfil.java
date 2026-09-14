package com.elora.module.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Espelha {@code usuario_perfil} do schema v2. */
@Entity
@Table(name = "usuario_perfil")
@IdClass(UsuarioPerfilId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioPerfil {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_id")
    private Perfil perfil;

    @CreationTimestamp
    @Column(name = "atribuido_em", nullable = false, updatable = false)
    private LocalDateTime atribuidoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atribuido_por")
    private Usuario atribuidoPor;
}
