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

    private String descricao;
    private String nomeCliente;
    private String emailCliente;
    private String cpfcnpjCliente;
    private LocalDate vencimento;
    private Integer parcelas;
    private String urlRetorno;

    public GatewayCobrancaRequest(Integer pagamentoId, BigDecimal valor, MetodoPagamento metodo, String gatewayToken){
        this.pagamentoId = pagamentoId;
        this.valor = valor;
        this.metodo = metodo;
        this.gatewayToken = gatewayToken;
    }
}
