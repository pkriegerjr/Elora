package com.elora.module.conhecimento.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FAQ {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFAQ;
    private String pergunta;
    private String resposta;
    private String categoria;
    private String status;
    @ManyToOne private CategoriaConteudo categoriaConteudo;
    public void cadastrarPergunta(){}
    public void atualizarResposta(){}
    public void publicarFAQ(){}
}
