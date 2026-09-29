package com.elora.module.contrato.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Linha de {@code assinatura_contrato} (sem expor o hash). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssinaturaResponse {

    private Integer id;
    private String papel;
    private LocalDateTime assinadoEm;
}
