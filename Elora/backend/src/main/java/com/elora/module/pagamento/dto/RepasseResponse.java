package com.elora.module.pagamento.dto;

import com.elora.module.pagamento.enums.StatusRepasse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Espelha a linha de {@code repasse} (v2). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepasseResponse {

    private Integer id;
    private Integer pagamentoId;
    private Integer profissionalId;
    private BigDecimal valor;
    private StatusRepasse status;
    private LocalDateTime processadoEm;
}
