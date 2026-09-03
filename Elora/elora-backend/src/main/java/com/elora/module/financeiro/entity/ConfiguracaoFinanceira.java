package com.elora.module.financeiro.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConfiguracaoFinanceira {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idConfiguracao;
    private Double percentualRepasse;
    private Double percentualPlataforma;
    private LocalDate dataConfiguracao;
    public void definirPercentuais(){}
    public void atualizarConfiguracao(){}
}
