package com.elora.module.pagamento.gateway;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Resultado da cobrança. INDISPONIVEL = fallback do circuit breaker. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GatewayCobrancaResult {

    public enum StatusCobranca {
        APROVADA,
        PENDENTE,
        RECUSADA,
        INDISPONIVEL
    }

    private StatusCobranca status;
    private String gatewayId;
    private String motivo;

    private String qrCodeBase64;
    private String pixCopiaECola;
    private String linhaDigitavel;
    private String boletoUrl;
    private String checkoutUrl;
    private LocalDateTime expiraEm;

    private GatewayCobrancaResult(StatusCobranca status, String gatewayId, String motivo) {
        this.status = status;
        this.gatewayId = gatewayId;
        this.motivo = motivo;
    }

    public static GatewayCobrancaResult aprovada(String gatewayId) {
        return new GatewayCobrancaResult(StatusCobranca.APROVADA, gatewayId, null);
    }

    public static GatewayCobrancaResult pendente(String gatewayId) {
        return new GatewayCobrancaResult(StatusCobranca.PENDENTE, gatewayId, null);
    }

    public static GatewayCobrancaResult recusada(String motivo) {
        return new GatewayCobrancaResult(StatusCobranca.RECUSADA, null, motivo);
    }

    public static GatewayCobrancaResult indisponivel() {
        return new GatewayCobrancaResult(StatusCobranca.INDISPONIVEL, null,
                "Gateway de pagamento indisponível, tente novamente");
    }

    public GatewayCobrancaResult comPix(String qrBase64, String copiaECola, LocalDateTime expiraEm) {
        this.qrCodeBase64 = qrBase64;
        this.pixCopiaECola = copiaECola;
        this.expiraEm = expiraEm;
        return this;
    }

    public GatewayCobrancaResult comBoleto(String linhaDigitavel, String url, LocalDateTime expiraEm) {
        this.linhaDigitavel = linhaDigitavel;
        this.boletoUrl = url;
        this.expiraEm = expiraEm;
        return this;
    }

    public GatewayCobrancaResult comCheckout(String url) {
        this.checkoutUrl = url;
        return this;
    }
}