package com.elora.module.conhecimento.entity;
import jakarta.persistence.*; import lombok.*; import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name="categoria_conteudo") @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoriaConteudo {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_categoria") private Integer id;
  @Column(name="nome_categoria", nullable=false, unique=true, length=80) private String nome;
  @Column(columnDefinition="TEXT") private String descricao;
  @CreationTimestamp @Column(name="criado_em", updatable=false) private LocalDateTime criadoEm;
}