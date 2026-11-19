package com.elora.module.pagamento.dto;

import com.elora.module.pagamento.enums.StatusPagamento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoStatusResponse {

    private Integer pagamentoId;
    private StatusPagamento status;
    private String statusLabel;
    private boolean canPay;
    private boolean canCancel;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}