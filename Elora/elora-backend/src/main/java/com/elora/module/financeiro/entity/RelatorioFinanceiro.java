package com.elora.module.financeiro.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RelatorioFinanceiro {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRelatorio;
    private LocalDate periodoInicial;
    private LocalDate periodoFinal;
    private Double totalRecebido;
    private Double totalRepassado;
    private Double totalTaxas;
    public void gerarRelatorio(){}
    public void exportarRelatorio(){}
}
