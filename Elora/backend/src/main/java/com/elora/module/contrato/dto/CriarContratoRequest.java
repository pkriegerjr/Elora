package com.elora.module.contrato.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MÓDULO CONTRATO - DTO do POST /contratos.
 * Código/status/criador são decididos no service (não vêm do front).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarContratoRequest {

    @NotNull(message = "clienteId é obrigatório")
    private Integer clienteId;

    @NotNull(message = "profissionalId é obrigatório")
    private Integer profissionalId;

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 150, message = "Título deve ter no máximo 150 caracteres")
    private String titulo;

    private String descricaoNecessidade;

    @NotNull(message = "valorHora é obrigatório")
    @DecimalMin(value = "0.0", message = "valorHora não pode ser negativo")
    private BigDecimal valorHora;

    @DecimalMin(value = "0.0", message = "valorTotal não pode ser negativo")
    private BigDecimal valorTotal;

    @Size(max = 255, message = "Endereço deve ter no máximo 255 caracteres")
    private String enderecoAtendimento;

    private LocalDate dataInicio;

    private LocalDate dataFim;
}
