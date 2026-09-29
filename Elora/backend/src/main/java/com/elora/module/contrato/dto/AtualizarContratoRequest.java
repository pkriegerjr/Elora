package com.elora.module.contrato.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** PUT /contracts/{id} — só não-finalizado; null = não mexer. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtualizarContratoRequest {

    @Size(max = 150)
    private String titulo;

    private String descricaoNecessidade;

    @DecimalMin(value = "0.0")
    private BigDecimal valorHora;

    @DecimalMin(value = "0.0")
    private BigDecimal valorTotal;

    @Size(max = 255)
    private String enderecoAtendimento;

    private LocalDate dataInicio;

    private LocalDate dataFim;
}
