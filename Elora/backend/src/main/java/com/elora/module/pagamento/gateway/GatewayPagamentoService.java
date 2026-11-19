package com.elora.module.pagamento.gateway;

/**
 * Fronteira da integração externa de cobrança (RNF-014).
 * Trocar de provedor = nova implementação desta interface condicionada por
 * {@code app.integracoes.pagamento.provider}.
 */
public interface GatewayPagamentoService {

    GatewayCobrancaResult cobrar(GatewayCobrancaRequest request);

    GatewayCobrancaResult consultar(String gatewayId);

    GatewayCobrancaResult estornar(String gatewayId);

    boolean cancelar(String gatewayId);
}