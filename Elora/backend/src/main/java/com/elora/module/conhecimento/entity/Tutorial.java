package com.elora.module.conhecimento.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "tutorial")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tutorial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_tutorial") private Integer id;
    @Column(nullable = false) private String titulo;
    @Column(columnDefinition = "TEXT") private String descricao;
    @Column(name = "link_conteudo", length = 500) private String linkConteudo;
    @Column(length = 50) @Builder.Default private String status = "rascunho";
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "categoria_id") private CategoriaConteudo categoriaRef;
    @CreationTimestamp @Column(name = "criado_em", updatable = false) private LocalDateTime criadoEm;
}