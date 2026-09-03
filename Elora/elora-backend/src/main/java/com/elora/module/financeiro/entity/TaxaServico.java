package com.elora.module.financeiro.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaxaServico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTaxa;
    private String nomeTaxa;
    private Double percentual;
    private LocalDate dataVigencia;
    private String statusTaxa;
    public void cadastrarTaxa(){}
    public void atualizarPercentual(){}
    public void inativarTaxa(){}
}
