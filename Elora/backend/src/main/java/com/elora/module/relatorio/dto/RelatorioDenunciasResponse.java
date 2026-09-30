package com.elora.module.relatorio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** GET /relatorios/denuncias — fila do jurídico/moderação por status. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioDenunciasResponse {

    private long total;
    private long abertas;
    private Map<String, Long> porStatus;
}
