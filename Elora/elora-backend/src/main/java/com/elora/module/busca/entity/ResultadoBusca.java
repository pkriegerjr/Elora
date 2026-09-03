package com.elora.module.busca.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResultadoBusca {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idResultado;
    private Double distancia;
    private Double notaProfissional;
    private String disponibilidade;
    public void exibirResultado(){}
    public void ordenarPorDistancia(){}
    public void ordenarPorAvaliacao(){}
}
