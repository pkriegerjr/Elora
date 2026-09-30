package com.elora.module.busca.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * MÓDULO BUSCA - Cartão do profissional (sem dados sensíveis: sem CPF,
 * e-mail, senha ou telefone — só o público).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfissionalResponse {

    private Integer id;
    private String nome;
    private String fotoUrl;
    private String descricaoPerfil;
    private BigDecimal precoHora;
    private BigDecimal notaMedia;
    private List<String> especialidades;
}
