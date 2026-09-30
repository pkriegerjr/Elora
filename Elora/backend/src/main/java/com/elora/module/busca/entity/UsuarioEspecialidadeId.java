package com.elora.module.busca.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * PK composta de {@code usuario_especialidade} (N:N). Exigida pelo @IdClass:
 * mesmos nomes de campo + Serializable + equals/hashCode (Lombok).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioEspecialidadeId implements Serializable {

    private Integer usuarioId;
    private Integer especialidadeId;
}
