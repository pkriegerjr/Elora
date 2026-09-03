package com.elora.module.conhecimento.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArtigoConhecimento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idArtigo;
    private String titulo;
    private String conteudo;
    private String categoria;
    private LocalDate dataPublicacao;
    @ManyToOne private CategoriaConteudo categoriaConteudo;
    public void criarArtigo(){}
    public void editarArtigo(){}
    public void publicarArtigo(){}
}
