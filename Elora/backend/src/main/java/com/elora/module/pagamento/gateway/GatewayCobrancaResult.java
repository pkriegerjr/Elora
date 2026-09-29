package com.elora.module.pagamento.gateway;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Resultado da cobrança. INDISPONIVEL = fallback do circuit breaker. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GatewayCobrancaResult {

    public enum StatusCobranca {
        APROVADA,
        RECUSADA,
        INDISPONIVEL
    }

    private StatusCobranca status;
    private String gatewayId;
    private String motivo;

    public static GatewayCobrancaResult aprovada(String gatewayId) {
        return new GatewayCobrancaResult(StatusCobranca.APROVADA, gatewayId, null);
    }

    public static GatewayCobrancaResult recusada(String motivo) {
        return new GatewayCobrancaResult(StatusCobranca.RECUSADA, null, motivo);
    }

    public static GatewayCobrancaResult indisponivel() {
        return new GatewayCobrancaResult(StatusCobranca.INDISPONIVEL, null,
                "Gateway de pagamento indisponível, tente novamente");
    }
}
