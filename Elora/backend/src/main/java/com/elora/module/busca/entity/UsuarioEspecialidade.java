package com.elora.module.busca.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MÓDULO BUSCA - Vínculo N:N {@code usuario_especialidade}.
 * Só os 2 IDs (para a busca, "quais" basta; nomes vêm da Especialidade).
 */
@Entity
@Table(name = "usuario_especialidade")
@IdClass(UsuarioEspecialidadeId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioEspecialidade {

    @Id
    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Id
    @Column(name = "especialidade_id")
    private Integer especialidadeId;
}
