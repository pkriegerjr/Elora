package com.elora.module.contrato.dto;

import com.elora.module.contrato.enums.StatusContrato;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Visão completa do contrato + assinaturas registradas. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoResponse {

    private Integer id;
    private String codigo;
    private Integer clienteId;
    private Integer profissionalId;
    private String titulo;
    private String descricaoNecessidade;
    private BigDecimal valorHora;
    private BigDecimal valorTotal;
    private String enderecoAtendimento;
    private StatusContrato status;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private List<AssinaturaResponse> assinaturas;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
