package com.elora.module.relatorio.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RelatorioContratos extends Relatorio {
    private Integer totalContratos;
    private Integer contratosAtivos;
    private Integer contratosEncerrados;
    @Override public void gerar(){}
    public void calcularContratos(){}
}
