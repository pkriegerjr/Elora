package com.elora.module.avaliacao.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * MÓDULO AVALIACAO - DTO do GET /avaliacoes/cuidador/{id}/media.
 *
 * <p>Exatamente o que o escopo pede: soma + quantidade + média, calculados
 * dos dados persistidos (nunca um contador manual que possa dessincronizar).</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaAvaliacaoResponse {

    private Integer cuidadorId;
    private long somaNotas;
    private long totalAvaliacoes;
    private BigDecimal media;
}
