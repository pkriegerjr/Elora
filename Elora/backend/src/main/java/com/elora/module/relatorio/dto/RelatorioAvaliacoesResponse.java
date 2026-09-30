package com.elora.module.relatorio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/** GET /relatorios/avaliacoes — média geral + distribuição 1..5 (pronto p/ gráfico). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioAvaliacoesResponse {

    private long total;
    private BigDecimal mediaGeral;
    private Map<String, Long> distribuicao;
}
