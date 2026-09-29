package com.elora.module.pagamento.enums;

/**
 * Espelha {@code pagamento.status CHECK('pendente','aprovado','recusado','estornado')} do schema v2.
 * Nomes idênticos aos valores do banco para {@code @Enumerated(STRING)} (mesmo padrão de Genero).
 */
public enum StatusPagamento {
    pendente,
    aprovado,
    recusado,
    estornado
}
