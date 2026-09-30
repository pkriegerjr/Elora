package com.elora.module.contrato.mapper;

import com.elora.module.contrato.dto.ContratoResponse;
import com.elora.module.contrato.entity.Contrato;

/** MÓDULO CONTRATO - Mapper estático Entity → DTO (sem Spring). */
public final class ContratoMapper {

    private ContratoMapper() {
    }

    /** Lê relações LAZY — chamar dentro de @Transactional do service. */
    public static ContratoResponse toResponse(Contrato contrato) {
        ContratoResponse dto = new ContratoResponse();
        dto.setId(contrato.getId());
        dto.setCodigo(contrato.getCodigo());
        dto.setClienteId(contrato.getCliente().getId());
        dto.setClienteNome(contrato.getCliente().getNome());
        dto.setProfissionalId(contrato.getProfissional().getId());
        dto.setProfissionalNome(contrato.getProfissional().getNome());
        dto.setTitulo(contrato.getTitulo());
        dto.setDescricaoNecessidade(contrato.getDescricaoNecessidade());
        dto.setValorHora(contrato.getValorHora());
        dto.setValorTotal(contrato.getValorTotal());
        dto.setEnderecoAtendimento(contrato.getEnderecoAtendimento());
        dto.setDataInicio(contrato.getDataInicio());
        dto.setDataFim(contrato.getDataFim());
        dto.setStatus(contrato.getStatus());
        dto.setCriadoPorId(contrato.getCriadoPor() == null ? null : contrato.getCriadoPor().getId());
        dto.setCriadoEm(contrato.getCriadoEm());
        dto.setAtualizadoEm(contrato.getAtualizadoEm());
        return dto;
    }
}
