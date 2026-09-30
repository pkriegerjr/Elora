package com.elora.module.usuario.enums;

/**
 * Espelha {@code profissional_detalhes.status_verificacao} do schema v2.2.
 * Formaliza o fluxo que o front (CAREGIVER_STATUS) já assumia:
 * pendente → em_analise → aprovado (ou rejeitado/correcao).
 */
public enum StatusVerificacao {
    pendente,
    em_analise,
    aprovado,
    rejeitado,
    correcao
}
