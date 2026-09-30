package com.elora.module.pagamento.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** POST /taxas (REQ-012, equipe financeira). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarTaxaRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100)
    private String nome;

    @NotNull(message = "Percentual é obrigatório")
    @DecimalMin(value = "0.00", message = "Percentual inválido")
    @DecimalMax(value = "100.00", message = "Percentual inválido")
    private BigDecimal percentual;

    @DecimalMin(value = "0.00", message = "Valor fixo inválido")
    private BigDecimal valorFixo;

    @NotNull(message = "Início da vigência é obrigatório")
    private LocalDate vigenteDe;

    private LocalDate vigenteAte;
}
