package com.elora.module.pagamento.enums;

/**
 * Espelha {@code repasse.status CHECK('pendente','processado','falha')} do schema v2.
 * Nomes idênticos aos valores do banco para {@code @Enumerated(STRING)}.
 */
public enum StatusRepasse {
    pendente,
    processado,
    falha
}
