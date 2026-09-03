package com.elora.module.conhecimento.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoriaConteudo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCategoria;
    private String nomeCategoria;
    private String descricao;
    public void cadastrarCategoria(){}
    public void atualizarCategoria(){}
}
