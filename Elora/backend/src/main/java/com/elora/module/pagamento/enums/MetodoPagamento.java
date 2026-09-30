package com.elora.module.pagamento.enums;

/**
 * Espelha {@code pagamento.metodo CHECK('pix','cartao','boleto')} do schema v2.
 * Nomes idênticos aos valores do banco para {@code @Enumerated(STRING)}.
 */
public enum MetodoPagamento {
    pix,
    cartao,
    boleto
}
