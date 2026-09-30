package com.elora.module.pagamento.gateway;

/**
 * Fronteira da integração externa de cobrança (RNF-014).
 * Trocar de provedor (Stripe/Mercado Pago/PagSeguro) = nova implementação
 * desta interface (marcada {@code @Primary}) — o serviço não muda.
 */
public interface GatewayPagamentoService {

    GatewayCobrancaResult cobrar(GatewayCobrancaRequest request);
}
