package com.elora.module.relatorio.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RelatorioJuridico extends Relatorio {
    private Integer totalDenuncias;
    private Integer totalDisputas;
    private Integer totalRescisoes;
    @Override public void gerar(){}
    public void calcularIndicadoresJuridicos(){}
}
