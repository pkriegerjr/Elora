package com.elora.module.conhecimento.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="artigo_conhecimento") @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ArtigoConhecimento {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_artigo") private Integer id;
  @Column(nullable=false, length=150) private String titulo;
  @Column(name="corpo", nullable=false, columnDefinition="TEXT") private String corpo;
  @Column(length=80) private String categoria; // denormalizado p/ busca rápida
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="categoria_id") private CategoriaConteudo categoriaRef;
  @Column(name="autor_id") private Integer autorId;
  @Column(nullable=false) private Boolean publicado=false;
  private LocalDateTime criadoEm, atualizadoEm;
}