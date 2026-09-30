package com.elora.module.busca.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** MÓDULO BUSCA - Item do catálogo (o front monta o filtro com a lista). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EspecialidadeResponse {

    private Integer id;
    private String nome;
}
