package com.elora.module.pagamento.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** PUT /taxas/{id} — tudo opcional, só o enviado é alterado. Sem delete físico. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtualizarTaxaRequest {

    @Size(max = 100)
    private String nome;

    @DecimalMin(value = "0.00", message = "Percentual inválido")
    @DecimalMax(value = "100.00", message = "Percentual inválido")
    private BigDecimal percentual;

    @DecimalMin(value = "0.00", message = "Valor fixo inválido")
    private BigDecimal valorFixo;

    private LocalDate vigenteAte;

    private Boolean ativo;
}
