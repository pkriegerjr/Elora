package com.elora.module.juridico.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * STUB in-memory (NÃO é @Entity). Ver {@link AnaliseJuridica} para o motivo.
 * Em v2, rescisão = `contrato.status` (rescindido/cancelado/em_disputa) + `disputa`.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessoRescisao {
    private Integer idAnalise;
    private Integer contratoId;
    private String parecer;
    private String statusAnalise = "solicitado";
}
