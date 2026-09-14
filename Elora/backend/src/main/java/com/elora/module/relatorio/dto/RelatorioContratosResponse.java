package com.elora.module.relatorio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/** GET /relatorios/contratos — funil por status + valor médio/hora. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioContratosResponse {

    private long total;
    private long novosNoPeriodo;
    private BigDecimal valorMedioHora;
    private Map<String, Long> porStatus;
}
