package com.elora.module.avaliacao.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Evidencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEvidencia;
    private String tipoEvidencia;
    private String arquivo;
    private String descricao;
    @ManyToOne private Denuncia denuncia;
    public void anexarEvidencia(){}
    public void removerEvidencia(){}
}
