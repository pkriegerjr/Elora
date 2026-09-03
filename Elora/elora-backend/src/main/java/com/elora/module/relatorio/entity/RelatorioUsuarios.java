package com.elora.module.relatorio.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RelatorioUsuarios extends Relatorio {
    private Integer totalUsuarios;
    private Integer totalClientes;
    private Integer totalProfissionais;
    @Override public void gerar(){ this.totalUsuarios = totalClientes + totalProfissionais; }
    public void calcularTotalUsuarios(){}
}
