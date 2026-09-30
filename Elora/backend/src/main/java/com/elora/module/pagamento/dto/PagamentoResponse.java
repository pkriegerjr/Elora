package com.elora.module.pagamento.dto;

import com.elora.module.pagamento.enums.MetodoPagamento;
import com.elora.module.pagamento.enums.StatusPagamento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Espelha a linha de {@code pagamento} (v2). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoResponse {

    private Integer id;
    private Integer contratoId;
    private Integer pagadorId;
    private Integer taxaId;
    private BigDecimal valorBruto;
    private BigDecimal valorTaxa;
    private BigDecimal valorLiquido;
    private MetodoPagamento metodo;
    private StatusPagamento status;
    private String gatewayId;
    private String idempotencyKey;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
