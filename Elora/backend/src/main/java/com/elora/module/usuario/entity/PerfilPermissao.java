package com.elora.module.usuario.entity;

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

/** Espelha {@code perfil_permissao} do schema v2. */
@Entity
@Table(name = "perfil_permissao")
@IdClass(PerfilPermissaoId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerfilPermissao {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_id")
    private Perfil perfil;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permissao_id")
    private Permissao permissao;
}
