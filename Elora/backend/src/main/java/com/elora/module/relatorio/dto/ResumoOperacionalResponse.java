package com.elora.module.relatorio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** GET /relatorios/resumo — snapshot operacional do dashboard admin. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumoOperacionalResponse {

    private long totalUsuarios;
    private long usuariosAtivos;
    private long totalProfissionais;
    private long profissionaisPendentes;
    private long contratosAtivos;
    private long contratosEmDisputa;
    private long pagamentosPendentes;
    private long repassesPendentes;
    private BigDecimal valorRepassesPendentes;
    private long denunciasAbertas;
    private BigDecimal mediaAvaliacoes;
}
