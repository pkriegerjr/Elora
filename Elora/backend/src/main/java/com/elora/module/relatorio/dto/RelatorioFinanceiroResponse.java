package com.elora.module.relatorio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/** GET /relatorios/financeiro — números para o painel financeiro (PIX/cartão/boleto, taxas, repasses). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioFinanceiroResponse {

    private String periodoInicio;
    private String periodoFim;
    private long totalPagamentos;
    private BigDecimal totalBruto;
    private BigDecimal totalTaxa;
    private BigDecimal totalLiquido;
    private BigDecimal ticketMedio;
    private Map<String, Long> porMetodo;
    private Map<String, Long> porStatus;
    private long repassesPendentes;
    private long repassesProcessados;
    private BigDecimal valorRepassesPendentes;
}
