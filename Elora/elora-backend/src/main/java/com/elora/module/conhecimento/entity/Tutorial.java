package com.elora.module.conhecimento.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tutorial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTutorial;
    private String titulo;
    private String descricao;
    private String linkConteudo;
    private String status;
    @ManyToOne private CategoriaConteudo categoriaConteudo;
    public void cadastrarTutorial(){}
    public void atualizarTutorial(){}
    public void publicarTutorial(){}
}
