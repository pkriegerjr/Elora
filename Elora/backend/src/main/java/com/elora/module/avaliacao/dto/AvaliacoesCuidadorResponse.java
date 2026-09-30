package com.elora.module.avaliacao.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * MÓDULO AVALIACAO - DTO do GET /avaliacoes/cuidador/{id}.
 *
 * <p>Resposta agregada pedida no escopo: dados do cuidador (id + média +
 * quantidade) junto com as avaliações individuais (quem avaliou, nota,
 * comentário, data).</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvaliacoesCuidadorResponse {

    private Integer cuidadorId;
    private BigDecimal media;
    private long totalAvaliacoes;
    private List<AvaliacaoResponse> avaliacoes;
}
