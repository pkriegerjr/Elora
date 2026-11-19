package com.elora.module.pagamento.dto;

import com.elora.module.pagamento.enums.MetodoPagamento;
import com.elora.module.pagamento.enums.StatusPagamento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Payload de PIX/boleto/cartão consumido pelo paymentService.js do front. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CobrancaResponse {

    private Integer pagamentoId;
    private StatusPagamento status;
    private MetodoPagamento metodo;
    private String gatewayId;
    private String qrCode;
    private String qrCodeBase64;
    private String copiaEcola;
    private String linhaDigitavel;
    private String pdfUrl;
    private String checkoutUrl;
    private LocalDateTime expiresAt;
}