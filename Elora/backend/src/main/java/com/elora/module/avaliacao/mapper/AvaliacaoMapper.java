package com.elora.module.avaliacao.mapper;

import com.elora.module.avaliacao.dto.AvaliacaoResponse;
import com.elora.module.avaliacao.dto.AvaliacoesCuidadorResponse;
import com.elora.module.avaliacao.dto.MediaAvaliacaoResponse;
import com.elora.module.avaliacao.entity.Avaliacao;

import java.math.BigDecimal;
import java.util.List;

/**
 * MÓDULO AVALIACAO - Mapper (conversões Entity → DTO num lugar só).
 * Classe utilitária estática, sem Spring (padrão dos mappers do projeto).
 */
public final class AvaliacaoMapper {

    private AvaliacaoMapper() {
    }

    /** Entity (com relações LAZY — chamar dentro de @Transactional) → item. */
    public static AvaliacaoResponse toResponse(Avaliacao a) {
        return new AvaliacaoResponse(
                a.getId(),
                a.getContrato().getId(),
                a.getAvaliador().getId(),
                a.getAvaliado().getId(), // nome do escopo: cuidadorId
                a.getNota().intValue(), // SMALLINT (Short) → Integer da API
                a.getComentario(),
                a.getCriadoEm()
        );
    }

    /** Agregação (soma/total/média) → DTO de média. */
    public static MediaAvaliacaoResponse toMediaResponse(Integer cuidadorId, long soma,
                                                         long total, BigDecimal media) {
        return new MediaAvaliacaoResponse(cuidadorId, soma, total, media);
    }

    /** Agregação + itens → resposta completa do cuidador. */
    public static AvaliacoesCuidadorResponse toCuidadorResponse(Integer cuidadorId, BigDecimal media,
                                                                long total, List<AvaliacaoResponse> itens) {
        return new AvaliacoesCuidadorResponse(cuidadorId, media, total, itens);
    }
}
