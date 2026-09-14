package com.elora.module.usuario.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Chave composta de {@code perfil_permissao} (v2). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerfilPermissaoId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer perfil;
    private Integer permissao;
}
