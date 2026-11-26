package com.elora.module.juridico.mapper;

import com.elora.module.juridico.dto.DenunciaResponse;
import com.elora.module.juridico.entity.Denuncia;

/** MÓDULO JURÍDICO - Entity → DTO público (sem objetos JPA nos responses). */
public final class DenunciaMapper {

    private DenunciaMapper() {
    }

    public static DenunciaResponse toResponse(Denuncia d) {
        if (d == null) {
            return null;
        }
        return new DenunciaResponse(
                d.getId(),
                d.getDenunciante() == null ? null : d.getDenunciante().getId(),
                d.getDenunciado() == null ? null : d.getDenunciado().getId(),
                d.getContrato() == null ? null : d.getContrato().getId(),
                d.getMotivo(),
                d.getDescricao(),
                d.getParecer(),
                d.getStatus(),
                d.getAnalisadoPor() == null ? null : d.getAnalisadoPor().getId(),
                d.getCriadoEm(),
                d.getAtualizadoEm()
        );
    }
}
