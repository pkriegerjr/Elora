package com.elora.module.pagamento.gateway;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

/**
 * Provedor simulado (sem rede, sem aleatoriedade — determinístico p/ testes).
 * Tokens de teste: {@code "tok_recusado"} recusa; {@code "tok_erro"} simula
 * queda do provedor (dispara o fallback); demais aprovam.
 * Produção: substituir por client HTTPS do provedor real.
 */
@Service
public class MockGatewayPagamentoService implements GatewayPagamentoService {

    @Override
    @CircuitBreaker(name = "gatewayPagamento", fallbackMethod = "indisponivel")
    public GatewayCobrancaResult cobrar(GatewayCobrancaRequest request) {
        if ("tok_erro".equals(request.getGatewayToken())) {
            throw new IllegalStateException("simulação de queda do provedor");
        }
        if ("tok_recusado".equals(request.getGatewayToken())) {
            return GatewayCobrancaResult.recusada("Pagamento recusado pela instituição financeira");
        }
        String gatewayId = "GW-MOCK-" + request.getPagamentoId() + "-"
                + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        return GatewayCobrancaResult.aprovada(gatewayId);
    }

    /** Fallback (RNF-014): provedor fora do ar ou circuito aberto. */
    public GatewayCobrancaResult indisponivel(GatewayCobrancaRequest request, Throwable t) {
        return GatewayCobrancaResult.indisponivel();
    }
}
