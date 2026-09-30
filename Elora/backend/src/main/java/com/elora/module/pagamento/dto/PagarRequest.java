package com.elora.module.pagamento.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POST /payments/{id}/pagar. Gateway mock determinístico (sem aleatoriedade):
 * {@code gatewayToken} {@code "tok_recusado"} recusa, qualquer outro aprova.
 * PIX/cartão/boleto seguem o mesmo fluxo de confirmação imediata no mock.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagarRequest {

    private String gatewayToken;
}
