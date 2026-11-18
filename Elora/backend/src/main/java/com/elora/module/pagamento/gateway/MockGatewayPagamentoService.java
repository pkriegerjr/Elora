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
@ConditionalOnProperty(name = "app.integracoes.pagamento.provider",
        havingValue = "mock", matchIfMissing = true)
public class MockGatewayPagamentoService implements GatewayPagamentoService {

    @Override
    @CircuitBreaker(name = "gatewayPagamento", fallbackMethod = "indisponivel")
    public GatewayCobrancaResult cobrar(GatewayCobrancaRequest req) {
        if ("tok_erro".equals(req.getGatewayToken())) {
            throw new IllegalStateException("simulacao de queda do provedor");
        }
        if ("tok_recusado".equals(req.getGatewayToken())) {
            return GatewayCobrancaResult.recusada("Pagamento recusado pela instituicao financeira");
        }
        String gatewayId = "GW-MOCK-" + req.getPagamentoId() + "-"
                + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        GatewayCobrancaResult r = GatewayCobrancaResult.aprovada(gatewayId);
        if (req.getMetodo() == MetodoPagamento.pix) {
            r.comPix(null, "00020126580014br.gov.bcb.pix0136GW-MOCK-" + req.getPagamentoId()
                    + "5204000053039865802BR5913ELORA6009SAO PAULO62070503***6304",
                    LocalDateTime.now().plusMinutes(30));
        } else if (req.getMetodo() == MetodoPagamento.boleto) {
            r.comBoleto("23790.00000 00000.000000 00000.000000 0 00000000000000",
                    "mock://boletos/" + req.getPagamentoId() + ".pdf",
                    LocalDateTime.now().plusDays(3));
        } else {
            r.comCheckout("mock://checkout/" + req.getPagamentoId());
        }
        return r;
    }

    @Override public GatewayCobrancaResult consultar(String gatewayId) {
        return GatewayCobrancaResult.aprovada(gatewayId);
    }

    @Override public GatewayCobrancaResult estornar(String gatewayId) {
        return GatewayCobrancaResult.aprovada(gatewayId);
    }

    @Override public boolean cancelar(String gatewayId) { return true; }

    public GatewayCobrancaResult indisponivel(GatewayCobrancaRequest req, Throwable t) {
        return GatewayCobrancaResult.indisponivel();
    }
}