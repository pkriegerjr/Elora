package com.elora.module.busca.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Localizacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idLocalizacao;
    private Double latitude;
    private Double longitude;
    private String cidade;
    private String estado;
    private Double raioBusca;
    public Double calcularDistancia(Localizacao outra){ return 0.0; }
    public void atualizarLocalizacao(){}
}
