package com.elora.module.contrato.enums;

/**
 * Espelha {@code contrato.status} do schema v2.
 * Nomes idênticos aos valores do banco para {@code @Enumerated(STRING)}
 * (mesmo padrão de Genero).
 * Funil: rascunho → proposta → negociacao → aguard_assinatura → ativo →
 * concluido; desvios: cancelado, rescindido, em_disputa.
 */
public enum StatusContrato {
    rascunho,
    proposta,
    negociacao,
    aguard_assinatura,
    ativo,
    concluido,
    rescindido,
    cancelado,
    em_disputa
}
