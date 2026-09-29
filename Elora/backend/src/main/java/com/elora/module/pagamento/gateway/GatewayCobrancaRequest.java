package com.elora.module.pagamento.gateway;

import com.elora.module.pagamento.enums.MetodoPagamento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Entrada da cobrança junto ao provedor (mock hoje, Stripe/MP/PagSeguro amanhã). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GatewayCobrancaRequest {

    private Integer pagamentoId;
    private BigDecimal valor;
    private MetodoPagamento metodo;
    private String gatewayToken;
}
