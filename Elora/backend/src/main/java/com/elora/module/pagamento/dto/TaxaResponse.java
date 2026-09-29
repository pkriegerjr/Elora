package com.elora.module.pagamento.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Espelha a linha de {@code taxa_servico} (v2). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxaResponse {

    private Integer id;
    private String nome;
    private BigDecimal percentual;
    private BigDecimal valorFixo;
    private LocalDate vigenteDe;
    private LocalDate vigenteAte;
    private Boolean ativo;
    private Integer criadoPor;
    private LocalDateTime criadoEm;
}
