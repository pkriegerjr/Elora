package com.elora.module.usuario.entity;

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

/** Espelha {@code juridico_detalhes} do schema v2. */
@Entity
@Table(name = "juridico_detalhes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JuridicoDetalhes {

    @Id
    @Column(name = "usuario_id")
    private Integer usuarioId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(length = 20)
    private String oab;

    @Column(name = "permissao_editar_contrato", nullable = false)
    private Boolean permissaoEditarContrato = false;

    @Column(name = "permissao_aprovar_termo", nullable = false)
    private Boolean permissaoAprovarTermo = false;
}
