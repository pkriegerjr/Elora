package com.elora.module.contrato.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MÓDULO CONTRATO - DTO do PUT /contratos/{id}. Tudo opcional:
 * null = "não mexer". Partes/código/status não mudam por aqui.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtualizarContratoRequest {

    @Size(max = 150, message = "Título deve ter no máximo 150 caracteres")
    private String titulo;

    private String descricaoNecessidade;

    @DecimalMin(value = "0.0", message = "valorHora não pode ser negativo")
    private BigDecimal valorHora;

    @DecimalMin(value = "0.0", message = "valorTotal não pode ser negativo")
    private BigDecimal valorTotal;

    @Size(max = 255, message = "Endereço deve ter no máximo 255 caracteres")
    private String enderecoAtendimento;

    private LocalDate dataInicio;

    private LocalDate dataFim;
}
