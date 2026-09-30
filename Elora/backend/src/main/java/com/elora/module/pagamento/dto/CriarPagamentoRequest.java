package com.elora.module.pagamento.dto;

import com.elora.module.pagamento.enums.MetodoPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * POST /payments. O pagador é sempre o usuário autenticado (não vai no body).
 * {@code idempotencyKey} opcional — sem ela o backend gera uma por criação.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarPagamentoRequest {

    @NotNull(message = "Contrato é obrigatório")
    private Integer contratoId;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal valorBruto;

    @NotNull(message = "Método é obrigatório")
    private MetodoPagamento metodo;

    @Size(max = 80)
    private String idempotencyKey;
}
