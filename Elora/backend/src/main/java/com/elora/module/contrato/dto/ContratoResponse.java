package com.elora.module.contrato.dto;

import com.elora.module.contrato.enums.StatusContrato;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MÓDULO CONTRATO - DTO de saída. Nomes das partes p/ exibição
 * (sem CPF/senha). LAZY: mapper roda dentro de @Transactional.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoResponse {

    private Integer id;
    private String codigo;
    private Integer clienteId;
    private String clienteNome;
    private Integer profissionalId;
    private String profissionalNome;
    private String titulo;
    private String descricaoNecessidade;
    private BigDecimal valorHora;
    private BigDecimal valorTotal;
    private String enderecoAtendimento;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private StatusContrato status;
    private Integer criadoPorId;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
