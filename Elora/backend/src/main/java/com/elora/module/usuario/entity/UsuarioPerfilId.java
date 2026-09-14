package com.elora.module.usuario.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Chave composta de {@code usuario_perfil} (v2). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioPerfilId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer usuario;
    private Integer perfil;
}
