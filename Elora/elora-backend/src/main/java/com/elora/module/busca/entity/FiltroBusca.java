package com.elora.module.busca.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FiltroBusca {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFiltro;
    private String genero;
    private String especialidade;
    private String localizacao;
    private Double avaliacaoMinima;
    private String disponibilidade;
    public void aplicarFiltro(){}
    public void limparFiltro(){}
}
