package com.elora.module.relatorio.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RelatorioPagamentos extends Relatorio {
    private Double totalPagamentos;
    private Double totalRepasses;
    private Double totalTaxas;
    @Override public void gerar(){}
    public void calcularReceitas(){}
}
